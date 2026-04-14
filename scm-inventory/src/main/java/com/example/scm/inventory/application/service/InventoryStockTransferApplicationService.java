package com.example.scm.inventory.application.service;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.inventory.application.command.StockTransferCommand;
import com.example.scm.inventory.application.command.StockTransferItemCommand;
import com.example.scm.inventory.application.query.StockTransferLineResultDTO;
import com.example.scm.inventory.application.query.StockTransferResultDTO;
import com.example.scm.inventory.domain.inventory.service.InventoryStockTransferDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 库存移库应用服务。
 * 负责组织源库位移出、目标库位移入，并返回移库执行结果。
 */
@Service
@Slf4j
public class InventoryStockTransferApplicationService {

    private final MaterialValidationService materialValidationService;
    private final StorageValidationService storageValidationService;
    private final InventoryStockTransferDomainService inventoryStockTransferDomainService;

    public InventoryStockTransferApplicationService(MaterialValidationService materialValidationService,
                                                    StorageValidationService storageValidationService,
                                                    InventoryStockTransferDomainService inventoryStockTransferDomainService) {
        this.materialValidationService = materialValidationService;
        this.storageValidationService = storageValidationService;
        this.inventoryStockTransferDomainService = inventoryStockTransferDomainService;
    }

    /**
     * 执行库存移库。
     */
    @Transactional
    public StockTransferResultDTO transfer(StockTransferCommand command) {
        validateCommand(command);
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Start stock-transfer application flow, tenantId={}, bizType={}, bizNo={}, itemCount={}",
                tenantId, command.getBizType(), command.getBizNo(), command.getItems().size());

        StockTransferResultDTO result = new StockTransferResultDTO();
        result.setBizType(command.getBizType());
        result.setBizNo(command.getBizNo());

        for (StockTransferItemCommand item : command.getItems()) {
            materialValidationService.validateMaterialEnabled(tenantId, item.getMaterialId());
            storageValidationService.validateStorageEnabled(tenantId, item.getFromWarehouseId(), item.getFromLocationId());
            storageValidationService.validateStorageEnabled(tenantId, item.getToWarehouseId(), item.getToLocationId());
            InventoryStockTransferDomainService.TransferExecutionResult executionResult = inventoryStockTransferDomainService.transfer(
                    tenantId,
                    command.getBizType(),
                    command.getBizNo(),
                    command.getOperatorId(),
                    item.getMaterialId(),
                    item.getFromWarehouseId(),
                    item.getFromLocationId(),
                    item.getToWarehouseId(),
                    item.getToLocationId(),
                    item.getQuantity()
            );
            StockTransferLineResultDTO line = new StockTransferLineResultDTO();
            line.setMoveOutTxnNo(executionResult.moveOutRecord().getTxnNo());
            line.setMoveInTxnNo(executionResult.moveInRecord().getTxnNo());
            line.setMaterialId(item.getMaterialId());
            line.setFromWarehouseId(item.getFromWarehouseId());
            line.setFromLocationId(item.getFromLocationId());
            line.setToWarehouseId(item.getToWarehouseId());
            line.setToLocationId(item.getToLocationId());
            line.setQuantity(item.getQuantity());
            line.setFromBeforeQty(executionResult.moveOutRecord().getBeforeQty());
            line.setFromAfterQty(executionResult.moveOutRecord().getAfterQty());
            line.setToBeforeQty(executionResult.moveInRecord().getBeforeQty());
            line.setToAfterQty(executionResult.moveInRecord().getAfterQty());
            result.getLines().add(line);
        }

        log.info("Finish stock-transfer application flow, tenantId={}, bizType={}, bizNo={}, lineCount={}",
                tenantId, result.getBizType(), result.getBizNo(), result.getLines().size());
        return result;
    }

    /**
     * 校验移库请求的基本完整性。
     */
    private void validateCommand(StockTransferCommand command) {
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
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stock-transfer items cannot be empty");
        }
    }
}
