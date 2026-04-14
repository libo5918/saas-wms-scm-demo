package com.example.scm.mdm.constant;

public final class MasterDataStatus {

    public static final int DISABLED = 0;
    public static final int ENABLED = 1;

    private MasterDataStatus() {
    }

    public static boolean isValid(Integer status) {
        return status != null && (status == DISABLED || status == ENABLED);
    }
}
