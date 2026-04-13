package com.example.scm.mdm.service;

import com.example.scm.mdm.dto.CreateMaterialRequest;
import com.example.scm.mdm.dto.UpdateMaterialRequest;
import com.example.scm.mdm.vo.MaterialVO;

import java.util.List;

/**
 * 物料应用服务接口，定义物料主数据的对外用例。
 */
public interface MaterialService {

    /**
     * 创建物料。
     */
    MaterialVO create(CreateMaterialRequest request);

    /**
     * 更新物料。
     */
    MaterialVO update(Long id, UpdateMaterialRequest request);

    /**
     * 查询物料详情。
     */
    MaterialVO getById(Long id);

    /**
     * 查询物料列表。
     */
    List<MaterialVO> list();

    /**
     * 删除物料。
     */
    void delete(Long id);
}
