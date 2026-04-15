package com.example.scm.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 创建采购订单明细请求。
 */
@Schema(description = "创建采购订单明细请求。")
public class CreatePurchaseOrderItemRequest {

    @Schema(description = "物料ID")
    @NotNull(message = "materialId cannot be null")
    private Long materialId;

    @Schema(description = "计划采购数量")
    @NotNull(message = "planQty cannot be null")
    @DecimalMin(value = "0.0001", message = "planQty must be greater than 0")
    private BigDecimal planQty;

    @Schema(description = "单价")
    @NotNull(message = "unitPrice cannot be null")
    @DecimalMin(value = "0.00", message = "unitPrice must be greater than or equal to 0")
    private BigDecimal unitPrice;

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public BigDecimal getPlanQty() {
        return planQty;
    }

    public void setPlanQty(BigDecimal planQty) {
        this.planQty = planQty;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
