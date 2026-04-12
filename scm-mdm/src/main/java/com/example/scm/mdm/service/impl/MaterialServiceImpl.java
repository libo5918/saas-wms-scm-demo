package com.example.scm.mdm.service.impl;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.mdm.dto.CreateMaterialRequest;
import com.example.scm.mdm.dto.UpdateMaterialRequest;
import com.example.scm.mdm.entity.Material;
import com.example.scm.mdm.mapper.MaterialMapper;
import com.example.scm.mdm.service.MaterialService;
import com.example.scm.mdm.support.MaterialAssembler;
import com.example.scm.mdm.support.MaterialValidator;
import com.example.scm.mdm.vo.MaterialVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MaterialServiceImpl implements MaterialService {

    private static final long SYSTEM_OPERATOR_ID = 1L;

    private final MaterialMapper materialMapper;
    private final MaterialValidator materialValidator;
    private final MaterialAssembler materialAssembler;

    public MaterialServiceImpl(MaterialMapper materialMapper,
                               MaterialValidator materialValidator,
                               MaterialAssembler materialAssembler) {
        this.materialMapper = materialMapper;
        this.materialValidator = materialValidator;
        this.materialAssembler = materialAssembler;
    }

    @Override
    @Transactional
    public MaterialVO create(CreateMaterialRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        materialValidator.validateForCreate(request);
        materialMapper.selectByCode(tenantId, request.getMaterialCode()).ifPresent(material -> {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Material code already exists");
        });

        Material material = materialAssembler.toNewEntity(tenantId, SYSTEM_OPERATOR_ID, request);
        materialMapper.insert(material);
        return materialAssembler.toVO(material);
    }

    @Override
    @Transactional
    public MaterialVO update(Long id, UpdateMaterialRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        materialValidator.validateForUpdate(request);
        Material material = materialMapper.selectById(tenantId, id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Material not found"));
        material.setMaterialName(request.getMaterialName());
        material.setMaterialSpec(request.getMaterialSpec());
        material.setUnit(request.getUnit());
        material.setMaterialType(request.getMaterialType());
        material.setStatus(request.getStatus());
        material.setUpdatedBy(SYSTEM_OPERATOR_ID);
        int affected = materialMapper.update(material);
        if (affected == 0) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Update material failed");
        }
        return materialAssembler.toVO(material);
    }

    @Override
    public MaterialVO getById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        return materialMapper.selectById(tenantId, id)
                .map(materialAssembler::toVO)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Material not found"));
    }

    @Override
    public List<MaterialVO> list() {
        Long tenantId = TenantContext.getRequiredTenantId();
        return materialMapper.selectByTenantId(tenantId).stream()
                .map(materialAssembler::toVO)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        int affected = materialMapper.softDelete(tenantId, id, SYSTEM_OPERATOR_ID);
        if (affected == 0) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Material not found");
        }
    }
}
