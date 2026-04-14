package com.example.scm.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "创建采购收货单明细请求。")
public class CreatePurchaseReceiptItemRequest {

    @Schema(description = "物料ID。")
    @NotNull(message = "materialId cannot be null")
    private Long materialId;

    @Schema(description = "收货入到的库位ID。")
    @NotNull(message = "locationId cannot be null")
    private Long locationId;

    @Schema(description = "本次收货数量。")
    @NotNull(message = "receiptQty cannot be null")
    @Positive(message = "receiptQty must be greater than zero")
    private BigDecimal receiptQty;

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public BigDecimal getReceiptQty() {
        return receiptQty;
    }

    public void setReceiptQty(BigDecimal receiptQty) {
        this.receiptQty = receiptQty;
    }
}
