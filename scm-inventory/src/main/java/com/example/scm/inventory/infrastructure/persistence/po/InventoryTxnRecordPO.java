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

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String txnNo;
    private String bizType;
    private String bizNo;
    private Long materialId;
    private Long warehouseId;
    private Long locationId;
    private String txnDirection;
    private BigDecimal txnQty;
    private BigDecimal beforeQty;
    private BigDecimal afterQty;
    private LocalDateTime createdAt;
}
