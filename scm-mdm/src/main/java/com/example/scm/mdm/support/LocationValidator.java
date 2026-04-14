package com.example.scm.mdm.support;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.mdm.constant.MasterDataStatus;
import com.example.scm.mdm.dto.CreateLocationRequest;
import com.example.scm.mdm.dto.UpdateLocationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LocationValidator {

    public void validateForCreate(CreateLocationRequest request) {
        validateWarehouseId(request.getWarehouseId());
        validateStatus(request.getStatus());
        validateCode(request.getLocationCode());
        validateName(request.getLocationName());
    }

    public void validateForUpdate(UpdateLocationRequest request) {
        validateStatus(request.getStatus());
        validateName(request.getLocationName());
    }

    private void validateWarehouseId(Long warehouseId) {
        if (warehouseId == null || warehouseId <= 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Warehouse id must be positive");
        }
    }

    private void validateCode(String locationCode) {
        if (!StringUtils.hasText(locationCode)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Location code cannot be blank");
        }
        if (locationCode.contains(" ")) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Location code cannot contain spaces");
        }
    }

    private void validateName(String locationName) {
        if (!StringUtils.hasText(locationName)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Location name cannot be blank");
        }
    }

    private void validateStatus(Integer status) {
        if (!MasterDataStatus.isValid(status)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Invalid location status");
        }
    }
}
