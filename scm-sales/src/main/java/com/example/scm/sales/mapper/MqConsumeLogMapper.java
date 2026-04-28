package com.example.scm.sales.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MqConsumeLogMapper {

    @Select("""
            SELECT COUNT(1)
            FROM mq_consume_log
            WHERE consumer_group = #{consumerGroup}
              AND topic = #{topic}
              AND event_id = #{eventId}
            """)
    int countByUniqueKey(@Param("consumerGroup") String consumerGroup,
                         @Param("topic") String topic,
                         @Param("eventId") String eventId);

    @Insert("""
            INSERT IGNORE INTO mq_consume_log(tenant_id, consumer_group, topic, event_id, biz_key)
            VALUES (#{tenantId}, #{consumerGroup}, #{topic}, #{eventId}, #{bizKey})
            """)
    int insertIgnore(@Param("tenantId") Long tenantId,
                     @Param("consumerGroup") String consumerGroup,
                     @Param("topic") String topic,
                     @Param("eventId") String eventId,
                     @Param("bizKey") String bizKey);
}
