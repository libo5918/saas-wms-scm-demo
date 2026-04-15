package com.example.scm.purchase.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 采购订单返回视图。
 */
@Getter
@Setter
@Schema(description = "采购订单返回视图。")
public class PurchaseOrderVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "采购订单号")
    private String orderNo;

    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "采购订单状态")
    private String orderStatus;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "明细列表")
    private List<PurchaseOrderItemVO> items = new ArrayList<>();
}
