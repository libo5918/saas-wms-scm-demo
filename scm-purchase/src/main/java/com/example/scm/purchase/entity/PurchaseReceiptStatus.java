package com.example.scm.purchase.entity;

/**
 * 采购收货单状态。
 *
 * <p>当前版本主要覆盖“创建后立即尝试入库”的处理流程：</p>
 * <p>1. CREATED：收货单已落库，但库存联动尚未完成。</p>
 * <p>2. STOCK_IN_SUCCESS：库存联动成功，流程完成。</p>
 * <p>3. STOCK_IN_FAILED：库存联动失败，可在原单据上继续重试。</p>
 * <p>4. CANCELLED：收货单已取消，不再允许继续入库。</p>
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
