package com.example.scm.inventory.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.scm.inventory.infrastructure.persistence.po.InventoryTxnRecordPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryTxnRecordMapper extends BaseMapper<InventoryTxnRecordPO> {
}
