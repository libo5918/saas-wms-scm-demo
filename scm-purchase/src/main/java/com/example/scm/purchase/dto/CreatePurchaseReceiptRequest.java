package com.example.scm.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CreatePurchaseReceiptRequest {

    @NotBlank(message = "receiptNo cannot be blank")
    private String receiptNo;

    @NotNull(message = "purchaseOrderId cannot be null")
    private Long purchaseOrderId;

    @NotNull(message = "warehouseId cannot be null")
    private Long warehouseId;

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
