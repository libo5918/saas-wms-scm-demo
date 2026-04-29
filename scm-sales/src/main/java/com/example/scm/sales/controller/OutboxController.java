package com.example.scm.sales.controller;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.Result;
import com.example.scm.sales.entity.OutboxEvent;
import com.example.scm.sales.mapper.OutboxEventMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/outbox")
@Tag(name = "Outbox 管理", description = "Outbox 运行状态查询接口")
public class OutboxController {

    private final OutboxEventMapper outboxEventMapper;

    public OutboxController(OutboxEventMapper outboxEventMapper) {
        this.outboxEventMapper = outboxEventMapper;
    }

    @GetMapping("/stats")
    @Operation(summary = "查询 Outbox 状态统计", description = "返回 NEW/FAILED/SENT/DISCARDED 等状态的事件数量")
    public Result<Map<String, Long>> stats() {
        List<Map<String, Object>> rows = outboxEventMapper.countByStatus();
        Map<String, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object status = row.get("status");
            Object total = row.get("total");
            if (status != null && total instanceof Number number) {
                result.put(String.valueOf(status), number.longValue());
            }
        }
        return Result.success(result);
    }

    @GetMapping("/events")
    @Operation(summary = "查询 Outbox 事件", description = "按状态筛选并返回最新事件列表，默认返回 20 条")
    public Result<List<OutboxEvent>> events(@RequestParam(value = "status", required = false) String status,
                                            @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return Result.success(outboxEventMapper.selectLatest(status, safeLimit));
    }

    @PostMapping("/{id}/requeue")
    @Operation(summary = "重放 Outbox 事件", description = "将 FAILED 或 DISCARDED 状态事件重置为 NEW，供发布器重新发送")
    public Result<Integer> requeue(@PathVariable("id") Long id) {
        int affected = outboxEventMapper.requeue(id);
        if (affected <= 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(),
                    "Outbox event cannot be requeued unless status is FAILED or DISCARDED");
        }
        return Result.success(affected);
    }

    @PostMapping("/requeue")
    @Operation(summary = "批量重放 Outbox 事件", description = "按状态批量将事件重置为 NEW，默认状态 FAILED")
    public Result<Integer> requeueBatch(@RequestParam(value = "status", defaultValue = "FAILED") String status,
                                        @RequestParam(value = "limit", defaultValue = "50") Integer limit) {
        if (!"FAILED".equals(status) && !"DISCARDED".equals(status)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(),
                    "status must be FAILED or DISCARDED");
        }
        int safeLimit = Math.min(Math.max(limit, 1), 500);
        int affected = outboxEventMapper.requeueByStatus(status, safeLimit);
        return Result.success(affected);
    }
}
