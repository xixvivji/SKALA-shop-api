package com.skala.shopping.notificationservice.web;

import java.util.List;

public final class NotificationPageResponse {

    private final List<NotificationResponse> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public NotificationPageResponse(
            List<NotificationResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        this.content = List.copyOf(content);
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<NotificationResponse> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}
