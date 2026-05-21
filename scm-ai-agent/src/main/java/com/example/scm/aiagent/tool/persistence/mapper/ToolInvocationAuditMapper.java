package com.example.scm.aiagent.tool.persistence.mapper;

import com.example.scm.aiagent.tool.persistence.po.ToolInvocationAuditPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Tool 调用审计 MySQL Mapper。
 */
@Mapper
public interface ToolInvocationAuditMapper {

    void insert(ToolInvocationAuditPO record);

    List<ToolInvocationAuditPO> selectRecent(@Param("tenantId") Long tenantId,
                                             @Param("toolName") String toolName,
                                             @Param("runId") String runId,
                                             @Param("limit") int limit);
}
