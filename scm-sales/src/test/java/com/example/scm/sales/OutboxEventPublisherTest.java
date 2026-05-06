package com.example.scm.sales;

import com.example.scm.sales.entity.OutboxEvent;
import com.example.scm.sales.mapper.OutboxEventMapper;
import com.example.scm.sales.support.OutboxEventPublisher;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxEventPublisherTest {

    @Test
    void shouldDiscardOutboxEventWhenRetryCountReachedMax() {
        OutboxEventMapper mapper = mock(OutboxEventMapper.class);
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka unavailable")));

        OutboxEvent event = new OutboxEvent();
        event.setId(11L);
        event.setEventId("evt-11");
        event.setEventType("ORDER_SHIP_REQUESTED");
        event.setEventKey("SO-11");
        event.setPayloadJson("{\"eventId\":\"evt-11\"}");
        event.setRetryCount(3);
        when(mapper.selectPending(100)).thenReturn(List.of(event));

        OutboxEventPublisher publisher = new OutboxEventPublisher(
                mapper,
                kafkaTemplate,
                true,
                100,
                3,
                1,
                "order.ship.requested.v1",
                "order.cancel.requested.v1",
                new SimpleMeterRegistry()
        );

        publisher.publishPendingEvents();

        verify(mapper, times(1)).markDiscarded(eq(11L));
        verify(mapper, never()).markFailed(eq(11L), org.mockito.ArgumentMatchers.any());
    }
}
