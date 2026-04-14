package com.example.scm.purchase.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "采购收货单明细实体，表示单个物料的收货数量和库位。")
public class PurchaseReceiptItem {

    @Schema(description = "明细主键ID。")
    private Long id;

    @Schema(description = "租户ID。")
    private Long tenantId;

    @Schema(description = "收货单ID。")
    private Long purchaseReceiptId;

    @Schema(description = "物料ID。")
    private Long materialId;

    @Schema(description = "库位ID。")
    private Long locationId;

    @Schema(description = "收货数量。")
    private BigDecimal receiptQty;

    @Schema(description = "创建时间。")
    private LocalDateTime createdAt;
}
