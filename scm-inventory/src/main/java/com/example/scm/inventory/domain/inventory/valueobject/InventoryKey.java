package com.example.scm.inventory.domain.inventory.valueobject;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@Schema(description = "库存维度值对象，唯一标识租户下某个物料在仓库和库位上的库存。")
public class InventoryKey {

    private final Long tenantId;
    private final Long materialId;
    private final Long warehouseId;
    private final Long locationId;
}
