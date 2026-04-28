package com.example.scm.inventory.integration.mq;

import com.alibaba.fastjson2.JSON;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.inventory.application.command.StockOutCommand;
import com.example.scm.inventory.application.command.StockOutItemCommand;
import com.example.scm.inventory.application.command.StockUnlockCommand;
import com.example.scm.inventory.application.command.StockUnlockItemCommand;
import com.example.scm.inventory.application.service.InventoryLockedStockOutApplicationService;
import com.example.scm.inventory.application.service.InventoryStockUnlockApplicationService;
import com.example.scm.inventory.infrastructure.persistence.mapper.MqConsumeLogMapper;
import com.example.scm.inventory.integration.mq.dto.InventoryShipResultEvent;
import com.example.scm.inventory.integration.mq.dto.SalesOrderEvent;
import com.example.scm.inventory.integration.mq.dto.SalesOrderEventItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 销售订单事件消费者。
 * <p>消费发货/取消事件，分别执行库存“冻结转扣减”和“解锁”，并通过 mq_consume_log 实现幂等。</p>
 */
@Component
@Slf4j
public class SalesOrderEventConsumer {

    private static final Long SYSTEM_OPERATOR_ID = 1L;
    private static final String CONSUMER_GROUP = "scm-inventory-order-consumer";
    private static final String BIZ_TYPE = "SALES_ORDER";
    private static final String SHIP_RESULT_TOPIC = "inventory.ship.result.v1";

    private final MqConsumeLogMapper mqConsumeLogMapper;
    private final InventoryLockedStockOutApplicationService lockedStockOutApplicationService;
    private final InventoryStockUnlockApplicationService stockUnlockApplicationService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public SalesOrderEventConsumer(MqConsumeLogMapper mqConsumeLogMapper,
                                   InventoryLockedStockOutApplicationService lockedStockOutApplicationService,
                                   InventoryStockUnlockApplicationService stockUnlockApplicationService,
                                   KafkaTemplate<String, String> kafkaTemplate) {
        this.mqConsumeLogMapper = mqConsumeLogMapper;
        this.lockedStockOutApplicationService = lockedStockOutApplicationService;
        this.stockUnlockApplicationService = stockUnlockApplicationService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "order.ship.requested.v1", groupId = CONSUMER_GROUP)
    public void onShipRequested(ConsumerRecord<String, String> record) {
        log.info("order.ship.requested.v1 topic={}, partition={}, offset={}, key={}, value={}",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());
        SalesOrderEvent event = parseEvent(record);
        if (!normalizeAndValidateEvent(event, record)) {
            return;
        }
        if (isDuplicated(event, record.topic())) {
            return;
        }

        try {
            TenantContext.setTenantId(event.getTenantId());
            lockedStockOutApplicationService.stockOut(toStockOutCommand(event));
            publishShipResult(event, "SUCCESS", null);
            markConsumed(event, record.topic());
            log.info("Consume ship event success, topic={}, partition={}, offset={}, eventId={}, orderNo={}",
                    record.topic(), record.partition(), record.offset(), event.getEventId(), event.getOrderNo());
        } catch (BusinessException ex) {
            if ("Duplicate stock-out request".equals(ex.getMessage())) {
                publishShipResult(event, "SUCCESS", null);
                markConsumed(event, record.topic());
                log.info("Consume ship event as idempotent success, topic={}, partition={}, offset={}, eventId={}, orderNo={}",
                        record.topic(), record.partition(), record.offset(), event.getEventId(), event.getOrderNo());
                return;
            }
            publishShipResult(event, "FAILED", ex.getMessage());
            markConsumed(event, record.topic());
            throw ex;
        } finally {
            TenantContext.clear();
        }
    }

    @KafkaListener(topics = "order.cancel.requested.v1", groupId = CONSUMER_GROUP)
    public void onCancelRequested(ConsumerRecord<String, String> record) {
        SalesOrderEvent event = parseEvent(record);
        if (!normalizeAndValidateEvent(event, record)) {
            return;
        }
        if (isDuplicated(event, record.topic())) {
            return;
        }

        try {
            TenantContext.setTenantId(event.getTenantId());
            stockUnlockApplicationService.unlock(toUnlockCommand(event));
            markConsumed(event, record.topic());
            log.info("Consume cancel event success, topic={}, partition={}, offset={}, eventId={}, orderNo={}",
                    record.topic(), record.partition(), record.offset(), event.getEventId(), event.getOrderNo());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 反序列化 Kafka 消息。
     */
    private SalesOrderEvent parseEvent(ConsumerRecord<String, String> record) {
        try {
            return JSON.parseObject(record.value(), SalesOrderEvent.class);
        } catch (Exception ex) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(),
                    "Parse sales order event failed, topic=" + record.topic());
        }
    }

    /**
     * 标准化并校验事件内容。
     * <p>当 eventId 缺失时使用 topic-partition-offset 兜底，保证可幂等。</p>
     */
    private boolean normalizeAndValidateEvent(SalesOrderEvent event, ConsumerRecord<String, String> record) {
        if (!StringUtils.hasText(event.getEventId())) {
            event.setEventId(record.topic() + "-" + record.partition() + "-" + record.offset());
            log.warn("Sales event has no eventId, use fallback id, topic={}, partition={}, offset={}, orderNo={}",
                    record.topic(), record.partition(), record.offset(), event.getOrderNo());
        }
        if (event.getTenantId() == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "tenantId cannot be null, topic=" + record.topic());
        }
        if (!StringUtils.hasText(event.getOrderNo())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "orderNo cannot be blank, topic=" + record.topic());
        }
        if (event.getWarehouseId() == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "warehouseId cannot be null, topic=" + record.topic());
        }
        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("Sales event has no items, skip consume to avoid retry loop, topic={}, partition={}, offset={}, eventId={}, orderNo={}",
                    record.topic(), record.partition(), record.offset(), event.getEventId(), event.getOrderNo());
            return false;
        }
        return true;
    }

    /**
     * 幂等检查：事件已消费则跳过。
     */
    private boolean isDuplicated(SalesOrderEvent event, String topic) {
        int duplicated = mqConsumeLogMapper.countByUniqueKey(CONSUMER_GROUP, topic, event.getEventId());
        if (duplicated > 0) {
            log.info("Skip duplicated sales event, topic={}, eventId={}, orderNo={}",
                    topic, event.getEventId(), event.getOrderNo());
            return true;
        }
        return false;
    }

    /**
     * 记录消费成功日志，作为后续幂等依据。
     */
    private void markConsumed(SalesOrderEvent event, String topic) {
        mqConsumeLogMapper.insertIgnore(
                event.getTenantId(),
                CONSUMER_GROUP,
                topic,
                event.getEventId(),
                event.getOrderNo()
        );
    }

    /**
     * 将销售事件映射为“已锁库存出库”应用命令。
     */
    private StockOutCommand toStockOutCommand(SalesOrderEvent event) {
        StockOutCommand command = new StockOutCommand();
        command.setBizType(BIZ_TYPE);
        command.setBizNo(event.getOrderNo());
        command.setOperatorId(SYSTEM_OPERATOR_ID);
        command.setItems(toStockOutItems(event));
        return command;
    }

    /**
     * 映射出库明细行。
     */
    private List<StockOutItemCommand> toStockOutItems(SalesOrderEvent event) {
        List<StockOutItemCommand> items = new ArrayList<>();
        for (SalesOrderEventItem eventItem : event.getItems()) {
            StockOutItemCommand item = new StockOutItemCommand();
            item.setMaterialId(eventItem.getMaterialId());
            item.setWarehouseId(event.getWarehouseId());
            item.setLocationId(eventItem.getLocationId());
            item.setQuantity(eventItem.getQuantity());
            items.add(item);
        }
        return items;
    }

    /**
     * 将销售事件映射为“库存解锁”应用命令。
     */
    private StockUnlockCommand toUnlockCommand(SalesOrderEvent event) {
        StockUnlockCommand command = new StockUnlockCommand();
        command.setBizType(BIZ_TYPE);
        command.setBizNo(event.getOrderNo());
        command.setOperatorId(SYSTEM_OPERATOR_ID);
        command.setItems(toUnlockItems(event));
        return command;
    }

    /**
     * 映射解锁明细行。
     */
    private List<StockUnlockItemCommand> toUnlockItems(SalesOrderEvent event) {
        List<StockUnlockItemCommand> items = new ArrayList<>();
        for (SalesOrderEventItem eventItem : event.getItems()) {
            StockUnlockItemCommand item = new StockUnlockItemCommand();
            item.setMaterialId(eventItem.getMaterialId());
            item.setWarehouseId(event.getWarehouseId());
            item.setLocationId(eventItem.getLocationId());
            item.setQuantity(eventItem.getQuantity());
            items.add(item);
        }
        return items;
    }

    private void publishShipResult(SalesOrderEvent event, String status, String failureReason) {
        InventoryShipResultEvent result = new InventoryShipResultEvent();
        result.setEventId(UUID.randomUUID().toString());
        result.setRequestEventId(event.getEventId());
        result.setTenantId(event.getTenantId());
        result.setOrderNo(event.getOrderNo());
        result.setStatus(status);
        result.setFailureReason(failureReason);
        kafkaTemplate.send(SHIP_RESULT_TOPIC, event.getOrderNo(), JSON.toJSONString(result));
    }
}
