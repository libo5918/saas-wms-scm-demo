package com.example.scm.inventory.application.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "库存移库结果明细 DTO。")
public class StockTransferLineResultDTO {

    @Schema(description = "移出流水号。")
    private String moveOutTxnNo;

    @Schema(description = "移入流水号。")
    private String moveInTxnNo;

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

    @Schema(description = "源库位移出前数量。")
    private BigDecimal fromBeforeQty;

    @Schema(description = "源库位移出后数量。")
    private BigDecimal fromAfterQty;

    @Schema(description = "目标库位移入前数量。")
    private BigDecimal toBeforeQty;

    @Schema(description = "目标库位移入后数量。")
    private BigDecimal toAfterQty;
}
