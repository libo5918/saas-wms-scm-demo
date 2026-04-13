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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 物料应用服务实现，负责物料主数据的校验、持久化和结果组装。
 */
@Service
@Slf4j
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

    /**
     * 创建新物料，并校验租户内编码唯一。
     */
    @Override
    @Transactional
    public MaterialVO create(CreateMaterialRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Start create material, tenantId={}, materialCode={}", tenantId, request.getMaterialCode());
        materialValidator.validateForCreate(request);
        materialMapper.selectByCode(tenantId, request.getMaterialCode()).ifPresent(material -> {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Material code already exists");
        });

        Material material = materialAssembler.toNewEntity(tenantId, SYSTEM_OPERATOR_ID, request);
        materialMapper.insert(material);
        log.info("Create material success, tenantId={}, materialId={}, materialCode={}",
                tenantId, material.getId(), material.getMaterialCode());
        return materialAssembler.toVO(material);
    }

    /**
     * 更新已有物料的基础属性。
     */
    @Override
    @Transactional
    public MaterialVO update(Long id, UpdateMaterialRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Start update material, tenantId={}, materialId={}", tenantId, id);
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
        log.info("Update material success, tenantId={}, materialId={}", tenantId, id);
        return materialAssembler.toVO(material);
    }

    /**
     * 查询单个物料详情。
     */
    @Override
    public MaterialVO getById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Query material detail, tenantId={}, materialId={}", tenantId, id);
        return materialMapper.selectById(tenantId, id)
                .map(materialAssembler::toVO)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Material not found"));
    }

    /**
     * 查询当前租户下的物料列表。
     */
    @Override
    public List<MaterialVO> list() {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Query material list, tenantId={}", tenantId);
        return materialMapper.selectByTenantId(tenantId).stream()
                .map(materialAssembler::toVO)
                .toList();
    }

    /**
     * 逻辑删除物料。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Delete material, tenantId={}, materialId={}", tenantId, id);
        int affected = materialMapper.softDelete(tenantId, id, SYSTEM_OPERATOR_ID);
        if (affected == 0) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Material not found");
        }
        log.info("Delete material success, tenantId={}, materialId={}", tenantId, id);
    }
}
