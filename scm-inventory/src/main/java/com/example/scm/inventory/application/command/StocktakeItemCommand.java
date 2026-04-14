package com.example.scm.inventory.application.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "库存盘点明细命令")
public class StocktakeItemCommand {

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "库位ID")
    private Long locationId;

    @Schema(description = "盘点数量")
    private BigDecimal countedQty;
}
