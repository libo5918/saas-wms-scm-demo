package com.example.scm.mdm.support;

import com.example.scm.mdm.dto.CreateMaterialRequest;
import com.example.scm.mdm.entity.Material;
import com.example.scm.mdm.vo.MaterialVO;
import org.springframework.stereotype.Component;

@Component
public class MaterialAssembler {

    public Material toNewEntity(Long tenantId, Long operatorId, CreateMaterialRequest request) {
        Material material = new Material();
        material.setTenantId(tenantId);
        material.setMaterialCode(request.getMaterialCode());
        material.setMaterialName(request.getMaterialName());
        material.setMaterialSpec(request.getMaterialSpec());
        material.setUnit(request.getUnit());
        material.setMaterialType(request.getMaterialType());
        material.setStatus(request.getStatus());
        material.setCreatedBy(operatorId);
        material.setUpdatedBy(operatorId);
        material.setDeleted(0);
        return material;
    }

    public MaterialVO toVO(Material material) {
        MaterialVO vo = new MaterialVO();
        vo.setId(material.getId());
        vo.setMaterialCode(material.getMaterialCode());
        vo.setMaterialName(material.getMaterialName());
        vo.setMaterialSpec(material.getMaterialSpec());
        vo.setUnit(material.getUnit());
        vo.setMaterialType(material.getMaterialType());
        vo.setStatus(material.getStatus());
        return vo;
    }
}
