package com.flowwork.modules.auth.service;

import com.flowwork.common.exception.BusinessException;
import com.flowwork.common.result.ResultCode;
import com.flowwork.modules.auth.entity.SysUser;
import com.flowwork.modules.auth.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 权限服务：用户权限码 Redis 缓存
 * key 设计见 db-design，TTL 30 分钟
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private static final String PERM_PREFIX = "flowwork:perm:user:";
    private static final String ROLE_PREFIX = "flowwork:role:user:";
    private static final long TTL = 30;

    private final SysUserMapper userMapper;
    private final StringRedisTemplate redis;

    public List<String> getPermissions(Long userId) {
        String key = PERM_PREFIX + userId;
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            return cached.isEmpty() ? List.of() : List.of(cached.split(","));
        }
        List<String> perms = userMapper.selectPermissionCodes(userId);
        redis.opsForValue().set(key, String.join(",", perms), TTL, TimeUnit.MINUTES);
        return perms;
    }

    public List<String> getRoles(Long userId) {
        String key = ROLE_PREFIX + userId;
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            return cached.isEmpty() ? List.of() : List.of(cached.split(","));
        }
        List<String> roles = userMapper.selectRoleCodes(userId);
        redis.opsForValue().set(key, String.join(",", roles), TTL, TimeUnit.MINUTES);
        return roles;
    }

    public void evict(Long userId) {
        redis.delete(PERM_PREFIX + userId);
        redis.delete(ROLE_PREFIX + userId);
    }

    public boolean hasAny(Long userId, String[] codes) {
        if (codes == null || codes.length == 0) return true;
        List<String> owned = getPermissions(userId);
        for (String c : codes) {
            if (owned.contains(c)) return true;
        }
        return false;
    }

    public boolean hasAll(Long userId, String[] codes) {
        if (codes == null || codes.length == 0) return true;
        List<String> owned = getPermissions(userId);
        for (String c : codes) {
            if (!owned.contains(c)) return false;
        }
        return true;
    }

    public void check(Long userId, String[] codes, RequirePermMode mode) {
        boolean pass = mode == RequirePermMode.AND ? hasAll(userId, codes) : hasAny(userId, codes);
        if (!pass) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    public enum RequirePermMode { AND, OR }

    public SysUser loadUserOrThrow(Long userId) {
        SysUser u = userMapper.selectById(userId);
        if (u == null) throw new BusinessException(ResultCode.UNAUTHORIZED);
        if (u.getStatus() != null && u.getStatus() == 0) {
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED, "账号已禁用");
        }
        return u;
    }
}
