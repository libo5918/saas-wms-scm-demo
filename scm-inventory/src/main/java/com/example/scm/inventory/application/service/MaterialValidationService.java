package com.example.scm.inventory.application.service;

import com.example.scm.inventory.integration.MaterialClient;
import org.springframework.stereotype.Service;

/**
 * 物料校验服务。
 * 负责在库存写操作前统一校验物料是否存在且处于启用状态。
 */
@Service
public class MaterialValidationService {

    private final MaterialClient materialClient;

    public MaterialValidationService(MaterialClient materialClient) {
        this.materialClient = materialClient;
    }

    /**
     * 校验单个物料是否可用于库存业务。
     */
    public void validateMaterialEnabled(Long tenantId, Long materialId) {
        materialClient.validateMaterialEnabled(tenantId, materialId);
    }
}
