package com.example.scm.inventory;

import com.example.scm.common.core.BusinessException;
import com.example.scm.inventory.domain.inventory.aggregate.InventoryBalance;
import com.example.scm.inventory.domain.inventory.entity.InventoryTransactionRecord;
import com.example.scm.inventory.domain.inventory.repository.InventoryBalanceRepository;
import com.example.scm.inventory.domain.inventory.repository.InventoryTransactionRecordRepository;
import com.example.scm.inventory.domain.inventory.service.InventoryStockOutDomainService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryStockOutDomainServiceTest {

    @Mock
    private InventoryBalanceRepository inventoryBalanceRepository;

    @Mock
    private InventoryTransactionRecordRepository inventoryTransactionRecordRepository;

    @InjectMocks
    private InventoryStockOutDomainService inventoryStockOutDomainService;

    @Test
    void shouldStockOutSuccessfully() {
        InventoryBalance balance = InventoryBalance.initialize(new InventoryKey(1L, 1001L, 2001L, 3001L), 1L);
        balance.stockIn("IN-INIT-001", "INIT", "INIT-001", new BigDecimal("10"), 1L);

        when(inventoryTransactionRecordRepository.existsStockOutRecord(1L, "SALES_ORDER", "SO-001", 1001L, 2001L, 3001L))
                .thenReturn(false);
        when(inventoryBalanceRepository.findByKey(any())).thenReturn(Optional.of(balance));

        InventoryTransactionRecord record = inventoryStockOutDomainService.stockOut(
                1L, "SALES_ORDER", "SO-001", 1L, 1001L, 2001L, 3001L, new BigDecimal("4")
        );

        assertEquals(InventoryTransactionDirection.OUT, record.getTxnDirection());
        assertEquals(new BigDecimal("4"), record.getTxnQty());
        assertEquals(new BigDecimal("10"), record.getBeforeQty());
        assertEquals(new BigDecimal("6"), record.getAfterQty());
        assertEquals(new BigDecimal("6"), balance.getAvailableQty());

        verify(inventoryBalanceRepository).save(balance);
        ArgumentCaptor<InventoryTransactionRecord> recordCaptor = ArgumentCaptor.forClass(InventoryTransactionRecord.class);
        verify(inventoryTransactionRecordRepository).save(recordCaptor.capture());
        assertEquals(InventoryTransactionDirection.OUT, recordCaptor.getValue().getTxnDirection());
    }

    @Test
    void shouldRejectWhenInventoryIsInsufficient() {
        InventoryBalance balance = InventoryBalance.initialize(new InventoryKey(1L, 1001L, 2001L, 3001L), 1L);
        balance.stockIn("IN-INIT-001", "INIT", "INIT-001", new BigDecimal("3"), 1L);

        when(inventoryTransactionRecordRepository.existsStockOutRecord(1L, "SALES_ORDER", "SO-001", 1001L, 2001L, 3001L))
                .thenReturn(false);
        when(inventoryBalanceRepository.findByKey(any())).thenReturn(Optional.of(balance));

        assertThrows(BusinessException.class, () -> inventoryStockOutDomainService.stockOut(
                1L, "SALES_ORDER", "SO-001", 1L, 1001L, 2001L, 3001L, new BigDecimal("5")
        ));

        verify(inventoryBalanceRepository, never()).save(any());
        verify(inventoryTransactionRecordRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateStockOutRequest() {
        when(inventoryTransactionRecordRepository.existsStockOutRecord(1L, "SALES_ORDER", "SO-001", 1001L, 2001L, 3001L))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> inventoryStockOutDomainService.stockOut(
                1L, "SALES_ORDER", "SO-001", 1L, 1001L, 2001L, 3001L, new BigDecimal("1")
        ));

        verify(inventoryBalanceRepository, never()).findByKey(any());
        verify(inventoryTransactionRecordRepository, never()).save(any());
    }
}
