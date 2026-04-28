package com.example.scm.inventory.integration.mq.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 销售订单领域事件（库存侧消费模型）。
 */
public class SalesOrderEvent {

    /** 事件ID（幂等主键的一部分）。 */
    private String eventId;
    /** 事件类型。 */
    private String eventType;
    /** 租户ID。 */
    private Long tenantId;
    /** 订单ID。 */
    private Long orderId;
    /** 订单号。 */
    private String orderNo;
    /** 仓库ID。 */
    private Long warehouseId;
    /** 订单明细行。 */
    private List<SalesOrderEventItem> items = new ArrayList<>();

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public List<SalesOrderEventItem> getItems() {
        return items;
    }

    public void setItems(List<SalesOrderEventItem> items) {
        this.items = items;
    }
}
