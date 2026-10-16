package com.flowwork.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowwork.common.ai.AiContextHolder;
import com.flowwork.common.annotation.AiTool;
import com.flowwork.modules.ai.entity.AiToolCallLog;
import com.flowwork.modules.ai.mapper.AiToolCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * AI Tool 审计切面：
 * - 记录每次工具调用的入参/结果/耗时/成败到 ai_tool_call_log
 * - 执行期间设置 AiContextHolder，使下游 WriteOperation 动态标记为 AI 发起
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AiToolAuditAspect {

    private final AiToolCallLogMapper logMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(aiTool)")
    public Object around(ProceedingJoinPoint pjp, AiTool aiTool) throws Throwable {
        //ProceedingJoinPoint 是一个 AOP 框架提供的接口，用于在方法执行前后插入自定义逻辑
        //pjp.getSignature().getName() 获取当前方法的名称，aiTool.value() 获取注解中的工具名称
        String toolName = aiTool.value().isBlank() ? pjp.getSignature().getName() : aiTool.value();
        long start = System.currentTimeMillis();//获取当前时间戳
        AiToolCallLog record = new AiToolCallLog();//创建审计日志记录
        record.setToolName(toolName);//设置工具名称
        record.setUserId(0L);//设置用户 ID
        try {
            record.setToolParams(truncate(objectMapper.writeValueAsString(pjp.getArgs())));//设置工具参数
           
        } catch (Exception ignored) {
        }
        AiContextHolder.enter(toolName);//进入上下文，设置工具名称
        Object result;
        try {
            result = pjp.proceed();//执行目标方法
            record.setSuccess(1);//设置成功标志为 1
            try {
                record.setToolResult(truncate(objectMapper.writeValueAsString(result)));//设置成功标志为 1
            } catch (Exception ignored) {
            }
            return result;
        } catch (Throwable e) {
            record.setSuccess(0);//设置成功标志为 0
            record.setErrorMsg(truncate(e.getMessage()));//设置错误信息
            throw e;
        } finally {
            record.setDurationMs((int) (System.currentTimeMillis() - start));//设置耗时
            try {
                logMapper.insert(record);//插入审计日志记录
            } catch (Exception ex) {
                log.warn("AI 工具审计日志落库失败: {}", ex.getMessage());
            }
            AiContextHolder.exit();//退出上下文，移除工具名称
                   }
    }
     // 截断字符串，防止数据库字段长度超出限制
    private String truncate(String s) {
        if (s == null) return null;
        // 如果字符串长度超过 1900，截断为 1900 字符
        // 否则，返回原始字符串
        return s.length() > 1900 ? s.substring(0, 1900) : s;
    }
}
