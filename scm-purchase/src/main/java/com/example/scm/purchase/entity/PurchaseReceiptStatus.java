package com.example.scm.purchase.entity;

/**
 * 采购收货单状态。
 */
public enum PurchaseReceiptStatus {
    CREATED,
    STOCK_IN_SUCCESS,
    STOCK_IN_FAILED,
    CANCELLED;

    /**
     * 当前状态是否允许再次触发库存入库。
     */
    public boolean canRetryStockIn() {
        return this == STOCK_IN_FAILED;
    }

    /**
     * 当前状态是否允许取消。
     */
    public boolean canCancel() {
        return this == CREATED || this == STOCK_IN_FAILED;
    }

    /**
     * 当前状态是否已经完成入库。
     */
    public boolean isStockInSuccess() {
        return this == STOCK_IN_SUCCESS;
    }
}
