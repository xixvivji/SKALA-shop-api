package com.skala.shopping.returns;

import com.skala.shopping.common.PageResponse;
import java.util.UUID;

public interface ReturnApi {
    ReturnView request(UUID memberId, UUID orderId, UUID orderItemId, int quantity,
                       String reason, String evidenceImageUrl, UUID commandId);
    PageResponse<ReturnView> getMine(UUID memberId, int page, int size);
    PageResponse<ReturnView> getAll(int page, int size);
    ReturnView changeStatus(UUID adminId, UUID returnId, String status, String adminNote,
                            UUID commandId);
}
