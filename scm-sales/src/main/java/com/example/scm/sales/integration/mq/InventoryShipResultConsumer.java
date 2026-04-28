package com.example.scm.sales.integration.mq;

import com.alibaba.fastjson2.JSON;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.sales.entity.SalesOrder;
import com.example.scm.sales.entity.SalesOrderStatus;
import com.example.scm.sales.integration.mq.dto.InventoryShipResultEvent;
import com.example.scm.sales.mapper.MqConsumeLogMapper;
import com.example.scm.sales.mapper.SalesOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class InventoryShipResultConsumer {

    private static final String CONSUMER_GROUP = "scm-sales-ship-result-consumer";
    private static final String TOPIC = "inventory.ship.result.v1";
    private static final long SYSTEM_OPERATOR_ID = 1L;

    private final MqConsumeLogMapper mqConsumeLogMapper;
    private final SalesOrderMapper salesOrderMapper;

    public InventoryShipResultConsumer(MqConsumeLogMapper mqConsumeLogMapper, SalesOrderMapper salesOrderMapper) {
        this.mqConsumeLogMapper = mqConsumeLogMapper;
        this.salesOrderMapper = salesOrderMapper;
    }

    @KafkaListener(topics = TOPIC, groupId = CONSUMER_GROUP)
    public void onShipResult(ConsumerRecord<String, String> record) {
        log.info("onShipResult topic={}, partition={}, offset={}, key={}, value={}",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());
        InventoryShipResultEvent event = parse(record.value());
        validate(event);
        if (mqConsumeLogMapper.countByUniqueKey(CONSUMER_GROUP, TOPIC, event.getEventId()) > 0) {
            return;
        }

        try {
            TenantContext.setTenantId(event.getTenantId());
            SalesOrder order = salesOrderMapper.selectByOrderNo(event.getTenantId(), event.getOrderNo())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Sales order not found"));
            SalesOrderStatus currentStatus = SalesOrderStatus.valueOf(order.getOrderStatus());
            if (currentStatus == SalesOrderStatus.CANCELLED) {
                markConsumed(event);
                return;
            }

            if ("SUCCESS".equals(event.getStatus())) {
                if (currentStatus != SalesOrderStatus.SHIPPED) {
                    salesOrderMapper.updateStatus(event.getTenantId(), order.getId(), SalesOrderStatus.SHIPPED.name(), null, SYSTEM_OPERATOR_ID);
                }
            } else {
                String reason = truncateFailureReason(event.getFailureReason());
                if (currentStatus != SalesOrderStatus.SHIP_FAILED) {
                    salesOrderMapper.updateStatus(event.getTenantId(), order.getId(), SalesOrderStatus.SHIP_FAILED.name(), reason, SYSTEM_OPERATOR_ID);
                }
            }
            markConsumed(event);
        } finally {
            TenantContext.clear();
        }
    }

    private InventoryShipResultEvent parse(String payload) {
        try {
            return JSON.parseObject(payload, InventoryShipResultEvent.class);
        } catch (Exception ex) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Parse inventory ship result event failed");
        }
    }

    private void validate(InventoryShipResultEvent event) {
        if (!StringUtils.hasText(event.getEventId())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "eventId cannot be blank");
        }
        if (event.getTenantId() == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "tenantId cannot be null");
        }
        if (!StringUtils.hasText(event.getOrderNo())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "orderNo cannot be blank");
        }
        if (!"SUCCESS".equals(event.getStatus()) && !"FAILED".equals(event.getStatus())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "status must be SUCCESS or FAILED");
        }
    }

    private void markConsumed(InventoryShipResultEvent event) {
        mqConsumeLogMapper.insertIgnore(event.getTenantId(), CONSUMER_GROUP, TOPIC, event.getEventId(), event.getOrderNo());
    }

    private String truncateFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return null;
        }
        return failureReason.length() <= 255 ? failureReason : failureReason.substring(0, 255);
    }
}
