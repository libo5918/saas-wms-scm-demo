package com.example.scm.inventory;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.TenantContext;
import com.example.scm.inventory.application.command.StockAdjustCommand;
import com.example.scm.inventory.application.command.StockAdjustItemCommand;
import com.example.scm.inventory.application.command.StockInCommand;
import com.example.scm.inventory.application.command.StockInItemCommand;
import com.example.scm.inventory.application.command.StockLockCommand;
import com.example.scm.inventory.application.command.StockLockItemCommand;
import com.example.scm.inventory.application.command.StockOutCommand;
import com.example.scm.inventory.application.command.StockOutItemCommand;
import com.example.scm.inventory.application.command.StockTransferCommand;
import com.example.scm.inventory.application.command.StockTransferItemCommand;
import com.example.scm.inventory.application.command.StockUnlockCommand;
import com.example.scm.inventory.application.command.StockUnlockItemCommand;
import com.example.scm.inventory.application.query.InventoryBalanceDTO;
import com.example.scm.inventory.application.query.InventoryTransactionRecordDTO;
import com.example.scm.inventory.application.query.StockAdjustResultDTO;
import com.example.scm.inventory.application.query.StockInResultDTO;
import com.example.scm.inventory.application.query.StockLockResultDTO;
import com.example.scm.inventory.application.query.StockOutResultDTO;
import com.example.scm.inventory.application.query.StockTransferResultDTO;
import com.example.scm.inventory.application.query.StocktakeResultDTO;
import com.example.scm.inventory.application.query.StockUnlockResultDTO;
import com.example.scm.inventory.application.service.InventoryBalanceQueryService;
import com.example.scm.inventory.application.service.InventoryLockedStockOutApplicationService;
import com.example.scm.inventory.application.service.InventoryStockAdjustApplicationService;
import com.example.scm.inventory.application.service.InventoryStockInApplicationService;
import com.example.scm.inventory.application.service.InventoryStockLockApplicationService;
import com.example.scm.inventory.application.service.InventoryStockOutApplicationService;
import com.example.scm.inventory.application.service.InventoryStockTransferApplicationService;
import com.example.scm.inventory.application.service.InventoryStocktakeApplicationService;
import com.example.scm.inventory.application.service.InventoryStockUnlockApplicationService;
import com.example.scm.inventory.application.service.InventoryTransactionRecordQueryService;
import com.example.scm.inventory.domain.inventory.aggregate.InventoryBalance;
import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;
import com.example.scm.inventory.domain.inventory.repository.InventoryBalanceRepository;
import com.example.scm.inventory.domain.inventory.repository.InventoryTransactionRecordRepository;
import com.example.scm.inventory.domain.inventory.service.InventoryLockedStockOutDomainService;
import com.example.scm.inventory.domain.inventory.service.InventoryStockAdjustDomainService;
import com.example.scm.inventory.domain.inventory.service.InventoryStockInDomainService;
import com.example.scm.inventory.domain.inventory.service.InventoryStockLockDomainService;
import com.example.scm.inventory.domain.inventory.service.InventoryStockOutDomainService;
import com.example.scm.inventory.domain.inventory.service.InventoryStockTransferDomainService;
import com.example.scm.inventory.domain.inventory.service.InventoryStocktakeDomainService;
import com.example.scm.inventory.domain.inventory.service.InventoryStockUnlockDomainService;
import com.example.scm.inventory.domain.inventory.valueobject.InventoryKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryApplicationServiceTest {

    @Mock
    private InventoryStockInDomainService inventoryStockInDomainService;
    @Mock
    private InventoryStockAdjustDomainService inventoryStockAdjustDomainService;
    @Mock
    private InventoryStockOutDomainService inventoryStockOutDomainService;
    @Mock
    private InventoryLockedStockOutDomainService inventoryLockedStockOutDomainService;
    @Mock
    private InventoryStockLockDomainService inventoryStockLockDomainService;
    @Mock
    private InventoryStockUnlockDomainService inventoryStockUnlockDomainService;
    @Mock
    private InventoryStockTransferDomainService inventoryStockTransferDomainService;
    @Mock
    private InventoryStocktakeDomainService inventoryStocktakeDomainService;
    @Mock
    private InventoryBalanceRepository inventoryBalanceRepository;
    @Mock
    private InventoryTransactionRecordRepository inventoryTransactionRecordRepository;

    @InjectMocks
    private InventoryStockInApplicationService inventoryStockInApplicationService;
    @InjectMocks
    private InventoryStockAdjustApplicationService inventoryStockAdjustApplicationService;
    @InjectMocks
    private InventoryStockOutApplicationService inventoryStockOutApplicationService;
    @InjectMocks
    private InventoryLockedStockOutApplicationService inventoryLockedStockOutApplicationService;
    @InjectMocks
    private InventoryStockLockApplicationService inventoryStockLockApplicationService;
    @InjectMocks
    private InventoryStockUnlockApplicationService inventoryStockUnlockApplicationService;
    @InjectMocks
    private InventoryStockTransferApplicationService inventoryStockTransferApplicationService;
    @InjectMocks
    private InventoryStocktakeApplicationService inventoryStocktakeApplicationService;
    @InjectMocks
    private InventoryBalanceQueryService inventoryBalanceQueryService;
    @InjectMocks
    private InventoryTransactionRecordQueryService inventoryTransactionRecordQueryService;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void shouldBuildStockInResultFromDomainRecord() {
        TenantContext.setTenantId(1L);
        StockInCommand command = stockInCommand();
        InventoryTransactionRecord record = InventoryTransactionRecord.stockIn(
                1L, "IN-001", "PURCHASE_RECEIPT", "RCV-001", 1001L, 2001L, 3001L,
                new BigDecimal("5"), BigDecimal.ZERO, new BigDecimal("5"));
        when(inventoryStockInDomainService.stockIn(1L, "PURCHASE_RECEIPT", "RCV-001", 99L, 1001L, 2001L, 3001L, new BigDecimal("5")))
                .thenReturn(record);

        StockInResultDTO result = inventoryStockInApplicationService.stockIn(command);

        assertEquals("RCV-001", result.getBizNo());
        assertEquals("IN-001", result.getLines().getFirst().getTxnNo());
        assertEquals(new BigDecimal("5"), result.getLines().getFirst().getAfterQty());
    }

    @Test
    void shouldRejectInvalidStockInCommand() {
        StockInCommand command = new StockInCommand();
        command.setBizType("");

        assertThrows(BusinessException.class, () -> inventoryStockInApplicationService.stockIn(command));
    }

    @Test
    void shouldBuildAdjustResultFromDomainRecord() {
        TenantContext.setTenantId(1L);
        StockAdjustCommand command = stockAdjustCommand();
        InventoryTransactionRecord record = InventoryTransactionRecord.adjustIn(
                1L, "ADJ-REC-001", "MANUAL_ADJUST", "ADJ-001", 1001L, 2001L, 3001L,
                new BigDecimal("2"), new BigDecimal("8"), new BigDecimal("10"));
        when(inventoryStockAdjustDomainService.adjust(1L, "MANUAL_ADJUST", "ADJ-001", "INCREASE", 99L, 1001L, 2001L, 3001L, new BigDecimal("2")))
                .thenReturn(record);

        StockAdjustResultDTO result = inventoryStockAdjustApplicationService.adjust(command);

        assertEquals("INCREASE", result.getAdjustType());
        assertEquals("ADJ-REC-001", result.getLines().getFirst().getTxnNo());
    }

    @Test
    void shouldBuildStockOutAndLockedStockOutResults() {
        TenantContext.setTenantId(1L);
        StockOutCommand command = stockOutCommand();
        InventoryTransactionRecord outRecord = InventoryTransactionRecord.stockOut(
                1L, "OUT-001", "SALES_ORDER", "SO-001", 1001L, 2001L, 3001L,
                new BigDecimal("3"), new BigDecimal("10"), new BigDecimal("7"));
        InventoryTransactionRecord lockedOutRecord = InventoryTransactionRecord.stockOut(
                1L, "LOCKED-OUT-001", "SALES_ORDER", "SO-001", 1001L, 2001L, 3001L,
                new BigDecimal("3"), new BigDecimal("10"), new BigDecimal("7"));
        when(inventoryStockOutDomainService.stockOut(1L, "SALES_ORDER", "SO-001", 99L, 1001L, 2001L, 3001L, new BigDecimal("3")))
                .thenReturn(outRecord);
        when(inventoryLockedStockOutDomainService.stockOut(1L, "SALES_ORDER", "SO-001", 99L, 1001L, 2001L, 3001L, new BigDecimal("3")))
                .thenReturn(lockedOutRecord);

        StockOutResultDTO stockOutResult = inventoryStockOutApplicationService.stockOut(command);
        StockOutResultDTO lockedOutResult = inventoryLockedStockOutApplicationService.stockOut(command);

        assertEquals("OUT-001", stockOutResult.getLines().getFirst().getTxnNo());
        assertEquals("LOCKED-OUT-001", lockedOutResult.getLines().getFirst().getTxnNo());
    }

    @Test
    void shouldBuildLockAndUnlockResults() {
        TenantContext.setTenantId(1L);
        StockLockCommand lockCommand = stockLockCommand();
        StockUnlockCommand unlockCommand = stockUnlockCommand();
        InventoryTransactionRecord lockRecord = InventoryTransactionRecord.lock(
                1L, "LOCK-001", "SALES_ORDER", "SO-001", 1001L, 2001L, 3001L,
                new BigDecimal("3"), BigDecimal.ZERO, new BigDecimal("3"));
        InventoryTransactionRecord unlockRecord = InventoryTransactionRecord.unlock(
                1L, "UNLOCK-001", "SALES_ORDER", "SO-001", 1001L, 2001L, 3001L,
                new BigDecimal("1"), new BigDecimal("3"), new BigDecimal("2"));
        when(inventoryStockLockDomainService.lock(1L, "SALES_ORDER", "SO-001", 99L, 1001L, 2001L, 3001L, new BigDecimal("3")))
                .thenReturn(lockRecord);
        when(inventoryStockUnlockDomainService.unlock(1L, "SALES_ORDER", "SO-001", 99L, 1001L, 2001L, 3001L, new BigDecimal("1")))
                .thenReturn(unlockRecord);

        StockLockResultDTO lockResult = inventoryStockLockApplicationService.lock(lockCommand);
        StockUnlockResultDTO unlockResult = inventoryStockUnlockApplicationService.unlock(unlockCommand);

        assertEquals("LOCK-001", lockResult.getLines().getFirst().getTxnNo());
        assertEquals("UNLOCK-001", unlockResult.getLines().getFirst().getTxnNo());
    }

    @Test
    void shouldBuildTransferResultFromExecutionResult() {
        TenantContext.setTenantId(1L);
        StockTransferCommand command = stockTransferCommand();
        InventoryTransactionRecord moveOutRecord = InventoryTransactionRecord.moveOut(
                1L, "MOVEOUT-001", "INVENTORY_TRANSFER", "TRF-001", 1001L, 2001L, 3001L,
                new BigDecimal("4"), new BigDecimal("10"), new BigDecimal("6"));
        InventoryTransactionRecord moveInRecord = InventoryTransactionRecord.moveIn(
                1L, "MOVEIN-001", "INVENTORY_TRANSFER", "TRF-001", 1001L, 2002L, 3002L,
                new BigDecimal("4"), new BigDecimal("1"), new BigDecimal("5"));
        when(inventoryStockTransferDomainService.transfer(1L, "INVENTORY_TRANSFER", "TRF-001", 99L, 1001L, 2001L, 3001L, 2002L, 3002L, new BigDecimal("4")))
                .thenReturn(new InventoryStockTransferDomainService.TransferExecutionResult(moveOutRecord, moveInRecord));

        StockTransferResultDTO result = inventoryStockTransferApplicationService.transfer(command);

        assertEquals("MOVEOUT-001", result.getLines().getFirst().getMoveOutTxnNo());
        assertEquals("MOVEIN-001", result.getLines().getFirst().getMoveInTxnNo());
        assertEquals(new BigDecimal("5"), result.getLines().getFirst().getToAfterQty());
    }

    @Test
    void shouldBuildStocktakeResultFromExecutionResult() {
        TenantContext.setTenantId(1L);
        when(inventoryStocktakeDomainService.stocktake(1L, "STOCKTAKE", "STK-001", 99L, 1001L, 2001L, 3001L, new BigDecimal("12")))
                .thenReturn(new InventoryStocktakeDomainService.StocktakeExecutionResult(
                        new BigDecimal("10"),
                        new BigDecimal("12"),
                        new BigDecimal("2"),
                        "INCREASE",
                        InventoryTransactionRecord.adjustIn(
                                1L, "STKIN-001", "STOCKTAKE", "STK-001", 1001L, 2001L, 3001L,
                                new BigDecimal("2"), new BigDecimal("10"), new BigDecimal("12")
                        )));

        StocktakeResultDTO result = inventoryStocktakeApplicationService.stocktake(stocktakeCommand());

        assertEquals("STK-001", result.getBizNo());
        assertEquals("INCREASE", result.getLines().getFirst().getAdjustType());
        assertEquals("STKIN-001", result.getLines().getFirst().getTxnNo());
    }

    @Test
    void shouldQueryBalanceAndTransactionRecords() {
        TenantContext.setTenantId(1L);
        InventoryBalance balance = InventoryBalance.initialize(new InventoryKey(1L, 1001L, 2001L, 3001L), 99L);
        balance.stockIn("IN-INIT-001", "INIT", "INIT-001", new BigDecimal("6"), 99L);
        when(inventoryBalanceRepository.findByKey(new InventoryKey(1L, 1001L, 2001L, 3001L))).thenReturn(Optional.of(balance));
        when(inventoryTransactionRecordRepository.findByBizNo(1L, "PURCHASE_RECEIPT", "RCV-001"))
                .thenReturn(List.of(InventoryTransactionRecord.stockIn(
                        1L, "TXN-001", "PURCHASE_RECEIPT", "RCV-001", 1001L, 2001L, 3001L,
                        new BigDecimal("6"), BigDecimal.ZERO, new BigDecimal("6"))));

        InventoryBalanceDTO balanceDTO = inventoryBalanceQueryService.getBalance(1001L, 2001L, 3001L);
        List<InventoryTransactionRecordDTO> recordDTOs = inventoryTransactionRecordQueryService.listByBizNo("PURCHASE_RECEIPT", "RCV-001");

        assertEquals(new BigDecimal("6"), balanceDTO.getAvailableQty());
        assertEquals("TXN-001", recordDTOs.getFirst().getTxnNo());
        assertEquals("IN", recordDTOs.getFirst().getTxnDirection());
        verify(inventoryTransactionRecordRepository).findByBizNo(1L, "PURCHASE_RECEIPT", "RCV-001");
    }

    @Test
    void shouldThrowWhenBalanceDoesNotExist() {
        TenantContext.setTenantId(1L);
        when(inventoryBalanceRepository.findByKey(any())).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> inventoryBalanceQueryService.getBalance(1001L, 2001L, 3001L));
    }

    private StockInCommand stockInCommand() {
        StockInCommand command = new StockInCommand();
        command.setBizType("PURCHASE_RECEIPT");
        command.setBizNo("RCV-001");
        command.setOperatorId(99L);
        StockInItemCommand item = new StockInItemCommand();
        item.setMaterialId(1001L);
        item.setWarehouseId(2001L);
        item.setLocationId(3001L);
        item.setQuantity(new BigDecimal("5"));
        command.setItems(List.of(item));
        return command;
    }

    private StockAdjustCommand stockAdjustCommand() {
        StockAdjustCommand command = new StockAdjustCommand();
        command.setBizType("MANUAL_ADJUST");
        command.setBizNo("ADJ-001");
        command.setAdjustType("INCREASE");
        command.setOperatorId(99L);
        StockAdjustItemCommand item = new StockAdjustItemCommand();
        item.setMaterialId(1001L);
        item.setWarehouseId(2001L);
        item.setLocationId(3001L);
        item.setQuantity(new BigDecimal("2"));
        command.setItems(List.of(item));
        return command;
    }

    private StockOutCommand stockOutCommand() {
        StockOutCommand command = new StockOutCommand();
        command.setBizType("SALES_ORDER");
        command.setBizNo("SO-001");
        command.setOperatorId(99L);
        StockOutItemCommand item = new StockOutItemCommand();
        item.setMaterialId(1001L);
        item.setWarehouseId(2001L);
        item.setLocationId(3001L);
        item.setQuantity(new BigDecimal("3"));
        command.setItems(List.of(item));
        return command;
    }

    private StockLockCommand stockLockCommand() {
        StockLockCommand command = new StockLockCommand();
        command.setBizType("SALES_ORDER");
        command.setBizNo("SO-001");
        command.setOperatorId(99L);
        StockLockItemCommand item = new StockLockItemCommand();
        item.setMaterialId(1001L);
        item.setWarehouseId(2001L);
        item.setLocationId(3001L);
        item.setQuantity(new BigDecimal("3"));
        command.setItems(List.of(item));
        return command;
    }

    private StockUnlockCommand stockUnlockCommand() {
        StockUnlockCommand command = new StockUnlockCommand();
        command.setBizType("SALES_ORDER");
        command.setBizNo("SO-001");
        command.setOperatorId(99L);
        StockUnlockItemCommand item = new StockUnlockItemCommand();
        item.setMaterialId(1001L);
        item.setWarehouseId(2001L);
        item.setLocationId(3001L);
        item.setQuantity(new BigDecimal("1"));
        command.setItems(List.of(item));
        return command;
    }

    private StockTransferCommand stockTransferCommand() {
        StockTransferCommand command = new StockTransferCommand();
        command.setBizType("INVENTORY_TRANSFER");
        command.setBizNo("TRF-001");
        command.setOperatorId(99L);
        StockTransferItemCommand item = new StockTransferItemCommand();
        item.setMaterialId(1001L);
        item.setFromWarehouseId(2001L);
        item.setFromLocationId(3001L);
        item.setToWarehouseId(2002L);
        item.setToLocationId(3002L);
        item.setQuantity(new BigDecimal("4"));
        command.setItems(List.of(item));
        return command;
    }

    private com.example.scm.inventory.application.command.StocktakeCommand stocktakeCommand() {
        com.example.scm.inventory.application.command.StocktakeCommand command = new com.example.scm.inventory.application.command.StocktakeCommand();
        command.setBizType("STOCKTAKE");
        command.setBizNo("STK-001");
        command.setOperatorId(99L);
        com.example.scm.inventory.application.command.StocktakeItemCommand item = new com.example.scm.inventory.application.command.StocktakeItemCommand();
        item.setMaterialId(1001L);
        item.setWarehouseId(2001L);
        item.setLocationId(3001L);
        item.setCountedQty(new BigDecimal("12"));
        command.setItems(List.of(item));
        return command;
    }
}
