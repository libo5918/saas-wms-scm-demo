package com.example.scm.purchase.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单明细实体，记录每个物料的采购数量和单价。
 */
@Getter
@Setter
@Schema(description = "采购订单明细实体，记录每个物料的采购数量和单价。")
public class PurchaseOrderItem {

    @Schema(description = "明细主键ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "采购订单ID")
    private Long purchaseOrderId;

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "计划采购数量")
    private BigDecimal planQty;

    @Schema(description = "已收货数量")
    private BigDecimal receivedQty;

    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
