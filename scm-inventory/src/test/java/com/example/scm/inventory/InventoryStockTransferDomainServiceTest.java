package com.example.scm.inventory;

import com.example.scm.common.core.BusinessException;
import com.example.scm.inventory.domain.inventory.aggregate.InventoryBalance;
import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;
import com.example.scm.inventory.domain.inventory.repository.InventoryBalanceRepository;
import com.example.scm.inventory.domain.inventory.repository.InventoryTransactionRecordRepository;
import com.example.scm.inventory.domain.inventory.service.InventoryStockTransferDomainService;
import com.example.scm.inventory.domain.inventory.valueobject.InventoryKey;
import com.example.scm.inventory.domain.inventory.valueobject.InventoryTransactionDirection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryStockTransferDomainServiceTest {

    @Mock
    private InventoryBalanceRepository inventoryBalanceRepository;

    @Mock
    private InventoryTransactionRecordRepository inventoryTransactionRecordRepository;

    @InjectMocks
    private InventoryStockTransferDomainService inventoryStockTransferDomainService;

    @Test
    void shouldTransferSuccessfully() {
        InventoryBalance fromBalance = InventoryBalance.initialize(new InventoryKey(1L, 1001L, 2001L, 3001L), 1L);
        fromBalance.stockIn("IN-INIT-001", "INIT", "INIT-001", new BigDecimal("10"), 1L);
        InventoryBalance toBalance = InventoryBalance.initialize(new InventoryKey(1L, 1001L, 2002L, 3002L), 1L);
        toBalance.stockIn("IN-INIT-002", "INIT", "INIT-002", new BigDecimal("2"), 1L);

        when(inventoryTransactionRecordRepository.existsMoveOutRecord(1L, "INVENTORY_TRANSFER", "TRF-001", 1001L, 2001L, 3001L))
                .thenReturn(false);
        when(inventoryTransactionRecordRepository.existsMoveInRecord(1L, "INVENTORY_TRANSFER", "TRF-001", 1001L, 2002L, 3002L))
                .thenReturn(false);
        when(inventoryBalanceRepository.findByKey(new InventoryKey(1L, 1001L, 2001L, 3001L))).thenReturn(Optional.of(fromBalance));
        when(inventoryBalanceRepository.findByKey(new InventoryKey(1L, 1001L, 2002L, 3002L))).thenReturn(Optional.of(toBalance));

        InventoryStockTransferDomainService.TransferExecutionResult result = inventoryStockTransferDomainService.transfer(
                1L, "INVENTORY_TRANSFER", "TRF-001", 1L, 1001L, 2001L, 3001L, 2002L, 3002L, new BigDecimal("3")
        );

        assertEquals(InventoryTransactionDirection.MOVE_OUT, result.moveOutRecord().getTxnDirection());
        assertEquals(InventoryTransactionDirection.MOVE_IN, result.moveInRecord().getTxnDirection());
        assertEquals(new BigDecimal("7"), fromBalance.getOnHandQty());
        assertEquals(new BigDecimal("7"), fromBalance.getAvailableQty());
        assertEquals(new BigDecimal("5"), toBalance.getOnHandQty());
        assertEquals(new BigDecimal("5"), toBalance.getAvailableQty());
        verify(inventoryBalanceRepository, times(2)).save(any(InventoryBalance.class));
        verify(inventoryTransactionRecordRepository, times(2)).save(any(InventoryTransactionRecord.class));
    }

    @Test
    void shouldInitializeTargetBalanceWhenMissing() {
        InventoryBalance fromBalance = InventoryBalance.initialize(new InventoryKey(1L, 1001L, 2001L, 3001L), 1L);
        fromBalance.stockIn("IN-INIT-001", "INIT", "INIT-001", new BigDecimal("8"), 1L);

        when(inventoryTransactionRecordRepository.existsMoveOutRecord(1L, "INVENTORY_TRANSFER", "TRF-002", 1001L, 2001L, 3001L))
                .thenReturn(false);
        when(inventoryTransactionRecordRepository.existsMoveInRecord(1L, "INVENTORY_TRANSFER", "TRF-002", 1001L, 2002L, 3002L))
                .thenReturn(false);
        when(inventoryBalanceRepository.findByKey(new InventoryKey(1L, 1001L, 2001L, 3001L))).thenReturn(Optional.of(fromBalance));
        when(inventoryBalanceRepository.findByKey(new InventoryKey(1L, 1001L, 2002L, 3002L))).thenReturn(Optional.empty());

        InventoryStockTransferDomainService.TransferExecutionResult result = inventoryStockTransferDomainService.transfer(
                1L, "INVENTORY_TRANSFER", "TRF-002", 1L, 1001L, 2001L, 3001L, 2002L, 3002L, new BigDecimal("3")
        );

        assertEquals(new BigDecimal("5"), fromBalance.getOnHandQty());
        assertEquals(BigDecimal.ZERO, result.moveInRecord().getBeforeQty());
        assertEquals(new BigDecimal("3"), result.moveInRecord().getAfterQty());

        ArgumentCaptor<InventoryBalance> balanceCaptor = ArgumentCaptor.forClass(InventoryBalance.class);
        verify(inventoryBalanceRepository, times(2)).save(balanceCaptor.capture());
        InventoryBalance initializedTargetBalance = balanceCaptor.getAllValues().get(1);
        assertEquals(2002L, initializedTargetBalance.getInventoryKey().getWarehouseId());
        assertEquals(3002L, initializedTargetBalance.getInventoryKey().getLocationId());
        assertEquals(new BigDecimal("3"), initializedTargetBalance.getOnHandQty());
    }

    @Test
    void shouldRejectWhenSourceAndTargetLocationAreTheSame() {
        assertThrows(BusinessException.class, () -> inventoryStockTransferDomainService.transfer(
                1L, "INVENTORY_TRANSFER", "TRF-003", 1L, 1001L, 2001L, 3001L, 2001L, 3001L, new BigDecimal("1")
        ));

        verify(inventoryBalanceRepository, never()).findByKey(any());
        verify(inventoryTransactionRecordRepository, never()).save(any());
    }
}
