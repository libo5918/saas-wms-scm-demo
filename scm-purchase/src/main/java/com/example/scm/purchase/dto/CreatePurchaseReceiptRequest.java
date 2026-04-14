package com.example.scm.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "创建采购收货单请求。")
public class CreatePurchaseReceiptRequest {

    @Schema(description = "收货单号。")
    @NotBlank(message = "receiptNo cannot be blank")
    private String receiptNo;

    @Schema(description = "关联采购订单ID。")
    @NotNull(message = "purchaseOrderId cannot be null")
    private Long purchaseOrderId;

    @Schema(description = "收货入库的目标仓库ID。")
    @NotNull(message = "warehouseId cannot be null")
    private Long warehouseId;

    @Schema(description = "收货明细列表。")
    @Valid
    @NotEmpty(message = "items cannot be empty")
    private List<CreatePurchaseReceiptItemRequest> items = new ArrayList<>();

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public List<CreatePurchaseReceiptItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CreatePurchaseReceiptItemRequest> items) {
        this.items = items;
    }
}
