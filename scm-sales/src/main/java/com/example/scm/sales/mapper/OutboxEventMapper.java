package com.example.scm.sales.mapper;

import com.example.scm.sales.entity.OutboxEvent;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OutboxEventMapper {

    @Insert("""
            INSERT INTO outbox_event(
                tenant_id, event_id, aggregate_type, aggregate_id, event_type, event_key,
                payload_json, status, retry_count, next_retry_time
            ) VALUES (
                #{tenantId}, #{eventId}, #{aggregateType}, #{aggregateId}, #{eventType}, #{eventKey},
                #{payloadJson}, #{status}, #{retryCount}, #{nextRetryTime}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(OutboxEvent event);

    @Select("""
            SELECT id, tenant_id, event_id, aggregate_type, aggregate_id, event_type, event_key,
                   payload_json, status, retry_count, next_retry_time, created_at, updated_at
            FROM outbox_event
            WHERE status IN ('NEW', 'FAILED')
              AND (next_retry_time IS NULL OR next_retry_time <= NOW())
            ORDER BY id ASC
            LIMIT #{limit}
            """)
    List<OutboxEvent> selectPending(@Param("limit") int limit);

    @Update("""
            UPDATE outbox_event
            SET status = 'SENT',
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int markSent(@Param("id") Long id);

    @Update("""
            UPDATE outbox_event
            SET status = 'FAILED',
                retry_count = retry_count + 1,
                next_retry_time = #{nextRetryTime},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int markFailed(@Param("id") Long id, @Param("nextRetryTime") LocalDateTime nextRetryTime);
}
