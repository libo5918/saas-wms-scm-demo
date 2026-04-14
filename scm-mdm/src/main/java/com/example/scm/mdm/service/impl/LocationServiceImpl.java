package com.example.scm.mdm.service.impl;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.mdm.dto.CreateLocationRequest;
import com.example.scm.mdm.dto.UpdateLocationRequest;
import com.example.scm.mdm.entity.Location;
import com.example.scm.mdm.mapper.LocationMapper;
import com.example.scm.mdm.mapper.WarehouseMapper;
import com.example.scm.mdm.service.LocationService;
import com.example.scm.mdm.support.LocationAssembler;
import com.example.scm.mdm.support.LocationValidator;
import com.example.scm.mdm.vo.LocationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 库位应用服务实现，负责库位主数据的校验、持久化和结果组装。
 */
@Service
@Slf4j
public class LocationServiceImpl implements LocationService {

    private static final long SYSTEM_OPERATOR_ID = 1L;

    private final LocationMapper locationMapper;
    private final WarehouseMapper warehouseMapper;
    private final LocationValidator locationValidator;
    private final LocationAssembler locationAssembler;

    public LocationServiceImpl(LocationMapper locationMapper,
                               WarehouseMapper warehouseMapper,
                               LocationValidator locationValidator,
                               LocationAssembler locationAssembler) {
        this.locationMapper = locationMapper;
        this.warehouseMapper = warehouseMapper;
        this.locationValidator = locationValidator;
        this.locationAssembler = locationAssembler;
    }

    /**
     * 创建新库位，并校验所属仓库存在且编码唯一。
     */
    @Override
    @Transactional
    public LocationVO create(CreateLocationRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Start create location, tenantId={}, warehouseId={}, locationCode={}",
                tenantId, request.getWarehouseId(), request.getLocationCode());
        locationValidator.validateForCreate(request);
        ensureWarehouseExists(tenantId, request.getWarehouseId());
        locationMapper.selectByCode(tenantId, request.getWarehouseId(), request.getLocationCode()).ifPresent(location -> {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Location code already exists");
        });

        Location location = locationAssembler.toNewEntity(tenantId, SYSTEM_OPERATOR_ID, request);
        locationMapper.insert(location);
        log.info("Create location success, tenantId={}, locationId={}, locationCode={}",
                tenantId, location.getId(), location.getLocationCode());
        return locationAssembler.toVO(location);
    }

    /**
     * 更新已有库位的基础属性。
     */
    @Override
    @Transactional
    public LocationVO update(Long id, UpdateLocationRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Start update location, tenantId={}, locationId={}", tenantId, id);
        locationValidator.validateForUpdate(request);
        Location location = locationMapper.selectById(tenantId, id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Location not found"));
        location.setLocationName(request.getLocationName());
        location.setLocationType(request.getLocationType());
        location.setStatus(request.getStatus());
        location.setUpdatedBy(SYSTEM_OPERATOR_ID);
        int affected = locationMapper.update(location);
        if (affected == 0) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Update location failed");
        }
        log.info("Update location success, tenantId={}, locationId={}", tenantId, id);
        return locationAssembler.toVO(location);
    }

    /**
     * 查询单个库位详情。
     */
    @Override
    public LocationVO getById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Query location detail, tenantId={}, locationId={}", tenantId, id);
        return locationMapper.selectById(tenantId, id)
                .map(locationAssembler::toVO)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Location not found"));
    }

    /**
     * 查询当前租户下的库位列表，可按仓库过滤。
     */
    @Override
    public List<LocationVO> list(Long warehouseId) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Query location list, tenantId={}, warehouseId={}", tenantId, warehouseId);
        List<Location> locations = warehouseId == null
                ? locationMapper.selectByTenantId(tenantId)
                : locationMapper.selectByWarehouseId(tenantId, warehouseId);
        return locations.stream()
                .map(locationAssembler::toVO)
                .toList();
    }

    /**
     * 逻辑删除库位。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Delete location, tenantId={}, locationId={}", tenantId, id);
        int affected = locationMapper.softDelete(tenantId, id, SYSTEM_OPERATOR_ID);
        if (affected == 0) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Location not found");
        }
        log.info("Delete location success, tenantId={}, locationId={}", tenantId, id);
    }

    private void ensureWarehouseExists(Long tenantId, Long warehouseId) {
        warehouseMapper.selectById(tenantId, warehouseId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Warehouse not found"));
    }
}
