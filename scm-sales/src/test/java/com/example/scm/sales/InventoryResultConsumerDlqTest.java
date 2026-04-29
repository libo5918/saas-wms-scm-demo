package com.example.scm.sales;

import com.example.scm.sales.integration.mq.InventoryCancelResultConsumer;
import com.example.scm.sales.integration.mq.InventoryShipResultConsumer;
import com.example.scm.sales.mapper.MqConsumeLogMapper;
import com.example.scm.sales.mapper.SalesOrderMapper;
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

class InventoryResultConsumerDlqTest {

    @Test
    void shouldPublishShipResultToDlqWhenTenantIdMissing() {
        MqConsumeLogMapper mqConsumeLogMapper = mock(MqConsumeLogMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        InventoryShipResultConsumer consumer = new InventoryShipResultConsumer(
                mqConsumeLogMapper,
                salesOrderMapper,
                "scm-sales-ship-result-consumer",
                "inventory.ship.result.v1",
                "sales.inventory.result.dlq.v1",
                kafkaTemplate,
                meterRegistry
        );

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "inventory.ship.result.v1", 0, 1L, "SO-BAD-1",
                "{\"eventId\":\"e1\",\"orderNo\":\"SO-BAD-1\",\"status\":\"SUCCESS\"}"
        );

        assertDoesNotThrow(() -> consumer.onShipResult(record));
        verify(kafkaTemplate, times(1)).send(eq("sales.inventory.result.dlq.v1"), eq("SO-BAD-1"), anyString());
        assertEquals(1.0, meterRegistry.counter("scm.sales.mq.consume.ship-result.dead-letter").count());
    }

    @Test
    void shouldPublishCancelResultToDlqWhenTenantIdMissing() {
        MqConsumeLogMapper mqConsumeLogMapper = mock(MqConsumeLogMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        InventoryCancelResultConsumer consumer = new InventoryCancelResultConsumer(
                mqConsumeLogMapper,
                salesOrderMapper,
                "scm-sales-cancel-result-consumer",
                "inventory.cancel.result.v1",
                "sales.inventory.result.dlq.v1",
                kafkaTemplate,
                meterRegistry
        );

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "inventory.cancel.result.v1", 0, 1L, "SO-BAD-2",
                "{\"eventId\":\"e2\",\"orderNo\":\"SO-BAD-2\",\"status\":\"FAILED\"}"
        );

        assertDoesNotThrow(() -> consumer.onCancelResult(record));
        verify(kafkaTemplate, times(1)).send(eq("sales.inventory.result.dlq.v1"), eq("SO-BAD-2"), anyString());
        assertEquals(1.0, meterRegistry.counter("scm.sales.mq.consume.cancel-result.dead-letter").count());
    }
}
