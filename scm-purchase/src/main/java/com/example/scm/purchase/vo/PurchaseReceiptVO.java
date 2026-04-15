package com.example.scm.purchase.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Schema(description = "采购收货单返回视图，包含单头和明细信息。")
public class PurchaseReceiptVO {

    @Schema(description = "收货单主键ID。")
    private Long id;

    @Schema(description = "收货单号。")
    private String receiptNo;

    @Schema(description = "采购订单ID。")
    private Long purchaseOrderId;

    @Schema(description = "供应商ID。")
    private Long supplierId;

    @Schema(description = "仓库ID。")
    private Long warehouseId;

    @Schema(description = "收货单状态。")
    private String receiptStatus;

    @Schema(description = "库存联动失败原因。")
    private String failureReason;

    @Schema(description = "收货明细列表。")
    private List<PurchaseReceiptItemVO> items = new ArrayList<>();
}
