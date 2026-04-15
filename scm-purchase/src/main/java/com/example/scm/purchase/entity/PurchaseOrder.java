package com.example.scm.purchase.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单实体，表示采购侧的下单主单信息。
 */
@Getter
@Setter
@Schema(description = "采购订单实体，表示采购侧的下单主单信息。")
public class PurchaseOrder {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

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

    @Schema(description = "创建人")
    private Long createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新人")
    private Long updatedBy;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "逻辑删除标记")
    private Integer deleted;
}
