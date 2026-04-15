package com.example.scm.mdm.support;

import com.example.scm.mdm.dto.CreateSupplierRequest;
import com.example.scm.mdm.entity.Supplier;
import com.example.scm.mdm.vo.SupplierVO;
import org.springframework.stereotype.Component;

@Component
public class SupplierAssembler {

    public Supplier toNewEntity(Long tenantId, Long operatorId, CreateSupplierRequest request) {
        Supplier supplier = new Supplier();
        supplier.setTenantId(tenantId);
        supplier.setSupplierCode(request.getSupplierCode());
        supplier.setSupplierName(request.getSupplierName());
        supplier.setContactName(request.getContactName());
        supplier.setContactPhone(request.getContactPhone());
        supplier.setStatus(request.getStatus());
        supplier.setCreatedBy(operatorId);
        supplier.setUpdatedBy(operatorId);
        supplier.setDeleted(0);
        return supplier;
    }

    public SupplierVO toVO(Supplier supplier) {
        SupplierVO vo = new SupplierVO();
        vo.setId(supplier.getId());
        vo.setSupplierCode(supplier.getSupplierCode());
        vo.setSupplierName(supplier.getSupplierName());
        vo.setContactName(supplier.getContactName());
        vo.setContactPhone(supplier.getContactPhone());
        vo.setStatus(supplier.getStatus());
        return vo;
    }
}
