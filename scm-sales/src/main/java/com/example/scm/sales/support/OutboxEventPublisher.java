package com.example.scm.sales.support;

import com.example.scm.sales.entity.OutboxEvent;
import com.example.scm.sales.mapper.OutboxEventMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 发布器骨架。
 *
 * <p>当前阶段仅做事件扫描、日志发布与状态推进，下一阶段可替换为 Kafka 真发送。</p>
 */
@Component
@Slf4j
public class OutboxEventPublisher {

    private final OutboxEventMapper outboxEventMapper;

    public OutboxEventPublisher(OutboxEventMapper outboxEventMapper) {
        this.outboxEventMapper = outboxEventMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventMapper.selectPending(100);
        for (OutboxEvent event : pendingEvents) {
            try {
                // TODO: 切换到 KafkaTemplate.send(topic, key, payload)
                log.info("Publish outbox event stub success, eventId={}, eventType={}, eventKey={}",
                        event.getEventId(), event.getEventType(), event.getEventKey());
                outboxEventMapper.markSent(event.getId());
            } catch (Exception ex) {
                log.error("Publish outbox event stub failed, eventId={}, eventType={}",
                        event.getEventId(), event.getEventType(), ex);
                outboxEventMapper.markFailed(event.getId(), LocalDateTime.now().plusMinutes(1));
            }
        }
    }
}
