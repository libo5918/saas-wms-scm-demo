package com.example.scm.inventory.domain.inventory.valueobject;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "库存流水方向枚举。")
public enum InventoryTransactionDirection {

    @Schema(description = "入库")
    IN,

    @Schema(description = "出库")
    OUT
}
