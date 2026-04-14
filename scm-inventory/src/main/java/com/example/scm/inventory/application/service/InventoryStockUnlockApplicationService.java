package com.example.scm.inventory.application.service;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.inventory.application.command.StockUnlockCommand;
import com.example.scm.inventory.application.command.StockUnlockItemCommand;
import com.example.scm.inventory.application.query.StockUnlockLineResultDTO;
import com.example.scm.inventory.application.query.StockUnlockResultDTO;
import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;
import com.example.scm.inventory.domain.inventory.service.InventoryStockUnlockDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class InventoryStockUnlockApplicationService {

    private final InventoryStockUnlockDomainService inventoryStockUnlockDomainService;

    public InventoryStockUnlockApplicationService(InventoryStockUnlockDomainService inventoryStockUnlockDomainService) {
        this.inventoryStockUnlockDomainService = inventoryStockUnlockDomainService;
    }

    @Transactional
    public StockUnlockResultDTO unlock(StockUnlockCommand command) {
        validateCommand(command);
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Start stock-unlock application flow, tenantId={}, bizType={}, bizNo={}, itemCount={}",
                tenantId, command.getBizType(), command.getBizNo(), command.getItems().size());

        StockUnlockResultDTO result = new StockUnlockResultDTO();
        result.setBizType(command.getBizType());
        result.setBizNo(command.getBizNo());

        for (StockUnlockItemCommand item : command.getItems()) {
            InventoryTransactionRecord record = inventoryStockUnlockDomainService.unlock(
                    tenantId, command.getBizType(), command.getBizNo(), command.getOperatorId(),
                    item.getMaterialId(), item.getWarehouseId(), item.getLocationId(), item.getQuantity());
            StockUnlockLineResultDTO line = new StockUnlockLineResultDTO();
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

    private void validateCommand(StockUnlockCommand command) {
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
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stock-unlock items cannot be empty");
        }
    }
}
