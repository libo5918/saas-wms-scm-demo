package com.example.scm.sales.integration.mq;

import com.alibaba.fastjson2.JSON;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.sales.entity.SalesOrder;
import com.example.scm.sales.entity.SalesOrderStatus;
import com.example.scm.sales.integration.mq.dto.InventoryCancelResultEvent;
import com.example.scm.sales.mapper.MqConsumeLogMapper;
import com.example.scm.sales.mapper.SalesOrderMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class InventoryCancelResultConsumer {

    private static final long SYSTEM_OPERATOR_ID = 1L;
    private final String consumerGroup;
    private final String topic;
    private final String deadLetterTopic;
    private final Counter consumedSuccessCounter;
    private final Counter consumedFailedCounter;
    private final Counter consumedDeadLetterCounter;

    private final MqConsumeLogMapper mqConsumeLogMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public InventoryCancelResultConsumer(MqConsumeLogMapper mqConsumeLogMapper,
                                         SalesOrderMapper salesOrderMapper,
                                         @Value("${mq.consumer-groups.sales-cancel-result:scm-sales-cancel-result-consumer}") String consumerGroup,
                                         @Value("${mq.topics.inventory-cancel-result:inventory.cancel.result.v1}") String topic,
                                         @Value("${mq.topics.sales-inventory-result-dlq:sales.inventory.result.dlq.v1}") String deadLetterTopic,
                                         KafkaTemplate<String, String> kafkaTemplate,
                                         MeterRegistry meterRegistry) {
        this.mqConsumeLogMapper = mqConsumeLogMapper;
        this.salesOrderMapper = salesOrderMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.consumerGroup = consumerGroup;
        this.topic = topic;
        this.deadLetterTopic = deadLetterTopic;
        this.consumedSuccessCounter = meterRegistry.counter("scm.sales.mq.consume.cancel-result.success");
        this.consumedFailedCounter = meterRegistry.counter("scm.sales.mq.consume.cancel-result.failed");
        this.consumedDeadLetterCounter = meterRegistry.counter("scm.sales.mq.consume.cancel-result.dead-letter");
    }

    @KafkaListener(topics = "${mq.topics.inventory-cancel-result:inventory.cancel.result.v1}", groupId = "${mq.consumer-groups.sales-cancel-result:scm-sales-cancel-result-consumer}")
    public void onCancelResult(ConsumerRecord<String, String> record) {
        log.info("onCancelResult topic={}, partition={}, offset={}, key={}, value={}",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());
        try {
            InventoryCancelResultEvent event = parse(record.value());
            validate(event);
            if (mqConsumeLogMapper.countByUniqueKey(consumerGroup, topic, event.getEventId()) > 0) {
                return;
            }
            TenantContext.setTenantId(event.getTenantId());
            SalesOrder order = salesOrderMapper.selectByOrderNo(event.getTenantId(), event.getOrderNo())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Sales order not found"));
            SalesOrderStatus currentStatus = SalesOrderStatus.valueOf(order.getOrderStatus());
            if (currentStatus == SalesOrderStatus.CANCELLED) {
                markConsumed(event);
                return;
            }

            if ("SUCCESS".equals(event.getStatus())) {
                salesOrderMapper.updateStatus(event.getTenantId(), order.getId(), SalesOrderStatus.CANCELLED.name(), null, SYSTEM_OPERATOR_ID);
            } else {
                String reason = truncateFailureReason(event.getFailureReason());
                salesOrderMapper.updateStatus(event.getTenantId(), order.getId(), SalesOrderStatus.CANCEL_FAILED.name(), reason, SYSTEM_OPERATOR_ID);
            }
            markConsumed(event);
            consumedSuccessCounter.increment();
        } catch (BusinessException ex) {
            consumedFailedCounter.increment();
            publishDeadLetter(record, ex.getMessage());
            log.error("Consume cancel result failed and published to DLQ, topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset(), ex);
        } catch (Exception ex) {
            consumedFailedCounter.increment();
            publishDeadLetter(record, ex.getMessage());
            log.error("Consume cancel result unexpected error and published to DLQ, topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset(), ex);
        } finally {
            TenantContext.clear();
        }
    }

    private InventoryCancelResultEvent parse(String payload) {
        try {
            return JSON.parseObject(payload, InventoryCancelResultEvent.class);
        } catch (Exception ex) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Parse inventory cancel result event failed");
        }
    }

    private void validate(InventoryCancelResultEvent event) {
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

    private void markConsumed(InventoryCancelResultEvent event) {
        mqConsumeLogMapper.insertIgnore(event.getTenantId(), consumerGroup, topic, event.getEventId(), event.getOrderNo());
    }

    private String truncateFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return null;
        }
        return failureReason.length() <= 255 ? failureReason : failureReason.substring(0, 255);
    }

    private void publishDeadLetter(ConsumerRecord<String, String> record, String reason) {
        String escapedReason = reason == null ? "unknown" : reason.replace("\"", "\\\"");
        String payload = "{\"sourceTopic\":\"" + record.topic() + "\","
                + "\"partition\":" + record.partition() + ","
                + "\"offset\":" + record.offset() + ","
                + "\"key\":\"" + (record.key() == null ? "" : record.key().replace("\"", "\\\"")) + "\","
                + "\"value\":" + JSON.toJSONString(record.value()) + ","
                + "\"reason\":\"" + escapedReason + "\"}";
        try {
            kafkaTemplate.send(deadLetterTopic, record.key(), payload).get(5, TimeUnit.SECONDS);
            consumedDeadLetterCounter.increment();
        } catch (Exception e) {
            log.error("Publish cancel-result dead-letter failed, dlqTopic={}, sourceTopic={}, partition={}, offset={}",
                    deadLetterTopic, record.topic(), record.partition(), record.offset(), e);
        }
    }
}
