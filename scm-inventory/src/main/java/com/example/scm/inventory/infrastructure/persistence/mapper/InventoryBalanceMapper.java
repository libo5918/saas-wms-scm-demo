package com.example.scm.inventory.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.scm.inventory.infrastructure.persistence.po.InventoryBalancePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryBalanceMapper extends BaseMapper<InventoryBalancePO> {
}
