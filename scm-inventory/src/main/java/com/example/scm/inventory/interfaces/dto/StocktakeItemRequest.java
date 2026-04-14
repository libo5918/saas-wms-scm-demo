package com.example.scm.inventory.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "库存盘点明细请求")
public class StocktakeItemRequest {

    @NotNull
    @Schema(description = "物料ID", example = "1")
    private Long materialId;

    @NotNull
    @Schema(description = "仓库ID", example = "2001")
    private Long warehouseId;

    @NotNull
    @Schema(description = "库位ID", example = "3001")
    private Long locationId;

    @NotNull
    @DecimalMin(value = "0")
    @Schema(description = "盘点数量", example = "8")
    private BigDecimal countedQty;
}
