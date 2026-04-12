package com.example.scm.mdm.support;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.mdm.constant.MaterialStatus;
import com.example.scm.mdm.dto.CreateMaterialRequest;
import com.example.scm.mdm.dto.UpdateMaterialRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MaterialValidator {

    public void validateForCreate(CreateMaterialRequest request) {
        validateStatus(request.getStatus());
        validateCode(request.getMaterialCode());
        validateName(request.getMaterialName());
    }

    public void validateForUpdate(UpdateMaterialRequest request) {
        validateStatus(request.getStatus());
        validateName(request.getMaterialName());
    }

    private void validateCode(String materialCode) {
        if (!StringUtils.hasText(materialCode)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Material code cannot be blank");
        }
        if (materialCode.contains(" ")) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Material code cannot contain spaces");
        }
    }

    private void validateName(String materialName) {
        if (!StringUtils.hasText(materialName)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Material name cannot be blank");
        }
    }

    private void validateStatus(Integer status) {
        if (!MaterialStatus.isValid(status)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Invalid material status");
        }
    }
}
