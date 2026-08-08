package com.skala.shopping.returns.internal.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class CreateReturnRequest {
    @NotNull private UUID orderId;
    @NotNull private UUID orderItemId;
    @Min(1) @Max(1_000_000) private int quantity;
    @NotBlank private String reason;
    @Size(max=1000) private String evidenceImageUrl;
    public CreateReturnRequest(){}
    public UUID getOrderId(){return orderId;} public void setOrderId(UUID value){orderId=value;}
    public UUID getOrderItemId(){return orderItemId;} public void setOrderItemId(UUID value){orderItemId=value;}
    public int getQuantity(){return quantity;} public void setQuantity(int value){quantity=value;}
    public String getReason(){return reason;} public void setReason(String value){reason=value;}
    public String getEvidenceImageUrl(){return evidenceImageUrl;}
    public void setEvidenceImageUrl(String value){evidenceImageUrl=value;}
}
