package com.example.scm.purchase.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 采购订单明细返回视图。
 */
@Getter
@Setter
@Schema(description = "采购订单明细返回视图。")
public class PurchaseOrderItemVO {

    @Schema(description = "明细主键ID")
    private Long id;

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "计划采购数量")
    private BigDecimal planQty;

    @Schema(description = "已收货数量")
    private BigDecimal receivedQty;

    @Schema(description = "单价")
    private BigDecimal unitPrice;
}
