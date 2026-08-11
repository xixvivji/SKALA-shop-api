package com.skala.shopping.notificationservice.web;

public final class UnreadCountResponse {

    private final long unreadCount;

    public UnreadCountResponse(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public long getUnreadCount() {
        return unreadCount;
    }
}
