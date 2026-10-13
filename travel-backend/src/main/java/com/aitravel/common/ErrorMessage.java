package com.aitravel.common;

/**
 * 业务错误文案常量。取值逐字对齐详细设计文档（§3.6 / §4.6 / §5.7 / §6.6）。
 */
public final class ErrorMessage {

    /** 用户名已注册（M-USER 注册）。 */
    public static final String USERNAME_EXISTS = "用户名已注册";

    /** 两次密码不一致（M-USER 注册）。 */
    public static final String PASSWORD_NOT_MATCH = "两次密码不一致";

    /** 用户名或密码错误（M-USER 登录，防用户名枚举）。 */
    public static final String BAD_CREDENTIALS = "用户名或密码错误";

    /** 账号已被禁用（M-USER 登录，401）。 */
    public static final String ACCOUNT_DISABLED = "账号已被禁用";

    /** 原密码错误（M-USER 修改密码）。 */
    public static final String OLD_PASSWORD_WRONG = "原密码错误";

    /** 景点不存在（M-SCENIC，404）。 */
    public static final String SCENIC_NOT_FOUND = "资源不存在";

    /** 行程不存在（M-TRIP，404）。 */
    public static final String TRIP_NOT_FOUND = "资源不存在";

    /** 越权访问他人行程（M-TRIP，403）。 */
    public static final String TRIP_FORBIDDEN = "无权限";

    /** 未找到该城市的景点数据（M-TRIP 生成攻略）。 */
    public static final String NO_SCENIC_IN_CITY = "未找到该城市的景点数据";

    /** AI 生成失败（M-TRIP，5001）。 */
    public static final String AI_GENERATE_FAILED = "生成失败，请稍后重试";

    /** AI 问答服务不可用（M-CHAT，5001）。 */
    public static final String AI_CHAT_UNAVAILABLE = "问答服务暂时不可用，请稍后重试";

    private ErrorMessage() {
    }
}
