package com.example.scm.inventory.application.service;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.inventory.application.command.StockInCommand;
import com.example.scm.inventory.application.command.StockInItemCommand;
import com.example.scm.inventory.application.query.StockInLineResultDTO;
import com.example.scm.inventory.application.query.StockInResultDTO;
import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;
import com.example.scm.inventory.domain.inventory.service.InventoryStockInDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InventoryStockInApplicationService {

    private final InventoryStockInDomainService inventoryStockInDomainService;

    public InventoryStockInApplicationService(InventoryStockInDomainService inventoryStockInDomainService) {
        this.inventoryStockInDomainService = inventoryStockInDomainService;
    }

    @Transactional
    public StockInResultDTO stockIn(StockInCommand command) {
        validateCommand(command);
        Long tenantId = TenantContext.getRequiredTenantId();

        StockInResultDTO result = new StockInResultDTO();
        result.setBizType(command.getBizType());
        result.setBizNo(command.getBizNo());

        for (StockInItemCommand item : command.getItems()) {
            InventoryTransactionRecord transactionRecord = inventoryStockInDomainService.stockIn(
                    tenantId,
                    command.getBizType(),
                    command.getBizNo(),
                    command.getOperatorId(),
                    item.getMaterialId(),
                    item.getWarehouseId(),
                    item.getLocationId(),
                    item.getQuantity()
            );

            StockInLineResultDTO line = new StockInLineResultDTO();
            line.setTxnNo(transactionRecord.getTxnNo());
            line.setMaterialId(transactionRecord.getMaterialId());
            line.setWarehouseId(transactionRecord.getWarehouseId());
            line.setLocationId(transactionRecord.getLocationId());
            line.setQuantity(transactionRecord.getTxnQty());
            line.setBeforeQty(transactionRecord.getBeforeQty());
            line.setAfterQty(transactionRecord.getAfterQty());
            result.getLines().add(line);
        }
        return result;
    }

    private void validateCommand(StockInCommand command) {
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
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "stock-in items cannot be empty");
        }
    }
}
