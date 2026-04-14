package com.example.scm.purchase.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 采购收货单实体，表示一次到货收货的单头信息。
 */
@Getter
@Setter
@Schema(description = "采购收货单实体，表示一次到货收货的单头信息。")
public class PurchaseReceipt {

    @Schema(description = "收货单主键ID。")
    private Long id;

    @Schema(description = "租户ID。")
    private Long tenantId;

    @Schema(description = "收货单号。")
    private String receiptNo;

    @Schema(description = "关联采购订单ID。")
    private Long purchaseOrderId;

    @Schema(description = "收货仓库ID。")
    private Long warehouseId;

    @Schema(description = "收货单状态。")
    private String receiptStatus;

    @Schema(description = "库存联动失败原因，成功时为空。")
    private String failureReason;

    @Schema(description = "创建人。")
    private Long createdBy;

    @Schema(description = "创建时间。")
    private LocalDateTime createdAt;

    @Schema(description = "更新人。")
    private Long updatedBy;

    @Schema(description = "更新时间。")
    private LocalDateTime updatedAt;

    @Schema(description = "逻辑删除标记。")
    private Integer deleted;
}
