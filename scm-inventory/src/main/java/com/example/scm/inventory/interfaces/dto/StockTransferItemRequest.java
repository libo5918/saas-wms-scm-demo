package com.example.scm.inventory.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "库存移库明细请求。")
public class StockTransferItemRequest {

    @NotNull
    @Schema(description = "物料ID。", example = "1")
    private Long materialId;

    @NotNull
    @Schema(description = "源仓库ID。", example = "2001")
    private Long fromWarehouseId;

    @NotNull
    @Schema(description = "源库位ID。", example = "3001")
    private Long fromLocationId;

    @NotNull
    @Schema(description = "目标仓库ID。", example = "2002")
    private Long toWarehouseId;

    @NotNull
    @Schema(description = "目标库位ID。", example = "3002")
    private Long toLocationId;

    @NotNull
    @DecimalMin(value = "0.0001")
    @Schema(description = "移库数量。", example = "3")
    private BigDecimal quantity;
}
