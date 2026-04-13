package com.example.scm.purchase.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class CreatePurchaseReceiptItemRequest {

    @NotNull(message = "materialId cannot be null")
    private Long materialId;

    @NotNull(message = "locationId cannot be null")
    private Long locationId;

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
