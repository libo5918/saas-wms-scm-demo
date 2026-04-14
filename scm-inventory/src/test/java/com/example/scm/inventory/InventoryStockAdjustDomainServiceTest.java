package com.example.scm.inventory;

import com.example.scm.common.core.BusinessException;
import com.example.scm.inventory.domain.inventory.aggregate.InventoryBalance;
import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;
import com.example.scm.inventory.domain.inventory.repository.InventoryBalanceRepository;
import com.example.scm.inventory.domain.inventory.repository.InventoryTransactionRecordRepository;
import com.example.scm.inventory.domain.inventory.service.InventoryStockAdjustDomainService;
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
class InventoryStockAdjustDomainServiceTest {

    @Mock
    private InventoryBalanceRepository inventoryBalanceRepository;

    @Mock
    private InventoryTransactionRecordRepository inventoryTransactionRecordRepository;

    @InjectMocks
    private InventoryStockAdjustDomainService inventoryStockAdjustDomainService;

    @Test
    void shouldAdjustIncreaseSuccessfully() {
        InventoryBalance balance = InventoryBalance.initialize(new InventoryKey(1L, 1001L, 2001L, 3001L), 1L);
        balance.stockIn("IN-INIT-001", "INIT", "INIT-001", new BigDecimal("10"), 1L);

        when(inventoryTransactionRecordRepository.existsAdjustInRecord(1L, "MANUAL_ADJUST", "ADJ-001", 1001L, 2001L, 3001L))
                .thenReturn(false);
        when(inventoryBalanceRepository.findByKey(any())).thenReturn(Optional.of(balance));

        InventoryTransactionRecord record = inventoryStockAdjustDomainService.adjust(
                1L, "MANUAL_ADJUST", "ADJ-001", "INCREASE", 1L, 1001L, 2001L, 3001L, new BigDecimal("3")
        );

        assertEquals(InventoryTransactionDirection.ADJUST_IN, record.getTxnDirection());
        assertEquals(new BigDecimal("13"), balance.getOnHandQty());
        assertEquals(new BigDecimal("13"), balance.getAvailableQty());
        verify(inventoryBalanceRepository).save(balance);
        verify(inventoryTransactionRecordRepository).save(any());
    }

    @Test
    void shouldRejectAdjustDecreaseWhenInventoryIsInsufficient() {
        InventoryBalance balance = InventoryBalance.initialize(new InventoryKey(1L, 1001L, 2001L, 3001L), 1L);
        balance.stockIn("IN-INIT-001", "INIT", "INIT-001", new BigDecimal("2"), 1L);

        when(inventoryTransactionRecordRepository.existsAdjustOutRecord(1L, "MANUAL_ADJUST", "ADJ-002", 1001L, 2001L, 3001L))
                .thenReturn(false);
        when(inventoryBalanceRepository.findByKey(any())).thenReturn(Optional.of(balance));

        assertThrows(BusinessException.class, () -> inventoryStockAdjustDomainService.adjust(
                1L, "MANUAL_ADJUST", "ADJ-002", "DECREASE", 1L, 1001L, 2001L, 3001L, new BigDecimal("5")
        ));

        verify(inventoryBalanceRepository, never()).save(any());
        verify(inventoryTransactionRecordRepository, never()).save(any());
    }
}
