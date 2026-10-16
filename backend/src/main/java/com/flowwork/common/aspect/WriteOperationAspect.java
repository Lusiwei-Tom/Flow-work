package com.flowwork.common.aspect;

import com.flowwork.common.ai.AiContextHolder;
import com.flowwork.common.annotation.WriteOperation;
import com.flowwork.common.security.SecurityUtils;
import com.flowwork.modules.auth.entity.SysUser;
import com.flowwork.modules.auth.mapper.SysUserMapper;
import com.flowwork.modules.realtime.event.RealtimeEvent;
import com.flowwork.modules.realtime.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 业务写操作切面：
 * - 成功执行后解析返回值中的 projectId / taskId
 * - 自动落库 Activity（persist=true）并广播 STOMP 事件
 * - AI 调用链（AiContextHolder）中执行时 aiGenerated=true
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class WriteOperationAspect {

    private final ActivityService activityService;
    private final SysUserMapper userMapper;

    /**
     * 环绕通知：拦截所有标注了 @WriteOperation 的方法
     * @Around("@annotation(wo)") 含义：
     *   - 切点表达式 @annotation(wo) 匹配"方法上存在某个注解"，并将该注解绑定到参数 wo
     *   - 参数名 wo 必须与切点中的变量名一致，框架自动注入 @WriteOperation 注解实例
     *   - 优先放行目标方法（pjp.proceed()），成功后再记录活动，失败则跳过
     */
    @Around("@annotation(wo)")
    public Object around(ProceedingJoinPoint pjp, WriteOperation wo) throws Throwable {
        Object result = pjp.proceed();
        try {
            publish(wo, result);
        } catch (Exception e) {
            log.warn("写操作动态记录失败 type={}: {}", wo.type(), e.getMessage());
        }
        return result;
    }

    /**
     * 核心发布逻辑：从返回值提取 projectId/taskId，判断来源（AI/人工），
     * 根据 @WriteOperation.persist() 决定落库或直接广播
     */
    private void publish(WriteOperation wo, Object result) {
        if (result == null) return;
        Long projectId = readId(result, "projectId");
        Long taskId = readId(result, "taskId", "id");
        if (projectId == null) return;

        // 判断当前是否在 AI 调用链中（AiContextHolder 由 AiToolAuditAspect 设置）
        boolean ai = AiContextHolder.isAiContext();
        Long actorId = SecurityUtils.getCurrentUserIdOrNull();
        String actorName = "";
        if (actorId != null) {
            SysUser u = userMapper.selectById(actorId);
            if (u != null) actorName = u.getNickname();
        }
        // AI 调用链中：操作者统一标记为"AI 助手"
        if (ai) {
            actorName = "AI 助手";
            if (actorId == null) actorId = 0L;
        }
        // 渲染活动文案模板（支持 {field} 占位符替换）
        String content = renderContent(wo.content(), result);

        // persist=true：持久化到 activity 表并广播；persist=false：仅广播不落库
        if (wo.persist()) {
            activityService.record(projectId, taskId, wo.type(), actorId, actorName, content, ai);
        } else {
            activityService.broadcast(RealtimeEvent.builder()
                    .type(wo.type()).projectId(projectId).taskId(taskId)
                    .actorId(actorId == null ? 0L : actorId).actorName(actorName)
                    .content(content).aiGenerated(ai).payload(result).timestamp(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * 按优先级从返回值中读取 ID：依次尝试多个属性名，取到第一个非 null 的
     */
    private Long readId(Object target, String... props) {
        for (String prop : props) {
            Object v = readProperty(target, prop);
            if (v instanceof Number n) return n.longValue();
        }
        return null;
    }

    /**
     * 反射读取对象属性值：兼容 Map（直接 get）和 JavaBean（动态调用 getter）
     */
    private Object readProperty(Object target, String prop) {
        try {
            // Map 类型：直接按 key 取值
            if (target instanceof Map<?, ?> map) return map.get(prop);
            // JavaBean 类型：拼装 getter 方法名并反射调用
            String getter = "get" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1);
            Method m = target.getClass().getMethod(getter);
            return m.invoke(target);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 活动文案模板渲染：将 {field} 占位符替换为返回值中对应属性
     * 示例："完成了任务 {title}" → "完成了任务 修复登录Bug"
     */
    private String renderContent(String template, Object result) {
        if (template == null || template.isBlank()) return null;
        String out = template;
        // 正则匹配 {field} 形式的占位符
        var matcher = java.util.regex.Pattern.compile("\\{(\\w+)}").matcher(template);
        while (matcher.find()) {
            Object v = readProperty(result, matcher.group(1));
            out = out.replace(matcher.group(), v == null ? "" : String.valueOf(v));
        }
        return out;
    }
}
