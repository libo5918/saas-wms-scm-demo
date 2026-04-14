package com.example.scm.inventory;

import com.example.scm.common.core.BusinessException;
import com.example.scm.inventory.domain.inventory.aggregate.InventoryBalance;
import com.example.scm.inventory.domain.inventory.repository.InventoryBalanceRepository;
import com.example.scm.inventory.domain.inventory.repository.InventoryTransactionRecordRepository;
import com.example.scm.inventory.domain.inventory.service.InventoryStocktakeDomainService;
import com.example.scm.inventory.domain.inventory.valueobject.InventoryKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryStocktakeDomainServiceTest {

    @Mock
    private InventoryBalanceRepository inventoryBalanceRepository;

    @Mock
    private InventoryTransactionRecordRepository inventoryTransactionRecordRepository;

    @InjectMocks
    private InventoryStocktakeDomainService inventoryStocktakeDomainService;

    @Test
    void shouldIncreaseInventoryWhenCountedQtyIsGreater() {
        InventoryBalance balance = InventoryBalance.initialize(new InventoryKey(1L, 1001L, 2001L, 3001L), 1L);
        balance.stockIn("IN-INIT-001", "INIT", "INIT-001", new BigDecimal("10"), 1L);

        when(inventoryBalanceRepository.findByKey(any())).thenReturn(Optional.of(balance));
        when(inventoryTransactionRecordRepository.existsAdjustInRecord(1L, "STOCKTAKE", "STK-001", 1001L, 2001L, 3001L))
                .thenReturn(false);

        InventoryStocktakeDomainService.StocktakeExecutionResult result = inventoryStocktakeDomainService.stocktake(
                1L, "STOCKTAKE", "STK-001", 1L, 1001L, 2001L, 3001L, new BigDecimal("12")
        );

        assertEquals("INCREASE", result.adjustType());
        assertEquals(new BigDecimal("2"), result.varianceQty());
        assertEquals(new BigDecimal("12"), balance.getOnHandQty());
        verify(inventoryBalanceRepository).save(balance);
        verify(inventoryTransactionRecordRepository).save(result.transactionRecord());
    }

    @Test
    void shouldDecreaseInventoryWhenCountedQtyIsLower() {
        InventoryBalance balance = InventoryBalance.initialize(new InventoryKey(1L, 1001L, 2001L, 3001L), 1L);
        balance.stockIn("IN-INIT-001", "INIT", "INIT-001", new BigDecimal("10"), 1L);

        when(inventoryBalanceRepository.findByKey(any())).thenReturn(Optional.of(balance));
        when(inventoryTransactionRecordRepository.existsAdjustOutRecord(1L, "STOCKTAKE", "STK-002", 1001L, 2001L, 3001L))
                .thenReturn(false);

        InventoryStocktakeDomainService.StocktakeExecutionResult result = inventoryStocktakeDomainService.stocktake(
                1L, "STOCKTAKE", "STK-002", 1L, 1001L, 2001L, 3001L, new BigDecimal("7")
        );

        assertEquals("DECREASE", result.adjustType());
        assertEquals(new BigDecimal("-3"), result.varianceQty());
        assertEquals(new BigDecimal("7"), balance.getOnHandQty());
    }

    @Test
    void shouldReturnNoAdjustmentWhenCountedQtyMatchesSystemQty() {
        InventoryBalance balance = InventoryBalance.initialize(new InventoryKey(1L, 1001L, 2001L, 3001L), 1L);
        balance.stockIn("IN-INIT-001", "INIT", "INIT-001", new BigDecimal("10"), 1L);

        when(inventoryBalanceRepository.findByKey(any())).thenReturn(Optional.of(balance));

        InventoryStocktakeDomainService.StocktakeExecutionResult result = inventoryStocktakeDomainService.stocktake(
                1L, "STOCKTAKE", "STK-003", 1L, 1001L, 2001L, 3001L, new BigDecimal("10")
        );

        assertEquals("NONE", result.adjustType());
        assertEquals(BigDecimal.ZERO, result.varianceQty());
        assertNull(result.transactionRecord());
        verify(inventoryBalanceRepository, never()).save(any());
    }

    @Test
    void shouldRejectNegativeCountedQty() {
        assertThrows(BusinessException.class, () -> inventoryStocktakeDomainService.stocktake(
                1L, "STOCKTAKE", "STK-004", 1L, 1001L, 2001L, 3001L, new BigDecimal("-1")
        ));
    }
}
