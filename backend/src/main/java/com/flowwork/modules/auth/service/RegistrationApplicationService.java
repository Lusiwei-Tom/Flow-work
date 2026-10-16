package com.flowwork.modules.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flowwork.common.exception.BusinessException;
import com.flowwork.common.result.ResultCode;
import com.flowwork.common.security.SecurityUtils;
import com.flowwork.modules.auth.entity.InviteCode;
import com.flowwork.modules.auth.entity.RegistrationApplication;
import com.flowwork.modules.auth.mapper.RegistrationApplicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationApplicationService {

    private final RegistrationApplicationMapper applicationMapper;
    private final InviteCodeService inviteCodeService;

    /** 公开：提交注册申请 */
    public RegistrationApplication submit(RegistrationApplication req) {
        if (req.getApplicantName() == null || req.getApplicantName().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "姓名不能为空");
        }
        if (req.getReason() == null || req.getReason().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "申请理由不能为空");
        }
        RegistrationApplication app = new RegistrationApplication();
        app.setApplicantName(req.getApplicantName());
        app.setPhone(req.getPhone());
        app.setEmail(req.getEmail());
        app.setReason(req.getReason());
        app.setStatus("pending");
        applicationMapper.insert(app);
        return app;
    }

    /** admin：申请列表（可按状态过滤） */
    public List<RegistrationApplication> list(String status) {
        LambdaQueryWrapper<RegistrationApplication> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(RegistrationApplication::getStatus, status);
        }
        qw.orderByDesc(RegistrationApplication::getCreatedAt);
        return applicationMapper.selectList(qw);
    }

    /** admin：审批通过 — 自动生成一个 member 角色、一次性的邀请码 */
    @Transactional
    public RegistrationApplication approve(Long id, String reviewNote) {
        RegistrationApplication app = applicationMapper.selectById(id);
        if (app == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!"pending".equals(app.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_CONFLICT, "该申请已处理");
        }
        InviteCode ic = new InviteCode();
        ic.setPresetRole("member");
        ic.setMaxUses(1);
        ic.setExpiresAt(LocalDateTime.now().plusDays(7));
        InviteCode saved = inviteCodeService.create(ic);

        app.setStatus("approved");
        app.setReviewNote(reviewNote);
        app.setInviteCode(saved.getCode());
        app.setReviewedBy(SecurityUtils.getCurrentUserId());
        app.setReviewedAt(LocalDateTime.now());
        applicationMapper.updateById(app);
        return app;
    }

    /** admin：审批拒绝 */
    public RegistrationApplication reject(Long id, String reviewNote) {
        RegistrationApplication app = applicationMapper.selectById(id);
        if (app == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!"pending".equals(app.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_CONFLICT, "该申请已处理");
        }
        app.setStatus("rejected");
        app.setReviewNote(reviewNote);
        app.setReviewedBy(SecurityUtils.getCurrentUserId());
        app.setReviewedAt(LocalDateTime.now());
        applicationMapper.updateById(app);
        return app;
    }
}
