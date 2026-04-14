package com.example.scm.sales;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.TenantContext;
import com.example.scm.sales.client.InventoryReservationClient;
import com.example.scm.sales.entity.SalesOrder;
import com.example.scm.sales.entity.SalesOrderItem;
import com.example.scm.sales.entity.SalesOrderStatus;
import com.example.scm.sales.mapper.SalesOrderItemMapper;
import com.example.scm.sales.mapper.SalesOrderMapper;
import com.example.scm.sales.service.impl.SalesOrderServiceImpl;
import com.example.scm.sales.support.SalesOrderAssembler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SalesOrderServiceImplTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldRetryFailedLockToSuccess() {
        SalesOrderMapper orderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper itemMapper = mock(SalesOrderItemMapper.class);
        InventoryReservationClient inventoryClient = mock(InventoryReservationClient.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        SalesOrderServiceImpl service = new SalesOrderServiceImpl(
                orderMapper,
                itemMapper,
                new SalesOrderAssembler(),
                inventoryClient,
                transactionTemplate
        );

        TenantContext.setTenantId(1L);

        when(orderMapper.selectById(1L, 1L))
                .thenReturn(Optional.of(buildOrder(1L, "SO-1001", SalesOrderStatus.LOCK_FAILED.name(), "Inventory unavailable")))
                .thenReturn(Optional.of(buildOrder(1L, "SO-1001", SalesOrderStatus.LOCK_SUCCESS.name(), null)));
        when(itemMapper.selectByOrderId(1L, 1L)).thenReturn(List.of(buildItem(11L, 1L, "2")));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class).doInTransaction(null));
        when(orderMapper.updateStatus(1L, 1L, SalesOrderStatus.LOCK_SUCCESS.name(), null, 1L)).thenReturn(1);

        service.retryLock(1L);

        verify(inventoryClient, times(1)).lock(eq(1L), eq(1L), any(SalesOrder.class), any(List.class));
        verify(orderMapper).updateStatus(1L, 1L, SalesOrderStatus.LOCK_SUCCESS.name(), null, 1L);
    }

    @Test
    void shouldUpdateOrderToShipFailedWhenShipFails() {
        SalesOrderMapper orderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper itemMapper = mock(SalesOrderItemMapper.class);
        InventoryReservationClient inventoryClient = mock(InventoryReservationClient.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        SalesOrderServiceImpl service = new SalesOrderServiceImpl(
                orderMapper,
                itemMapper,
                new SalesOrderAssembler(),
                inventoryClient,
                transactionTemplate
        );

        TenantContext.setTenantId(1L);

        when(orderMapper.selectById(1L, 2L)).thenReturn(Optional.of(buildOrder(2L, "SO-1002", SalesOrderStatus.LOCK_SUCCESS.name(), null)));
        when(itemMapper.selectByOrderId(1L, 2L)).thenReturn(List.of(buildItem(21L, 2L, "3")));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class).doInTransaction(null));
        when(orderMapper.updateStatus(1L, 2L, SalesOrderStatus.SHIP_FAILED.name(), "Inventory unavailable", 1L)).thenReturn(1);
        doThrow(new BusinessException("500", "Inventory unavailable"))
                .when(inventoryClient)
                .stockOut(eq(1L), eq(1L), any(SalesOrder.class), any(List.class));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.ship(2L));

        assertEquals("Inventory unavailable", exception.getMessage());
        verify(orderMapper).updateStatus(1L, 2L, SalesOrderStatus.SHIP_FAILED.name(), "Inventory unavailable", 1L);
    }

    @Test
    void shouldRetryFailedShipToSuccess() {
        SalesOrderMapper orderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper itemMapper = mock(SalesOrderItemMapper.class);
        InventoryReservationClient inventoryClient = mock(InventoryReservationClient.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        SalesOrderServiceImpl service = new SalesOrderServiceImpl(
                orderMapper,
                itemMapper,
                new SalesOrderAssembler(),
                inventoryClient,
                transactionTemplate
        );

        TenantContext.setTenantId(1L);

        when(orderMapper.selectById(1L, 3L))
                .thenReturn(Optional.of(buildOrder(3L, "SO-1003", SalesOrderStatus.SHIP_FAILED.name(), "Inventory unavailable")))
                .thenReturn(Optional.of(buildOrder(3L, "SO-1003", SalesOrderStatus.SHIPPED.name(), null)));
        when(itemMapper.selectByOrderId(1L, 3L)).thenReturn(List.of(buildItem(31L, 3L, "4")));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class).doInTransaction(null));
        when(orderMapper.updateStatus(1L, 3L, SalesOrderStatus.SHIPPED.name(), null, 1L)).thenReturn(1);

        service.retryShip(3L);

        verify(inventoryClient).stockOut(eq(1L), eq(1L), any(SalesOrder.class), any(List.class));
        verify(orderMapper).updateStatus(1L, 3L, SalesOrderStatus.SHIPPED.name(), null, 1L);
    }

    @Test
    void shouldUnlockInventoryWhenCancelLockedOrder() {
        SalesOrderMapper orderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper itemMapper = mock(SalesOrderItemMapper.class);
        InventoryReservationClient inventoryClient = mock(InventoryReservationClient.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        SalesOrderServiceImpl service = new SalesOrderServiceImpl(
                orderMapper,
                itemMapper,
                new SalesOrderAssembler(),
                inventoryClient,
                transactionTemplate
        );

        TenantContext.setTenantId(1L);

        when(orderMapper.selectById(1L, 4L))
                .thenReturn(Optional.of(buildOrder(4L, "SO-1004", SalesOrderStatus.LOCK_SUCCESS.name(), null)))
                .thenReturn(Optional.of(buildOrder(4L, "SO-1004", SalesOrderStatus.CANCELLED.name(), null)));
        when(itemMapper.selectByOrderId(1L, 4L)).thenReturn(List.of(buildItem(41L, 4L, "5")));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class).doInTransaction(null));
        when(orderMapper.updateStatus(1L, 4L, SalesOrderStatus.CANCELLED.name(), null, 1L)).thenReturn(1);

        service.cancel(4L);

        verify(inventoryClient).unlock(eq(1L), eq(1L), any(SalesOrder.class), any(List.class));
        verify(orderMapper).updateStatus(1L, 4L, SalesOrderStatus.CANCELLED.name(), null, 1L);
    }

    @Test
    void shouldRejectRetryShipForNonFailedOrder() {
        SalesOrderMapper orderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper itemMapper = mock(SalesOrderItemMapper.class);
        InventoryReservationClient inventoryClient = mock(InventoryReservationClient.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        SalesOrderServiceImpl service = new SalesOrderServiceImpl(
                orderMapper,
                itemMapper,
                new SalesOrderAssembler(),
                inventoryClient,
                transactionTemplate
        );

        TenantContext.setTenantId(1L);

        when(orderMapper.selectById(1L, 5L)).thenReturn(Optional.of(buildOrder(5L, "SO-1005", SalesOrderStatus.LOCK_SUCCESS.name(), null)));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.retryShip(5L));

        assertEquals("Only failed sales order can retry ship", exception.getMessage());
        verify(inventoryClient, never()).stockOut(eq(1L), eq(1L), any(SalesOrder.class), any(List.class));
    }

    private SalesOrder buildOrder(Long id, String orderNo, String status, String failureReason) {
        SalesOrder order = new SalesOrder();
        order.setId(id);
        order.setTenantId(1L);
        order.setOrderNo(orderNo);
        order.setWarehouseId(2001L);
        order.setOrderStatus(status);
        order.setFailureReason(failureReason);
        return order;
    }

    private SalesOrderItem buildItem(Long id, Long orderId, String qty) {
        SalesOrderItem item = new SalesOrderItem();
        item.setId(id);
        item.setTenantId(1L);
        item.setSalesOrderId(orderId);
        item.setMaterialId(1L);
        item.setLocationId(3001L);
        item.setSaleQty(new BigDecimal(qty));
        return item;
    }
}
