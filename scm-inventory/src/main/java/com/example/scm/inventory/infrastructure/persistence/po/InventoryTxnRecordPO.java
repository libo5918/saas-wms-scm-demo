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
@TableName("inventory_txn_record")
@Schema(description = "库存流水持久化对象，对应 inventory_txn_record 表。")
public class InventoryTxnRecordPO {

    @Schema(description = "主键ID。")
    @TableId(type = IdType.AUTO)
    private Long id;
    @Schema(description = "租户ID。")
    private Long tenantId;
    @Schema(description = "库存流水号。")
    private String txnNo;
    @Schema(description = "业务类型。")
    private String bizType;
    @Schema(description = "业务单号。")
    private String bizNo;
    @Schema(description = "物料ID。")
    private Long materialId;
    @Schema(description = "仓库ID。")
    private Long warehouseId;
    @Schema(description = "库位ID。")
    private Long locationId;
    @Schema(description = "出入库方向。")
    private String txnDirection;
    @Schema(description = "本次变动数量。")
    private BigDecimal txnQty;
    @Schema(description = "变动前数量。")
    private BigDecimal beforeQty;
    @Schema(description = "变动后数量。")
    private BigDecimal afterQty;
    @Schema(description = "创建时间。")
    private LocalDateTime createdAt;
}
