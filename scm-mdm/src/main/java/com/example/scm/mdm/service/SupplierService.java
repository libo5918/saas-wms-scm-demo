package com.example.scm.mdm.service;

import com.example.scm.mdm.dto.CreateSupplierRequest;
import com.example.scm.mdm.dto.UpdateSupplierRequest;
import com.example.scm.mdm.vo.SupplierVO;

import java.util.List;

/**
 * 供应商应用服务接口，定义供应商主数据的对外用例。
 */
public interface SupplierService {

    SupplierVO create(CreateSupplierRequest request);

    SupplierVO update(Long id, UpdateSupplierRequest request);

    SupplierVO getById(Long id);

    List<SupplierVO> list();

    void delete(Long id);
}
