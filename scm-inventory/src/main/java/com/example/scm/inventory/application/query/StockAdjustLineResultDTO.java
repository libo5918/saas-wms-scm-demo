package com.example.scm.inventory.application.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "库存调整结果明细 DTO。")
public class StockAdjustLineResultDTO {

    @Schema(description = "流水号。")
    private String txnNo;

    @Schema(description = "物料ID。")
    private Long materialId;

    @Schema(description = "仓库ID。")
    private Long warehouseId;

    @Schema(description = "库位ID。")
    private Long locationId;

    @Schema(description = "调整数量。")
    private BigDecimal quantity;

    @Schema(description = "调整前数量。")
    private BigDecimal beforeQty;

    @Schema(description = "调整后数量。")
    private BigDecimal afterQty;
}
