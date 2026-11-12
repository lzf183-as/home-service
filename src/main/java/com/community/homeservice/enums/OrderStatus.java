package com.community.homeservice.enums;

/**
 * 订单状态机：
 * PENDING_ACCEPT -> ACCEPTED -> SERVING -> PENDING_CONFIRM -> COMPLETED
 * 任意未完成状态可 -> CANCELLED（待接单由用户取消，已接单需管理员/员工协调后取消）
 */
public enum OrderStatus {

    PENDING_ACCEPT("待接单", "warning"),
    ACCEPTED("已接单", "info"),
    SERVING("服务中", "primary"),
    PENDING_CONFIRM("待确认", "purple"),
    COMPLETED("已完成", "success"),
    CANCELLED("已取消", "muted");

    private final String label;
    private final String tone;

    OrderStatus(String label, String tone) {
        this.label = label;
        this.tone = tone;
    }

    public String getLabel() {
        return label;
    }

    public String getTone() {
        return tone;
    }

    public static String labelOf(String name) {
        for (OrderStatus s : values()) {
            if (s.name().equals(name)) {
                return s.label;
            }
        }
        return name;
    }

    public static String toneOf(String name) {
        for (OrderStatus s : values()) {
            if (s.name().equals(name)) {
                return s.tone;
            }
        }
        return "muted";
    }

    public static boolean canCancel(String name) {
        return PENDING_ACCEPT.name().equals(name) || ACCEPTED.name().equals(name);
    }
}
