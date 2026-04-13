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

    private Long id;
    private Long tenantId;
    private Long purchaseReceiptId;
    private Long materialId;
    private Long locationId;
    private BigDecimal receiptQty;
    private LocalDateTime createdAt;
}
