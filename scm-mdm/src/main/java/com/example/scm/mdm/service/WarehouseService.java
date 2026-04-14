package com.example.scm.mdm.service;

import com.example.scm.mdm.dto.CreateWarehouseRequest;
import com.example.scm.mdm.dto.UpdateWarehouseRequest;
import com.example.scm.mdm.vo.WarehouseVO;

import java.util.List;

/**
 * 仓库应用服务接口，定义仓库主数据的对外用例。
 */
public interface WarehouseService {

    /**
     * 创建仓库。
     */
    WarehouseVO create(CreateWarehouseRequest request);

    /**
     * 更新仓库。
     */
    WarehouseVO update(Long id, UpdateWarehouseRequest request);

    /**
     * 查询仓库详情。
     */
    WarehouseVO getById(Long id);

    /**
     * 查询仓库列表。
     */
    List<WarehouseVO> list();

    /**
     * 删除仓库。
     */
    void delete(Long id);
}
