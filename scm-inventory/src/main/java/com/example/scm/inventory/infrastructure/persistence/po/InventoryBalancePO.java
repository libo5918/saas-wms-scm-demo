package com.example.scm.inventory.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("inventory_balance")
@Schema(description = "库存余额持久化对象，对应 inventory_balance 表。")
public class InventoryBalancePO {

    @Schema(description = "主键ID。")
    @TableId(type = IdType.AUTO)
    private Long id;
    @Schema(description = "租户ID。")
    private Long tenantId;
    @Schema(description = "物料ID。")
    private Long materialId;
    @Schema(description = "仓库ID。")
    private Long warehouseId;
    @Schema(description = "库位ID。")
    private Long locationId;
    @Schema(description = "现存数量。")
    private BigDecimal onHandQty;
    @Schema(description = "锁定数量。")
    private BigDecimal lockedQty;
    @Schema(description = "可用数量。")
    private BigDecimal availableQty;
    @Schema(description = "乐观锁版本号。")
    private Long version;
    @Schema(description = "创建人。")
    private Long createdBy;
    @Schema(description = "创建时间。")
    private LocalDateTime createdAt;
    @Schema(description = "更新人。")
    private Long updatedBy;
    @Schema(description = "更新时间。")
    private LocalDateTime updatedAt;
    @Schema(description = "逻辑删除标记。")
    private Integer deleted;
}
