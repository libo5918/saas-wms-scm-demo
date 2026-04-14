package com.example.scm.mdm.support;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.mdm.constant.MasterDataStatus;
import com.example.scm.mdm.dto.CreateWarehouseRequest;
import com.example.scm.mdm.dto.UpdateWarehouseRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WarehouseValidator {

    public void validateForCreate(CreateWarehouseRequest request) {
        validateStatus(request.getStatus());
        validateCode(request.getWarehouseCode());
        validateName(request.getWarehouseName());
    }

    public void validateForUpdate(UpdateWarehouseRequest request) {
        validateStatus(request.getStatus());
        validateName(request.getWarehouseName());
    }

    private void validateCode(String warehouseCode) {
        if (!StringUtils.hasText(warehouseCode)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Warehouse code cannot be blank");
        }
        if (warehouseCode.contains(" ")) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Warehouse code cannot contain spaces");
        }
    }

    private void validateName(String warehouseName) {
        if (!StringUtils.hasText(warehouseName)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Warehouse name cannot be blank");
        }
    }

    private void validateStatus(Integer status) {
        if (!MasterDataStatus.isValid(status)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Invalid warehouse status");
        }
    }
}
