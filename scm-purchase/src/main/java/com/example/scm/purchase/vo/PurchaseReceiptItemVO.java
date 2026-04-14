package com.example.scm.purchase.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "采购收货单明细返回视图。")
public class PurchaseReceiptItemVO {

    @Schema(description = "明细主键ID。")
    private Long id;

    @Schema(description = "物料ID。")
    private Long materialId;

    @Schema(description = "库位ID。")
    private Long locationId;

    @Schema(description = "收货数量。")
    private BigDecimal receiptQty;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
