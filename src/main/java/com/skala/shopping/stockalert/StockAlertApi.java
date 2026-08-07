package com.skala.shopping.stockalert;

import com.skala.shopping.common.PageResponse;
import java.util.UUID;

public interface StockAlertApi {

    StockAlertResponse subscribe(UUID memberId, UUID productId);

    void unsubscribe(UUID memberId, UUID productId);

    PageResponse<StockAlertResponse> getSubscriptions(UUID memberId, int page, int size);
}
