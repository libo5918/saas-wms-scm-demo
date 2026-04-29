package com.example.scm.sales.support;

import com.alibaba.fastjson2.JSON;
import com.example.scm.sales.entity.OutboxEvent;
import com.example.scm.sales.entity.OutboxEventStatus;
import com.example.scm.sales.entity.SalesOrder;
import com.example.scm.sales.entity.SalesOrderItem;
import com.example.scm.sales.entity.SalesOrderStatus;
import com.example.scm.sales.integration.mq.dto.SalesOrderEventItemPayload;
import com.example.scm.sales.integration.mq.dto.SalesOrderEventPayload;
import com.example.scm.sales.mapper.OutboxEventMapper;
import com.example.scm.sales.mapper.SalesOrderItemMapper;
import com.example.scm.sales.mapper.SalesOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class PendingOrderCompensationJob {

    private static final String AGGREGATE_TYPE = "SALES_ORDER";

    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final OutboxEventMapper outboxEventMapper;
    private final int timeoutMinutes;
    private final int scanLimit;
    private final int cooldownMinutes;
    private final int maxCompensateEventsPerOrder;

    public PendingOrderCompensationJob(SalesOrderMapper salesOrderMapper,
                                       SalesOrderItemMapper salesOrderItemMapper,
                                       OutboxEventMapper outboxEventMapper,
                                       @Value("${compensation.pending-order.timeout-minutes:10}") int timeoutMinutes,
                                       @Value("${compensation.pending-order.limit:100}") int scanLimit,
                                       @Value("${compensation.pending-order.cooldown-minutes:15}") int cooldownMinutes,
                                       @Value("${compensation.pending-order.max-events-per-order:5}") int maxCompensateEventsPerOrder) {
        this.salesOrderMapper = salesOrderMapper;
        this.salesOrderItemMapper = salesOrderItemMapper;
        this.outboxEventMapper = outboxEventMapper;
        this.timeoutMinutes = timeoutMinutes;
        this.scanLimit = scanLimit;
        this.cooldownMinutes = cooldownMinutes;
        this.maxCompensateEventsPerOrder = maxCompensateEventsPerOrder;
    }

    @Scheduled(fixedDelayString = "${compensation.pending-order.fixed-delay-ms:60000}")
    public void compensatePendingOrders() {
        List<SalesOrder> timeoutOrders = salesOrderMapper.selectTimeoutPending(timeoutMinutes, scanLimit);
        for (SalesOrder order : timeoutOrders) {
            SalesOrderStatus status = SalesOrderStatus.valueOf(order.getOrderStatus());
            if (status == SalesOrderStatus.SHIP_PENDING) {
                compensateOrder(order, "ORDER_SHIP_REQUESTED", "order.ship.requested.v1");
            } else if (status == SalesOrderStatus.CANCEL_PENDING) {
                compensateOrder(order, "ORDER_CANCEL_REQUESTED", "order.cancel.requested.v1");
            }
        }
    }

    private void compensateOrder(SalesOrder order, String eventType, String topic) {
        String aggregateId = String.valueOf(order.getId());
        int unsent = outboxEventMapper.countUnsentByAggregate(AGGREGATE_TYPE, String.valueOf(order.getId()), eventType);
        if (unsent > 0) {
            return;
        }
        int recent = outboxEventMapper.countRecentByAggregate(AGGREGATE_TYPE, aggregateId, eventType, cooldownMinutes);
        if (recent > 0) {
            return;
        }
        int total = outboxEventMapper.countByAggregate(AGGREGATE_TYPE, aggregateId, eventType);
        if (total >= maxCompensateEventsPerOrder) {
            log.error("Skip compensation due to max compensate limit reached, orderId={}, orderNo={}, status={}, eventType={}, total={}, max={}",
                    order.getId(), order.getOrderNo(), order.getOrderStatus(), eventType, total, maxCompensateEventsPerOrder);
            return;
        }
        List<SalesOrderItem> items = salesOrderItemMapper.selectByOrderId(order.getTenantId(), order.getId());
        OutboxEvent event = new OutboxEvent();
        event.setTenantId(order.getTenantId());
        event.setEventId(UUID.randomUUID().toString());
        event.setAggregateType(AGGREGATE_TYPE);
        event.setAggregateId(String.valueOf(order.getId()));
        event.setEventType(eventType);
        event.setEventKey(order.getOrderNo());
        event.setTopic(topic);
        event.setPayloadJson(buildPayload(event.getEventId(), order, items, eventType));
        event.setStatus(OutboxEventStatus.NEW.name());
        event.setRetryCount(0);
        event.setNextRetryTime(null);
        outboxEventMapper.insert(event);
        log.warn("Compensate pending order by appending outbox event, orderId={}, orderNo={}, status={}, eventType={}",
                order.getId(), order.getOrderNo(), order.getOrderStatus(), eventType);
    }

    private String buildPayload(String eventId, SalesOrder order, List<SalesOrderItem> items, String eventType) {
        SalesOrderEventPayload payload = new SalesOrderEventPayload();
        payload.setEventId(eventId);
        payload.setEventType(eventType);
        payload.setTenantId(order.getTenantId());
        payload.setOrderId(order.getId());
        payload.setOrderNo(order.getOrderNo());
        payload.setWarehouseId(order.getWarehouseId());
        List<SalesOrderEventItemPayload> payloadItems = new ArrayList<>();
        for (SalesOrderItem item : items) {
            SalesOrderEventItemPayload payloadItem = new SalesOrderEventItemPayload();
            payloadItem.setMaterialId(item.getMaterialId());
            payloadItem.setLocationId(item.getLocationId());
            payloadItem.setQuantity(item.getSaleQty());
            payloadItems.add(payloadItem);
        }
        payload.setItems(payloadItems);
        return JSON.toJSONString(payload);
    }
}
