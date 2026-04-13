package com.example.scm.purchase.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "采购收货单实体，表示一次采购到货的单头信息。")
public class PurchaseReceipt {

    private Long id;
    private Long tenantId;
    private String receiptNo;
    private Long purchaseOrderId;
    private Long warehouseId;
    private String receiptStatus;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
