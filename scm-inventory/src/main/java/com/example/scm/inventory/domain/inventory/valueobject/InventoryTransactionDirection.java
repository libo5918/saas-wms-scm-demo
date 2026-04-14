package com.example.scm.inventory.domain.inventory.valueobject;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "库存流水方向枚举。")
public enum InventoryTransactionDirection {

    @Schema(description = "入库")
    IN,

    @Schema(description = "出库")
    OUT,

    @Schema(description = "锁库")
    LOCK,

    @Schema(description = "解锁")
    UNLOCK,

    @Schema(description = "调整入库")
    ADJUST_IN,

    @Schema(description = "调整出库")
    ADJUST_OUT,

    @Schema(description = "移库移出")
    MOVE_OUT,

    @Schema(description = "移库移入")
    MOVE_IN
}
