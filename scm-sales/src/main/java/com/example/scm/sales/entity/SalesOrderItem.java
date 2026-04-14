package com.example.scm.sales.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "销售订单明细实体。")
public class SalesOrderItem {

    @Schema(description = "明细主键ID。")
    private Long id;
    @Schema(description = "租户ID。")
    private Long tenantId;
    @Schema(description = "销售单ID。")
    private Long salesOrderId;
    @Schema(description = "物料ID。")
    private Long materialId;
    @Schema(description = "库位ID。")
    private Long locationId;
    @Schema(description = "销售数量。")
    private BigDecimal saleQty;
    @Schema(description = "创建时间。")
    private LocalDateTime createdAt;
}
