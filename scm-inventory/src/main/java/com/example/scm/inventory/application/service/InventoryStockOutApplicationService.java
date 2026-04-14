package com.example.scm.inventory.application.service;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.inventory.application.command.StockOutCommand;
import com.example.scm.inventory.application.command.StockOutItemCommand;
import com.example.scm.inventory.application.query.StockOutLineResultDTO;
import com.example.scm.inventory.application.query.StockOutResultDTO;
import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;
import com.example.scm.inventory.domain.inventory.service.InventoryStockOutDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 库存出库应用服务。
 * 负责组织普通出库流程，并将领域层结果转换为接口输出 DTO。
 */
@Service
@Slf4j
public class InventoryStockOutApplicationService {

    private final MaterialValidationService materialValidationService;
    private final StorageValidationService storageValidationService;
    private final InventoryStockOutDomainService inventoryStockOutDomainService;

    public InventoryStockOutApplicationService(MaterialValidationService materialValidationService,
                                               StorageValidationService storageValidationService,
                                               InventoryStockOutDomainService inventoryStockOutDomainService) {
        this.materialValidationService = materialValidationService;
        this.storageValidationService = storageValidationService;
        this.inventoryStockOutDomainService = inventoryStockOutDomainService;
    }

    /**
     * 按业务单执行库存出库，并返回出库结果。
     */
    @Transactional
    public StockOutResultDTO stockOut(StockOutCommand command) {
        validateCommand(command);
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Start stock-out application flow, tenantId={}, bizType={}, bizNo={}, itemCount={}",
                tenantId, command.getBizType(), command.getBizNo(), command.getItems().size());

        StockOutResultDTO result = new StockOutResultDTO();
        result.setBizType(command.getBizType());
        result.setBizNo(command.getBizNo());

        for (StockOutItemCommand item : command.getItems()) {
            materialValidationService.validateMaterialEnabled(tenantId, item.getMaterialId());
            storageValidationService.validateStorageEnabled(tenantId, item.getWarehouseId(), item.getLocationId());
            InventoryTransactionRecord transactionRecord = inventoryStockOutDomainService.stockOut(
                    tenantId,
                    command.getBizType(),
                    command.getBizNo(),
                    command.getOperatorId(),
                    item.getMaterialId(),
                    item.getWarehouseId(),
                    item.getLocationId(),
                    item.getQuantity()
            );
            StockOutLineResultDTO line = new StockOutLineResultDTO();
            line.setTxnNo(transactionRecord.getTxnNo());
            line.setMaterialId(transactionRecord.getMaterialId());
            line.setWarehouseId(transactionRecord.getWarehouseId());
            line.setLocationId(transactionRecord.getLocationId());
            line.setQuantity(transactionRecord.getTxnQty());
            line.setBeforeQty(transactionRecord.getBeforeQty());
            line.setAfterQty(transactionRecord.getAfterQty());
            result.getLines().add(line);
            log.info("Stock-out line success, tenantId={}, txnNo={}, materialId={}, warehouseId={}, locationId={}, qty={}",
                    tenantId, transactionRecord.getTxnNo(), transactionRecord.getMaterialId(),
                    transactionRecord.getWarehouseId(), transactionRecord.getLocationId(), transactionRecord.getTxnQty());
        }

        log.info("Finish stock-out application flow, tenantId={}, bizType={}, bizNo={}, lineCount={}",
                tenantId, result.getBizType(), result.getBizNo(), result.getLines().size());
        return result;
    }

    /**
     * 校验出库请求的基本完整性。
     */
    private void validateCommand(StockOutCommand command) {
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
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stock-out items cannot be empty");
        }
    }
}
