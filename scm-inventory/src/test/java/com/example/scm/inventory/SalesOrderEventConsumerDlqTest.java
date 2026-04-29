package com.example.scm.inventory;

import com.example.scm.inventory.application.service.InventoryLockedStockOutApplicationService;
import com.example.scm.inventory.application.service.InventoryStockUnlockApplicationService;
import com.example.scm.inventory.infrastructure.persistence.mapper.MqConsumeLogMapper;
import com.example.scm.inventory.integration.mq.SalesOrderEventConsumer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SalesOrderEventConsumerDlqTest {

    @Test
    void shouldPublishToDlqWhenShipRequestPayloadInvalidJson() {
        MqConsumeLogMapper mqConsumeLogMapper = mock(MqConsumeLogMapper.class);
        InventoryLockedStockOutApplicationService lockedStockOutService = mock(InventoryLockedStockOutApplicationService.class);
        InventoryStockUnlockApplicationService unlockService = mock(InventoryStockUnlockApplicationService.class);
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        SalesOrderEventConsumer consumer = new SalesOrderEventConsumer(
                mqConsumeLogMapper,
                lockedStockOutService,
                unlockService,
                kafkaTemplate,
                "scm-inventory-order-consumer",
                "inventory.ship.result.v1",
                "inventory.cancel.result.v1",
                "sales.order.requested.dlq.v1",
                meterRegistry
        );

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "order.ship.requested.v1", 0, 12L, "SO-BAD-100", "{bad json}"
        );

        assertDoesNotThrow(() -> consumer.onShipRequested(record));
        verify(kafkaTemplate, times(1)).send(eq("sales.order.requested.dlq.v1"), eq("SO-BAD-100"), anyString());
        assertEquals(1.0, meterRegistry.counter("scm.inventory.mq.consume.dead-letter").count());
    }
}
