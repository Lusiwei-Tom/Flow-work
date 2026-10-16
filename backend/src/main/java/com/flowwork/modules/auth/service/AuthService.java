package com.flowwork.modules.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flowwork.common.exception.BusinessException;
import com.flowwork.common.result.ResultCode;
import com.flowwork.common.security.JwtUtil;
import com.flowwork.modules.auth.dto.LoginRequest;
import com.flowwork.modules.auth.dto.LoginResponse;
import com.flowwork.modules.auth.dto.RegisterRequest;
import com.flowwork.modules.auth.entity.InviteCode;
import com.flowwork.modules.auth.entity.SysUser;
import com.flowwork.modules.auth.mapper.SysUserMapper;
import com.flowwork.modules.project.entity.ProjectMember;
import com.flowwork.modules.project.mapper.ProjectMemberMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAIL = 5;
    private static final int LOCK_MINUTES = 30;
    private static final String REFRESH_PREFIX = "flowwork:auth:refresh:";
    private static final String REFRESH_COUNT_PREFIX = "flowwork:auth:refresh:cnt:";
    private static final String SMS_PREFIX = "flowwork:sms:";

    private final SysUserMapper userMapper;
    private final PermissionService permissionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;
    private final InviteCodeService inviteCodeService;
    private final ProjectMemberMapper projectMemberMapper;

    /** 账号密码登录 */
    public LoginResponse login(LoginRequest req, String ip) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername())
                .last("LIMIT 1"));
        if (user == null) {
            throw new BusinessException(ResultCode.LOGIN_FAIL);
        }

        // 锁定判断
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED,
                    "密码错误过多，账号锁定至 " + user.getLockedUntil());
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED, "账号已禁用");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            handleLoginFail(user);
            throw new BusinessException(ResultCode.LOGIN_FAIL);
        }

        // 登录成功：重置失败计数
        user.setLoginFailCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ip);
        userMapper.updateById(user);

        return buildLoginResponse(user, req.getDeviceId());
    }

    private void handleLoginFail(SysUser user) {
        int fails = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
        user.setLoginFailCount(fails);
        if (fails >= MAX_FAIL) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            user.setLoginFailCount(0);
            log.warn("用户 {} 连续登录失败 {} 次，锁定 {} 分钟", user.getUsername(), fails, LOCK_MINUTES);
        }
        userMapper.updateById(user);
    }

    /** 注册新用户：必须使用邀请码，角色由码决定 */
    @Transactional
    public LoginResponse register(RegisterRequest req) {
        // 1. 校验邀请码
        InviteCode ic = inviteCodeService.validate(req.getInviteCode());

        Long usernameCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername()));
        if (usernameCount != null && usernameCount > 0) {
            throw new BusinessException(ResultCode.BIZ_CONFLICT, "用户名已存在");
        }
        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            Long phoneId = userMapper.findAccountId(req.getPhone());
            if (phoneId != null) throw new BusinessException(ResultCode.BIZ_CONFLICT, "手机号已注册");
        }
        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setPhone(blankToNull(req.getPhone()));
        user.setEmail(blankToNull(req.getEmail()));
        user.setNickname(req.getNickname() != null && !req.getNickname().isBlank() ? req.getNickname() : req.getUsername());
        user.setStatus(1);
        user.setLoginFailCount(0);
        userMapper.insert(user);

        // 2. 按邀请码预设角色分配（找不到角色回退 member）
        Long roleId = userMapper.selectRoleIdByCode(ic.getPresetRole());
        if (roleId == null) roleId = 3L;
        userMapper.insertUserRole(user.getId(), roleId);

        // 3. 绑定项目（如邀请码指定）
        if (ic.getPresetProjectId() != null) {
            ProjectMember pm = new ProjectMember();
            pm.setProjectId(ic.getPresetProjectId());
            pm.setUserId(user.getId());
            pm.setProjectRole(ic.getPresetProjectRole() != null ? ic.getPresetProjectRole() : "MEMBER");
            pm.setJoinedAt(LocalDateTime.now());
            projectMemberMapper.insert(pm);
        }

        // 4. 邀请码使用次数 +1
        inviteCodeService.markUsed(ic.getId());

        permissionService.evict(user.getId());
        return buildLoginResponse(user, UUID.randomUUID().toString());
    }

    /** Refresh Token 滑动续期 + 旋转 */
    public LoginResponse refresh(String refreshToken, String deviceId) {
        Claims claims;
        try {
            claims = jwtUtil.parse(refreshToken);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        String jti = claims.getId();
        String redisKey = REFRESH_PREFIX + jti;
        String stored = redis.opsForValue().get(redisKey);// 从 Redis 中获取存储的 userId
        Long userId = Long.valueOf(claims.getSubject());// 从 claims 中获取 userId
        log.info("Refresh Token 校验：{} -> {}", stored, userId);
        if (stored == null || !stored.equals(String.valueOf(userId))) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Refresh Token 已失效，请重新登录");
        }// 校验 userId 是否匹配

        // 滑动续期计数
        String cntKey = REFRESH_COUNT_PREFIX + (deviceId != null ? deviceId : "default") + ":" + userId;
        Long count = redis.opsForValue().increment(cntKey);// 增加续期次数计数
        if (count != null && count == 1) {
            redis.expire(cntKey, 7, TimeUnit.DAYS);// 初始设置过期时间为 7 天
        }
        if (count != null && count > 3) {
            redis.delete(redisKey);// 删除 refresh token
            throw new BusinessException(ResultCode.TOKEN_INVALID, "续期次数过多，请重新登录");
        }

        // 旋转：旧 token 失效
        redis.delete(redisKey);// 删除 refresh token



        SysUser user = permissionService.loadUserOrThrow(userId);
        return buildLoginResponse(user, deviceId != null ? deviceId : UUID.randomUUID().toString());
    }

    /** 登出：删除 refresh token */
    public void logout(String refreshToken) {
        try {
            Claims claims = jwtUtil.parse(refreshToken);
            redis.delete(REFRESH_PREFIX + claims.getId());
        } catch (Exception ignored) {
            // 已经失效则无需处理
        }
    }

    public LoginResponse.UserInfoVO currentUser(Long userId) {
        SysUser user = permissionService.loadUserOrThrow(userId);
        return LoginResponse.UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .roles(permissionService.getRoles(userId))
                .permissions(permissionService.getPermissions(userId))
                .build();
    }

    /** 发送短信验证码（演示实现：写入 Redis，生产对接阿里云短信 SDK） */
    public String sendSmsCode(String phone) {
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        redis.opsForValue().set(SMS_PREFIX + phone, code, 5, TimeUnit.MINUTES);
        log.info("[短信验证码演示] phone={} code={}", phone, code);
        return code;
    }

    public LoginResponse phoneLogin(String phone, String code, String deviceId) {
        String cached = redis.opsForValue().get(SMS_PREFIX + phone);
        if (cached == null || !cached.equals(code)) {
            throw new BusinessException(ResultCode.LOGIN_FAIL, "验证码错误或已过期");
        }
        redis.delete(SMS_PREFIX + phone);
        Long userId = userMapper.findAccountId(phone);
        if (userId == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该手机号未注册");
        }
        SysUser user = permissionService.loadUserOrThrow(userId);
        return buildLoginResponse(user, deviceId);
    }

    private LoginResponse buildLoginResponse(SysUser user, String deviceId) {
        List<String> roles = permissionService.getRoles(user.getId());
        List<String> perms = permissionService.getPermissions(user.getId());

        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getUsername(), roles, perms);
        String refreshToken = jwtUtil.createRefreshToken(user.getId(), deviceId != null ? deviceId : "default");

        // refresh jti 存 Redis
        String jti;
        try {
            jti = jwtUtil.parse(refreshToken).getId();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SERVER_ERROR);
        }
        redis.opsForValue().set(REFRESH_PREFIX + jti, String.valueOf(user.getId()),
                jwtUtil.getRefreshTtl(), TimeUnit.SECONDS);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessTtl())
                .userInfo(LoginResponse.UserInfoVO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .avatar(user.getAvatar())
                        .roles(roles)
                        .permissions(perms)
                        .build())
                .build();
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
