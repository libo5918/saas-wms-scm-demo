package com.example.scm.inventory.integration.mq.dto;

import java.math.BigDecimal;

/**
 * 销售订单事件明细行（库存侧消费模型）。
 */
public class SalesOrderEventItem {

    /** 物料ID。 */
    private Long materialId;
    /** 库位ID。 */
    private Long locationId;
    /** 数量。 */
    private BigDecimal quantity;

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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
