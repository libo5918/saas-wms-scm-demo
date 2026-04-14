package com.example.scm.inventory.application.service;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.inventory.application.command.StocktakeCommand;
import com.example.scm.inventory.application.command.StocktakeItemCommand;
import com.example.scm.inventory.application.query.StocktakeLineResultDTO;
import com.example.scm.inventory.application.query.StocktakeResultDTO;
import com.example.scm.inventory.domain.inventory.service.InventoryStocktakeDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 库存盘点应用服务。
 * 适用于调用方只掌握实盘数量的场景，由系统自动比较账面库存并推导差异处理结果。
 */
@Service
public class InventoryStocktakeApplicationService {

    private final MaterialValidationService materialValidationService;
    private final StorageValidationService storageValidationService;
    private final InventoryStocktakeDomainService inventoryStocktakeDomainService;

    public InventoryStocktakeApplicationService(MaterialValidationService materialValidationService,
                                                StorageValidationService storageValidationService,
                                                InventoryStocktakeDomainService inventoryStocktakeDomainService) {
        this.materialValidationService = materialValidationService;
        this.storageValidationService = storageValidationService;
        this.inventoryStocktakeDomainService = inventoryStocktakeDomainService;
    }

    /**
     * 执行库存盘点。
     * 调用方只需要传入实盘数量，系统会自动计算差异并决定是增量调整、减量调整还是无差异。
     */
    @Transactional
    public StocktakeResultDTO stocktake(StocktakeCommand command) {
        validateCommand(command);
        Long tenantId = TenantContext.getRequiredTenantId();
        StocktakeResultDTO result = new StocktakeResultDTO();
        result.setBizType(command.getBizType());
        result.setBizNo(command.getBizNo());

        for (StocktakeItemCommand item : command.getItems()) {
            materialValidationService.validateMaterialEnabled(tenantId, item.getMaterialId());
            storageValidationService.validateStorageEnabled(tenantId, item.getWarehouseId(), item.getLocationId());
            InventoryStocktakeDomainService.StocktakeExecutionResult executionResult = inventoryStocktakeDomainService.stocktake(
                    tenantId,
                    command.getBizType(),
                    command.getBizNo(),
                    command.getOperatorId(),
                    item.getMaterialId(),
                    item.getWarehouseId(),
                    item.getLocationId(),
                    item.getCountedQty()
            );
            StocktakeLineResultDTO line = new StocktakeLineResultDTO();
            line.setTxnNo(executionResult.transactionRecord() == null ? null : executionResult.transactionRecord().getTxnNo());
            line.setMaterialId(item.getMaterialId());
            line.setWarehouseId(item.getWarehouseId());
            line.setLocationId(item.getLocationId());
            line.setSystemQty(executionResult.systemQty());
            line.setCountedQty(executionResult.countedQty());
            line.setVarianceQty(executionResult.varianceQty());
            line.setAdjustType(executionResult.adjustType());
            result.getLines().add(line);
        }

        return result;
    }

    /**
     * 校验盘点请求的基本完整性。
     */
    private void validateCommand(StocktakeCommand command) {
        if (!StringUtils.hasText(command.getBizType())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "bizType cannot be blank");
        }
        if (!StringUtils.hasText(command.getBizNo())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "bizNo cannot be blank");
        }
        if (command.getOperatorId() == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "operatorId cannot be null");
        }
        if (command.getItems() == null || command.getItems().isEmpty()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stocktake items cannot be empty");
        }
    }
}
