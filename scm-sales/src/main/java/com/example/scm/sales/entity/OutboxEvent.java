package com.example.scm.sales.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OutboxEvent {

    private Long id;
    private Long tenantId;
    private String eventId;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private String eventKey;
    private String topic;
    private String payloadJson;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
