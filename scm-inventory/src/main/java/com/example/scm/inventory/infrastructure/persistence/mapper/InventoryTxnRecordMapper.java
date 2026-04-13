package com.example.scm.inventory.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.scm.inventory.infrastructure.persistence.po.InventoryTxnRecordPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存流水 MyBatis-Plus Mapper，负责 `inventory_txn_record` 表基础 CRUD。
 */
@Mapper
public interface InventoryTxnRecordMapper extends BaseMapper<InventoryTxnRecordPO> {
}
