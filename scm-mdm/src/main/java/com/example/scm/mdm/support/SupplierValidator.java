package com.example.scm.mdm.support;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.mdm.constant.MasterDataStatus;
import com.example.scm.mdm.dto.CreateSupplierRequest;
import com.example.scm.mdm.dto.UpdateSupplierRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SupplierValidator {

    public void validateForCreate(CreateSupplierRequest request) {
        validateStatus(request.getStatus());
        validateCode(request.getSupplierCode());
        validateName(request.getSupplierName());
    }

    public void validateForUpdate(UpdateSupplierRequest request) {
        validateStatus(request.getStatus());
        validateName(request.getSupplierName());
    }

    private void validateCode(String supplierCode) {
        if (!StringUtils.hasText(supplierCode)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Supplier code cannot be blank");
        }
        if (supplierCode.contains(" ")) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Supplier code cannot contain spaces");
        }
    }

    private void validateName(String supplierName) {
        if (!StringUtils.hasText(supplierName)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Supplier name cannot be blank");
        }
    }

    private void validateStatus(Integer status) {
        if (!MasterDataStatus.isValid(status)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Invalid supplier status");
        }
    }
}
