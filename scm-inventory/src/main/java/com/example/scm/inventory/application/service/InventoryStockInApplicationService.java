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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 库存入库应用服务，负责编排业务单入库流程。
 */
@Service
@Slf4j
public class InventoryStockInApplicationService {

    private final InventoryStockInDomainService inventoryStockInDomainService;

    public InventoryStockInApplicationService(InventoryStockInDomainService inventoryStockInDomainService) {
        this.inventoryStockInDomainService = inventoryStockInDomainService;
    }

    /**
     * 按业务单执行库存入库，并返回入库结果。
     */
    @Transactional
    public StockInResultDTO stockIn(StockInCommand command) {
        validateCommand(command);
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Start stock-in application flow, tenantId={}, bizType={}, bizNo={}, itemCount={}",
                tenantId, command.getBizType(), command.getBizNo(), command.getItems().size());

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
            log.info("Stock-in line success, tenantId={}, txnNo={}, materialId={}, warehouseId={}, locationId={}, qty={}",
                    tenantId, transactionRecord.getTxnNo(), transactionRecord.getMaterialId(),
                    transactionRecord.getWarehouseId(), transactionRecord.getLocationId(), transactionRecord.getTxnQty());
        }
        log.info("Finish stock-in application flow, tenantId={}, bizType={}, bizNo={}, lineCount={}",
                tenantId, result.getBizType(), result.getBizNo(), result.getLines().size());
        return result;
    }

    /**
     * 校验应用服务层入参完整性。
     */
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
