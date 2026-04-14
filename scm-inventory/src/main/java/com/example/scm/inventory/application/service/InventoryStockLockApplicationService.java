package com.example.scm.inventory.application.service;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.inventory.application.command.StockLockCommand;
import com.example.scm.inventory.application.command.StockLockItemCommand;
import com.example.scm.inventory.application.query.StockLockLineResultDTO;
import com.example.scm.inventory.application.query.StockLockResultDTO;
import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;
import com.example.scm.inventory.domain.inventory.service.InventoryStockLockDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 库存锁库应用服务。
 * 负责按业务单冻结可用库存，并将领域结果组装成接口输出。
 */
@Service
@Slf4j
public class InventoryStockLockApplicationService {

    private final MaterialValidationService materialValidationService;
    private final StorageValidationService storageValidationService;
    private final InventoryStockLockDomainService inventoryStockLockDomainService;

    public InventoryStockLockApplicationService(MaterialValidationService materialValidationService,
                                                StorageValidationService storageValidationService,
                                                InventoryStockLockDomainService inventoryStockLockDomainService) {
        this.materialValidationService = materialValidationService;
        this.storageValidationService = storageValidationService;
        this.inventoryStockLockDomainService = inventoryStockLockDomainService;
    }

    /**
     * 执行库存锁定。
     */
    @Transactional
    public StockLockResultDTO lock(StockLockCommand command) {
        validateCommand(command);
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Start stock-lock application flow, tenantId={}, bizType={}, bizNo={}, itemCount={}",
                tenantId, command.getBizType(), command.getBizNo(), command.getItems().size());

        StockLockResultDTO result = new StockLockResultDTO();
        result.setBizType(command.getBizType());
        result.setBizNo(command.getBizNo());

        for (StockLockItemCommand item : command.getItems()) {
            materialValidationService.validateMaterialEnabled(tenantId, item.getMaterialId());
            storageValidationService.validateStorageEnabled(tenantId, item.getWarehouseId(), item.getLocationId());
            InventoryTransactionRecord record = inventoryStockLockDomainService.lock(
                    tenantId, command.getBizType(), command.getBizNo(), command.getOperatorId(),
                    item.getMaterialId(), item.getWarehouseId(), item.getLocationId(), item.getQuantity());
            StockLockLineResultDTO line = new StockLockLineResultDTO();
            line.setTxnNo(record.getTxnNo());
            line.setMaterialId(record.getMaterialId());
            line.setWarehouseId(record.getWarehouseId());
            line.setLocationId(record.getLocationId());
            line.setQuantity(record.getTxnQty());
            line.setBeforeQty(record.getBeforeQty());
            line.setAfterQty(record.getAfterQty());
            result.getLines().add(line);
        }
        return result;
    }

    /**
     * 校验锁库请求的基本完整性。
     */
    private void validateCommand(StockLockCommand command) {
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
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stock-lock items cannot be empty");
        }
    }
}
