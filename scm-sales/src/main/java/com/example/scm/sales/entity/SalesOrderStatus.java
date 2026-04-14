package com.example.scm.sales.entity;

public enum SalesOrderStatus {
    CREATED,
    LOCK_SUCCESS,
    LOCK_FAILED,
    SHIP_FAILED,
    SHIPPED,
    CANCELLED;

    public boolean isLockSuccess() {
        return this == LOCK_SUCCESS;
    }

    public boolean canShip() {
        return this == LOCK_SUCCESS;
    }

    public boolean canRetryShip() {
        return this == SHIP_FAILED;
    }

    public boolean canRetryLock() {
        return this == LOCK_FAILED;
    }

    public boolean canCancel() {
        return this == CREATED || this == LOCK_FAILED || this == LOCK_SUCCESS || this == SHIP_FAILED;
    }
}
