package com.example.scm.inventory;

import com.example.scm.common.core.BusinessException;
import com.example.scm.inventory.domain.inventory.aggregate.InventoryBalance;
import com.example.scm.inventory.domain.inventory.valueobject.InventoryKey;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryBalanceAggregateTest {

    @Test
    void shouldExecuteInventoryMutationsSuccessfully() {
        InventoryBalance balance = InventoryBalance.initialize(new InventoryKey(1L, 1001L, 2001L, 3001L), 99L);

        balance.stockIn("IN-001", "INIT", "INIT-001", new BigDecimal("10"), 99L);
        balance.lock("LOCK-001", "SALES_ORDER", "SO-001", new BigDecimal("4"), 99L);
        balance.unlock("UNLOCK-001", "SALES_ORDER", "SO-001", new BigDecimal("1"), 99L);
        balance.adjustIn("ADJIN-001", "MANUAL_ADJUST", "ADJ-001", new BigDecimal("2"), 99L);
        balance.adjustOut("ADJOUT-001", "MANUAL_ADJUST", "ADJ-002", new BigDecimal("1"), 99L);
        balance.moveOut("MOVEOUT-001", "TRANSFER", "TRF-001", new BigDecimal("2"), 99L);
        balance.moveIn("MOVEIN-001", "TRANSFER", "TRF-001", new BigDecimal("5"), 99L);
        balance.lockedStockOut("LOCKED-OUT-001", "SALES_ORDER", "SO-001", new BigDecimal("3"), 99L);

        assertEquals(new BigDecimal("11"), balance.getOnHandQty());
        assertEquals(BigDecimal.ZERO, balance.getLockedQty());
        assertEquals(new BigDecimal("11"), balance.getAvailableQty());
        assertEquals(8L, balance.getVersion());
    }

    @Test
    void shouldRejectInvalidInventoryMutations() {
        InventoryBalance balance = InventoryBalance.initialize(new InventoryKey(1L, 1001L, 2001L, 3001L), 99L);
        balance.stockIn("IN-001", "INIT", "INIT-001", new BigDecimal("2"), 99L);

        assertThrows(BusinessException.class, () -> balance.stockIn("IN-ERR", "INIT", "INIT-002", BigDecimal.ZERO, 99L));
        assertThrows(BusinessException.class, () -> balance.stockOut("OUT-ERR", "SO", "SO-001", new BigDecimal("3"), 99L));
        assertThrows(BusinessException.class, () -> balance.lock("LOCK-ERR", "SO", "SO-001", new BigDecimal("3"), 99L));
        assertThrows(BusinessException.class, () -> balance.adjustOut("ADJ-ERR", "ADJ", "ADJ-001", new BigDecimal("3"), 99L));
        assertThrows(BusinessException.class, () -> balance.moveOut("MOVE-ERR", "TRF", "TRF-001", new BigDecimal("3"), 99L));
        assertThrows(BusinessException.class, () -> balance.unlock("UNLOCK-ERR", "SO", "SO-001", new BigDecimal("1"), 99L));
        assertThrows(BusinessException.class, () -> balance.lockedStockOut("LOCKED-ERR", "SO", "SO-001", new BigDecimal("1"), 99L));
    }
}
