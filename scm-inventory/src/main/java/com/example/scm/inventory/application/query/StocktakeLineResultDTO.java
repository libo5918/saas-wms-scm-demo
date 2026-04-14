package com.example.scm.inventory.application.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "库存盘点结果明细 DTO")
public class StocktakeLineResultDTO {

    @Schema(description = "流水号")
    private String txnNo;

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "库位ID")
    private Long locationId;

    @Schema(description = "系统数量")
    private BigDecimal systemQty;

    @Schema(description = "盘点数量")
    private BigDecimal countedQty;

    @Schema(description = "差异数量")
    private BigDecimal varianceQty;

    @Schema(description = "调整类型")
    private String adjustType;
}
