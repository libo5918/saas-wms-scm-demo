package com.example.scm.common.security;

public final class GatewayHeaders {

    public static final String TENANT_ID = "X-Tenant-Id";
    public static final String USER_ID = "X-User-Id";
    public static final String USERNAME = "X-User-Name";
    public static final String USER_ROLES = "X-User-Roles";
    public static final String GATEWAY_INTERNAL = "X-Gateway-Internal";
    public static final String GATEWAY_SECRET = "X-Gateway-Secret";

    private GatewayHeaders() {
    }
}
