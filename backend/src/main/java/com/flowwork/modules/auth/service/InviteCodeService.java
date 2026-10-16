package com.flowwork.modules.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flowwork.common.exception.BusinessException;
import com.flowwork.common.result.ResultCode;
import com.flowwork.common.security.SecurityUtils;
import com.flowwork.modules.auth.entity.InviteCode;
import com.flowwork.modules.auth.mapper.InviteCodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InviteCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final InviteCodeMapper inviteCodeMapper;

    private static final List<String> ALLOWED_ROLES = Arrays.asList("admin", "project_manager", "member");
    private static final List<String> ALLOWED_PROJECT_ROLES = Arrays.asList("MANAGER", "MEMBER", "OBSERVER");

    /** 生成邀请码 */
    @Transactional
    public InviteCode create(InviteCode req) {
        String role = req.getPresetRole() == null ? "member" : req.getPresetRole();
        if (!ALLOWED_ROLES.contains(role)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的角色类型");
        }
        if (req.getPresetProjectRole() != null && !req.getPresetProjectRole().isBlank()
                && !ALLOWED_PROJECT_ROLES.contains(req.getPresetProjectRole())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的项目角色");
        }
        if (req.getMaxUses() == null || req.getMaxUses() < 0) req.setMaxUses(1);

        InviteCode ic = new InviteCode();
        ic.setCode(generateCode());
        ic.setPresetRole(role);
        ic.setPresetProjectId(req.getPresetProjectId());
        ic.setPresetProjectRole(req.getPresetProjectRole());
        ic.setMaxUses(req.getMaxUses());
        ic.setUsedCount(0);
        ic.setExpiresAt(req.getExpiresAt());
        ic.setCreatedBy(SecurityUtils.getCurrentUserId());
        ic.setStatus("active");
        inviteCodeMapper.insert(ic);
        return ic;
    }

    /** 全部邀请码列表 */
    public List<InviteCode> list() {
        return inviteCodeMapper.selectList(new LambdaQueryWrapper<InviteCode>()
                .orderByDesc(InviteCode::getCreatedAt));
    }

    /** 停用 */
    public void disable(Long id) {
        InviteCode ic = inviteCodeMapper.selectById(id);
        if (ic == null) throw new BusinessException(ResultCode.NOT_FOUND);
        ic.setStatus("disabled");
        inviteCodeMapper.updateById(ic);
    }

    /** 删除 */
    public void delete(Long id) {
        inviteCodeMapper.deleteById(id);
    }

    /**
     * 校验并使用邀请码，返回邀请码记录（校验失败抛异常）。
     * 调用方在用户创建成功后需调用 markUsed。
     */
    public InviteCode validate(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "邀请码不能为空");
        }
        InviteCode ic = inviteCodeMapper.selectOne(new LambdaQueryWrapper<InviteCode>()
                .eq(InviteCode::getCode, code.trim())
                .last("LIMIT 1"));
        if (ic == null || !"active".equals(ic.getStatus())) {
            throw new BusinessException(ResultCode.INVITE_CODE_INVALID);
        }
        if (ic.getExpiresAt() != null && ic.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.INVITE_CODE_EXPIRED, "邀请码已过期");
        }
        if (ic.getMaxUses() != null && ic.getMaxUses() > 0
                && ic.getUsedCount() >= ic.getMaxUses()) {
            throw new BusinessException(ResultCode.INVITE_CODE_EXPIRED, "邀请码使用次数已用尽");
        }
        return ic;
    }

    /** 注册成功后标记已使用（原子自增） */
    public void markUsed(Long id) {
        inviteCodeMapper.incrementUsedCount(id);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder("FW-");
        for (int i = 0; i < 8; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
