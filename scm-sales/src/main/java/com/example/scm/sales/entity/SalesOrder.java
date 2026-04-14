package com.example.scm.sales.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "销售订单实体。")
public class SalesOrder {

    @Schema(description = "销售单主键ID。")
    private Long id;
    @Schema(description = "租户ID。")
    private Long tenantId;
    @Schema(description = "销售单号。")
    private String orderNo;
    @Schema(description = "仓库ID。")
    private Long warehouseId;
    @Schema(description = "销售单状态。")
    private String orderStatus;
    @Schema(description = "库存联动失败原因。")
    private String failureReason;
    @Schema(description = "创建人。")
    private Long createdBy;
    @Schema(description = "创建时间。")
    private LocalDateTime createdAt;
    @Schema(description = "更新人。")
    private Long updatedBy;
    @Schema(description = "更新时间。")
    private LocalDateTime updatedAt;
    @Schema(description = "逻辑删除标记。")
    private Integer deleted;
}
