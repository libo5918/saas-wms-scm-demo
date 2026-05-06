package com.example.scm.sales.support;

import com.example.scm.sales.entity.OutboxEvent;
import com.example.scm.sales.mapper.OutboxEventMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 发布器：
 * 定时扫描待发送事件，发送到 Kafka，成功后置为 SENT，失败则退避重试。
 */
@Component
@Slf4j
public class OutboxEventPublisher {

    private final OutboxEventMapper outboxEventMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final boolean publisherEnabled;
    private final int fetchLimit;
    private final int maxRetries;
    private final int retryBackoffMinutes;
    private final String shipRequestedTopic;
    private final String cancelRequestedTopic;
    private final Counter publishSuccessCounter;
    private final Counter publishFailedCounter;

    public OutboxEventPublisher(OutboxEventMapper outboxEventMapper,
                                KafkaTemplate<String, String> kafkaTemplate,
                                @Value("${outbox.publisher.enabled:true}") boolean publisherEnabled,
                                @Value("${outbox.publisher.fetch-limit:100}") int fetchLimit,
                                @Value("${outbox.publisher.max-retries:20}") int maxRetries,
                                @Value("${outbox.publisher.retry-backoff-minutes:1}") int retryBackoffMinutes,
                                @Value("${mq.topics.order-ship-requested:order.ship.requested.v1}") String shipRequestedTopic,
                                @Value("${mq.topics.order-cancel-requested:order.cancel.requested.v1}") String cancelRequestedTopic,
                                MeterRegistry meterRegistry) {
        this.outboxEventMapper = outboxEventMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.publisherEnabled = publisherEnabled;
        this.fetchLimit = fetchLimit;
        this.maxRetries = maxRetries;
        this.retryBackoffMinutes = retryBackoffMinutes;
        this.shipRequestedTopic = shipRequestedTopic;
        this.cancelRequestedTopic = cancelRequestedTopic;
        this.publishSuccessCounter = meterRegistry.counter("scm.sales.outbox.publish.success");
        this.publishFailedCounter = meterRegistry.counter("scm.sales.outbox.publish.failed");
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:5000}")
    public void publishPendingEvents() {
        if (!publisherEnabled) {
            log.debug("Outbox publisher is disabled by config: outbox.publisher.enabled=false");
            return;
        }
        List<OutboxEvent> pendingEvents = outboxEventMapper.selectPending(fetchLimit);
        log.info("查询状态为new的outboxevent事件");
        for (OutboxEvent event : pendingEvents) {
            try {
                String topic = resolveTopic(event);
                kafkaTemplate.send(topic, event.getEventKey(), event.getPayloadJson()).get();
                log.info("Publish outbox event success, eventId={}, topic={}, eventType={}, eventKey={}",
                        event.getEventId(), topic, event.getEventType(), event.getEventKey());
                outboxEventMapper.markSent(event.getId());
                publishSuccessCounter.increment();
            } catch (Exception ex) {
                publishFailedCounter.increment();
                if (event.getRetryCount() >= maxRetries) {
                    outboxEventMapper.markDiscarded(event.getId());
                    log.error("Publish outbox event failed and discarded after max retries, eventId={}, eventType={}, retryCount={}, maxRetries={}",
                            event.getEventId(), event.getEventType(), event.getRetryCount(), maxRetries, ex);
                    continue;
                }
                log.error("Publish outbox event failed, eventId={}, eventType={}, topic={}",
                        event.getEventId(), event.getEventType(), event.getTopic(), ex);
                outboxEventMapper.markFailed(event.getId(), LocalDateTime.now().plusMinutes(retryBackoffMinutes));
            }
        }
    }

    private String resolveTopic(OutboxEvent event) {
        if (StringUtils.hasText(event.getTopic())) {
            return event.getTopic();
        }
        return switch (event.getEventType()) {
            case "ORDER_SHIP_REQUESTED" -> shipRequestedTopic;
            case "ORDER_CANCEL_REQUESTED" -> cancelRequestedTopic;
            default -> throw new IllegalArgumentException("Unknown outbox event type: " + event.getEventType());
        };
    }
}
