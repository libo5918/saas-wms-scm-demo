package com.example.scm.inventory.domain.inventory.valueobject;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@Schema(description = "库存维度值对象，唯一标识租户下某个物料在仓库库位上的库存。")
public class InventoryKey {

    @Schema(description = "租户ID。")
    private final Long tenantId;

    @Schema(description = "物料ID。")
    private final Long materialId;

    @Schema(description = "仓库ID。")
    private final Long warehouseId;

    @Schema(description = "库位ID。")
    private final Long locationId;
}
