package com.example.scm.inventory.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.scm.inventory.infrastructure.persistence.po.InventoryBalancePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存余额 MyBatis-Plus Mapper，负责 `inventory_balance` 表基础 CRUD。
 */
@Mapper
public interface InventoryBalanceMapper extends BaseMapper<InventoryBalancePO> {
}
