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

@Service
@Slf4j
public class InventoryStockLockApplicationService {

    private final InventoryStockLockDomainService inventoryStockLockDomainService;

    public InventoryStockLockApplicationService(InventoryStockLockDomainService inventoryStockLockDomainService) {
        this.inventoryStockLockDomainService = inventoryStockLockDomainService;
    }

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
