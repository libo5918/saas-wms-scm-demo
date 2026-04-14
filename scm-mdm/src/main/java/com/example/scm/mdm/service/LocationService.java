package com.example.scm.mdm.service;

import com.example.scm.mdm.dto.CreateLocationRequest;
import com.example.scm.mdm.dto.UpdateLocationRequest;
import com.example.scm.mdm.vo.LocationVO;

import java.util.List;

/**
 * 库位应用服务接口，定义库位主数据的对外用例。
 */
public interface LocationService {

    /**
     * 创建库位。
     */
    LocationVO create(CreateLocationRequest request);

    /**
     * 更新库位。
     */
    LocationVO update(Long id, UpdateLocationRequest request);

    /**
     * 查询库位详情。
     */
    LocationVO getById(Long id);

    /**
     * 查询库位列表。
     */
    List<LocationVO> list(Long warehouseId);

    /**
     * 删除库位。
     */
    void delete(Long id);
}
