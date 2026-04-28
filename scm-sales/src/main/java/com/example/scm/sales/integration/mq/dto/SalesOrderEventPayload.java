package com.example.scm.sales.integration.mq.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 销售订单领域事件载荷（发往 Kafka 的消息体）。
 * <p>用于承载订单头信息与明细行，供库存等下游服务消费。</p>
 */
public class SalesOrderEventPayload {

    /** 全局事件ID，用于下游幂等。 */
    private String eventId;
    /** 事件类型，如 ORDER_SHIP_REQUESTED / ORDER_CANCEL_REQUESTED。 */
    private String eventType;
    /** 租户ID。 */
    private Long tenantId;
    /** 销售订单ID。 */
    private Long orderId;
    /** 销售订单号。 */
    private String orderNo;
    /** 发货仓库ID。 */
    private Long warehouseId;
    /** 事件明细行。 */
    private List<SalesOrderEventItemPayload> items = new ArrayList<>();

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

    public List<SalesOrderEventItemPayload> getItems() {
        return items;
    }

    public void setItems(List<SalesOrderEventItemPayload> items) {
        this.items = items;
    }
}
