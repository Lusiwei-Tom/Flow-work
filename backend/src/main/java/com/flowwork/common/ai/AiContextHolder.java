package com.flowwork.common.ai;

/**
 * AI 调用链 ThreadLocal 标记。
 * AI Tool 执行期间 enter()，realtime 切面据此标记 ai_generated。
 */
public final class AiContextHolder {
    private static final ThreadLocal<Boolean> AI_CONTEXT = ThreadLocal.withInitial(() -> false);// 标记是否为 AI 调用链
    private static final ThreadLocal<String> TOOL_NAME = new ThreadLocal<>();// 当前 AI Tool 名称

    private AiContextHolder() {}

    public static void enter(String toolName) {
        AI_CONTEXT.set(true);// 设置为 AI 调用链
        TOOL_NAME.set(toolName);// 设置当前 AI Tool 名称
    }

    public static boolean isAiContext() {
        return Boolean.TRUE.equals(AI_CONTEXT.get());// 判断是否为 AI 调用链
    }

    public static String currentToolName() {
        return TOOL_NAME.get();// 获取当前 AI Tool 名称
    }

    public static void exit() {
        AI_CONTEXT.remove();// 移除 AI 调用链标记
        TOOL_NAME.remove();// 移除当前 AI Tool 名称
    }
}
