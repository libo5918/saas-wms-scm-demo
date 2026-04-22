package com.example.scm.sales.support;

import com.example.scm.sales.entity.OutboxEvent;
import com.example.scm.sales.mapper.OutboxEventMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
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

    public OutboxEventPublisher(OutboxEventMapper outboxEventMapper,
                                KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventMapper = outboxEventMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventMapper.selectPending(100);
        for (OutboxEvent event : pendingEvents) {
            try {
                String topic = resolveTopic(event);
                kafkaTemplate.send(topic, event.getEventKey(), event.getPayloadJson()).get();
                log.info("Publish outbox event success, eventId={}, topic={}, eventType={}, eventKey={}",
                        event.getEventId(), topic, event.getEventType(), event.getEventKey());
                outboxEventMapper.markSent(event.getId());
            } catch (Exception ex) {
                log.error("Publish outbox event failed, eventId={}, eventType={}, topic={}",
                        event.getEventId(), event.getEventType(), event.getTopic(), ex);
                outboxEventMapper.markFailed(event.getId(), LocalDateTime.now().plusMinutes(1));
            }
        }
    }

    private String resolveTopic(OutboxEvent event) {
        if (StringUtils.hasText(event.getTopic())) {
            return event.getTopic();
        }
        return switch (event.getEventType()) {
            case "ORDER_SHIP_REQUESTED" -> "order.ship.requested.v1";
            case "ORDER_CANCEL_REQUESTED" -> "order.cancel.requested.v1";
            default -> throw new IllegalArgumentException("Unknown outbox event type: " + event.getEventType());
        };
    }
}
