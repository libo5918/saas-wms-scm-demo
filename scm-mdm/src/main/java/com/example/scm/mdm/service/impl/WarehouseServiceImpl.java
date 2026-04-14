package com.example.scm.mdm.service.impl;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.mdm.dto.CreateWarehouseRequest;
import com.example.scm.mdm.dto.UpdateWarehouseRequest;
import com.example.scm.mdm.entity.Warehouse;
import com.example.scm.mdm.mapper.WarehouseMapper;
import com.example.scm.mdm.service.WarehouseService;
import com.example.scm.mdm.support.WarehouseAssembler;
import com.example.scm.mdm.support.WarehouseValidator;
import com.example.scm.mdm.vo.WarehouseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 仓库应用服务实现，负责仓库主数据的校验、持久化和结果组装。
 */
@Service
@Slf4j
public class WarehouseServiceImpl implements WarehouseService {

    private static final long SYSTEM_OPERATOR_ID = 1L;

    private final WarehouseMapper warehouseMapper;
    private final WarehouseValidator warehouseValidator;
    private final WarehouseAssembler warehouseAssembler;

    public WarehouseServiceImpl(WarehouseMapper warehouseMapper,
                                WarehouseValidator warehouseValidator,
                                WarehouseAssembler warehouseAssembler) {
        this.warehouseMapper = warehouseMapper;
        this.warehouseValidator = warehouseValidator;
        this.warehouseAssembler = warehouseAssembler;
    }

    /**
     * 创建新仓库，并校验租户内编码唯一。
     */
    @Override
    @Transactional
    public WarehouseVO create(CreateWarehouseRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Start create warehouse, tenantId={}, warehouseCode={}", tenantId, request.getWarehouseCode());
        warehouseValidator.validateForCreate(request);
        warehouseMapper.selectByCode(tenantId, request.getWarehouseCode()).ifPresent(warehouse -> {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Warehouse code already exists");
        });

        Warehouse warehouse = warehouseAssembler.toNewEntity(tenantId, SYSTEM_OPERATOR_ID, request);
        warehouseMapper.insert(warehouse);
        log.info("Create warehouse success, tenantId={}, warehouseId={}, warehouseCode={}",
                tenantId, warehouse.getId(), warehouse.getWarehouseCode());
        return warehouseAssembler.toVO(warehouse);
    }

    /**
     * 更新已有仓库的基础属性。
     */
    @Override
    @Transactional
    public WarehouseVO update(Long id, UpdateWarehouseRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Start update warehouse, tenantId={}, warehouseId={}", tenantId, id);
        warehouseValidator.validateForUpdate(request);
        Warehouse warehouse = warehouseMapper.selectById(tenantId, id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Warehouse not found"));
        warehouse.setWarehouseName(request.getWarehouseName());
        warehouse.setWarehouseType(request.getWarehouseType());
        warehouse.setContactName(request.getContactName());
        warehouse.setContactPhone(request.getContactPhone());
        warehouse.setAddress(request.getAddress());
        warehouse.setStatus(request.getStatus());
        warehouse.setUpdatedBy(SYSTEM_OPERATOR_ID);
        int affected = warehouseMapper.update(warehouse);
        if (affected == 0) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Update warehouse failed");
        }
        log.info("Update warehouse success, tenantId={}, warehouseId={}", tenantId, id);
        return warehouseAssembler.toVO(warehouse);
    }

    /**
     * 查询单个仓库详情。
     */
    @Override
    public WarehouseVO getById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Query warehouse detail, tenantId={}, warehouseId={}", tenantId, id);
        return warehouseMapper.selectById(tenantId, id)
                .map(warehouseAssembler::toVO)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Warehouse not found"));
    }

    /**
     * 查询当前租户下的仓库列表。
     */
    @Override
    public List<WarehouseVO> list() {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Query warehouse list, tenantId={}", tenantId);
        return warehouseMapper.selectByTenantId(tenantId).stream()
                .map(warehouseAssembler::toVO)
                .toList();
    }

    /**
     * 逻辑删除仓库。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Delete warehouse, tenantId={}, warehouseId={}", tenantId, id);
        int affected = warehouseMapper.softDelete(tenantId, id, SYSTEM_OPERATOR_ID);
        if (affected == 0) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Warehouse not found");
        }
        log.info("Delete warehouse success, tenantId={}, warehouseId={}", tenantId, id);
    }
}
