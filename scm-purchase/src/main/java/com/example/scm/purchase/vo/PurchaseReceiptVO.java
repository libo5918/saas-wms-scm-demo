package com.example.scm.purchase.vo;

import java.util.ArrayList;
import java.util.List;

public class PurchaseReceiptVO {

    private Long id;
    private String receiptNo;
    private Long purchaseOrderId;
    private Long warehouseId;
    private String receiptStatus;
    private List<PurchaseReceiptItemVO> items = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getReceiptStatus() {
        return receiptStatus;
    }

    public void setReceiptStatus(String receiptStatus) {
        this.receiptStatus = receiptStatus;
    }

    public List<PurchaseReceiptItemVO> getItems() {
        return items;
    }

    public void setItems(List<PurchaseReceiptItemVO> items) {
        this.items = items;
    }
}
