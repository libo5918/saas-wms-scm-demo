package com.example.scm.inventory;

import com.example.scm.common.core.BusinessException;
import com.example.scm.inventory.domain.inventory.aggregate.InventoryBalance;
import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;
import com.example.scm.inventory.domain.inventory.repository.InventoryBalanceRepository;
import com.example.scm.inventory.domain.inventory.repository.InventoryTransactionRecordRepository;
import com.example.scm.inventory.domain.inventory.service.InventoryStockLockDomainService;
import com.example.scm.inventory.domain.inventory.service.InventoryStockUnlockDomainService;
import com.example.scm.inventory.domain.inventory.valueobject.InventoryKey;
import com.example.scm.inventory.domain.inventory.valueobject.InventoryTransactionDirection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryStockLockUnlockDomainServiceTest {

    @Mock
    private InventoryBalanceRepository inventoryBalanceRepository;

    @Mock
    private InventoryTransactionRecordRepository inventoryTransactionRecordRepository;

    @InjectMocks
    private InventoryStockLockDomainService inventoryStockLockDomainService;

    @InjectMocks
    private InventoryStockUnlockDomainService inventoryStockUnlockDomainService;

    @Test
    void shouldLockSuccessfully() {
        InventoryBalance balance = InventoryBalance.initialize(new InventoryKey(1L, 1001L, 2001L, 3001L), 1L);
        balance.stockIn("IN-INIT-001", "INIT", "INIT-001", new BigDecimal("10"), 1L);
        when(inventoryTransactionRecordRepository.existsLockRecord(1L, "SALES_ORDER", "SO-001", 1001L, 2001L, 3001L))
                .thenReturn(false);
        when(inventoryBalanceRepository.findByKey(any())).thenReturn(Optional.of(balance));

        InventoryTransactionRecord record = inventoryStockLockDomainService.lock(
                1L, "SALES_ORDER", "SO-001", 1L, 1001L, 2001L, 3001L, new BigDecimal("4"));

        assertEquals(InventoryTransactionDirection.LOCK, record.getTxnDirection());
        assertEquals(new BigDecimal("4"), balance.getLockedQty());
        assertEquals(new BigDecimal("6"), balance.getAvailableQty());
    }

    @Test
    void shouldUnlockSuccessfully() {
        InventoryBalance balance = InventoryBalance.initialize(new InventoryKey(1L, 1001L, 2001L, 3001L), 1L);
        balance.stockIn("IN-INIT-001", "INIT", "INIT-001", new BigDecimal("10"), 1L);
        balance.lock("LOCK-INIT-001", "SALES_ORDER", "SO-001", new BigDecimal("4"), 1L);
        when(inventoryTransactionRecordRepository.existsUnlockRecord(1L, "SALES_ORDER", "SO-001", 1001L, 2001L, 3001L))
                .thenReturn(false);
        when(inventoryBalanceRepository.findByKey(any())).thenReturn(Optional.of(balance));

        InventoryTransactionRecord record = inventoryStockUnlockDomainService.unlock(
                1L, "SALES_ORDER", "SO-001", 1L, 1001L, 2001L, 3001L, new BigDecimal("2"));

        assertEquals(InventoryTransactionDirection.UNLOCK, record.getTxnDirection());
        assertEquals(new BigDecimal("2"), balance.getLockedQty());
        assertEquals(new BigDecimal("8"), balance.getAvailableQty());
    }

    @Test
    void shouldRejectWhenUnlockQuantityExceedsLockedQty() {
        InventoryBalance balance = InventoryBalance.initialize(new InventoryKey(1L, 1001L, 2001L, 3001L), 1L);
        balance.stockIn("IN-INIT-001", "INIT", "INIT-001", new BigDecimal("10"), 1L);
        when(inventoryTransactionRecordRepository.existsUnlockRecord(1L, "SALES_ORDER", "SO-001", 1001L, 2001L, 3001L))
                .thenReturn(false);
        when(inventoryBalanceRepository.findByKey(any())).thenReturn(Optional.of(balance));

        assertThrows(BusinessException.class, () -> inventoryStockUnlockDomainService.unlock(
                1L, "SALES_ORDER", "SO-001", 1L, 1001L, 2001L, 3001L, new BigDecimal("1")));
        verify(inventoryTransactionRecordRepository, never()).save(any());
    }
}
