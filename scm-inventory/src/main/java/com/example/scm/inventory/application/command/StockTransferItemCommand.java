package com.example.scm.inventory.application.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "库存移库明细命令。")
public class StockTransferItemCommand {

    @Schema(description = "物料ID。")
    private Long materialId;

    @Schema(description = "源仓库ID。")
    private Long fromWarehouseId;

    @Schema(description = "源库位ID。")
    private Long fromLocationId;

    @Schema(description = "目标仓库ID。")
    private Long toWarehouseId;

    @Schema(description = "目标库位ID。")
    private Long toLocationId;

    @Schema(description = "移库数量。")
    private BigDecimal quantity;
}
