package com.example.scm.mdm.support;

import com.example.scm.mdm.dto.CreateWarehouseRequest;
import com.example.scm.mdm.entity.Warehouse;
import com.example.scm.mdm.vo.WarehouseVO;
import org.springframework.stereotype.Component;

@Component
public class WarehouseAssembler {

    public Warehouse toNewEntity(Long tenantId, Long operatorId, CreateWarehouseRequest request) {
        Warehouse warehouse = new Warehouse();
        warehouse.setTenantId(tenantId);
        warehouse.setWarehouseCode(request.getWarehouseCode());
        warehouse.setWarehouseName(request.getWarehouseName());
        warehouse.setWarehouseType(request.getWarehouseType());
        warehouse.setContactName(request.getContactName());
        warehouse.setContactPhone(request.getContactPhone());
        warehouse.setAddress(request.getAddress());
        warehouse.setStatus(request.getStatus());
        warehouse.setCreatedBy(operatorId);
        warehouse.setUpdatedBy(operatorId);
        warehouse.setDeleted(0);
        return warehouse;
    }

    public WarehouseVO toVO(Warehouse warehouse) {
        WarehouseVO vo = new WarehouseVO();
        vo.setId(warehouse.getId());
        vo.setWarehouseCode(warehouse.getWarehouseCode());
        vo.setWarehouseName(warehouse.getWarehouseName());
        vo.setWarehouseType(warehouse.getWarehouseType());
        vo.setContactName(warehouse.getContactName());
        vo.setContactPhone(warehouse.getContactPhone());
        vo.setAddress(warehouse.getAddress());
        vo.setStatus(warehouse.getStatus());
        return vo;
    }
}
