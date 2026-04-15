package com.example.scm.purchase;

import com.example.scm.common.core.TenantContext;
import com.example.scm.common.core.BusinessException;
import com.example.scm.purchase.client.MaterialClient;
import com.example.scm.purchase.client.SupplierClient;
import com.example.scm.purchase.dto.CreatePurchaseOrderItemRequest;
import com.example.scm.purchase.dto.CreatePurchaseOrderRequest;
import com.example.scm.purchase.entity.PurchaseOrder;
import com.example.scm.purchase.entity.PurchaseOrderItem;
import com.example.scm.purchase.mapper.PurchaseOrderItemMapper;
import com.example.scm.purchase.mapper.PurchaseOrderMapper;
import com.example.scm.purchase.service.impl.PurchaseOrderServiceImpl;
import com.example.scm.purchase.support.PurchaseOrderAssembler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PurchaseOrderServiceImplTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreatePurchaseOrder() {
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        PurchaseOrderItemMapper itemMapper = mock(PurchaseOrderItemMapper.class);
        SupplierClient supplierClient = mock(SupplierClient.class);
        MaterialClient materialClient = mock(MaterialClient.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        PurchaseOrderServiceImpl service = new PurchaseOrderServiceImpl(
                orderMapper,
                itemMapper,
                new PurchaseOrderAssembler(),
                supplierClient,
                materialClient,
                transactionTemplate
        );

        TenantContext.setTenantId(1L);
        CreatePurchaseOrderRequest request = buildRequest();

        when(orderMapper.selectByOrderNo(1L, "PO-1001")).thenReturn(Optional.empty());
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> invocation.<org.springframework.transaction.support.TransactionCallback<PurchaseOrder>>getArgument(0).doInTransaction(null));
        when(orderMapper.selectById(1L, 1L)).thenReturn(Optional.of(buildOrder(1L)));
        when(itemMapper.selectByOrderId(1L, 1L)).thenReturn(List.of(buildItem(11L)));
        org.mockito.Mockito.doAnswer(invocation -> {
            PurchaseOrder order = invocation.getArgument(0);
            order.setId(1L);
            return null;
        }).when(orderMapper).insert(any(PurchaseOrder.class));

        var result = service.create(request);

        assertEquals("PO-1001", result.getOrderNo());
        assertEquals(1L, result.getSupplierId());
        assertEquals("CREATED", result.getOrderStatus());
        assertEquals(1, result.getItems().size());
    }

    @Test
    void shouldCancelCreatedPurchaseOrder() {
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        PurchaseOrderItemMapper itemMapper = mock(PurchaseOrderItemMapper.class);
        SupplierClient supplierClient = mock(SupplierClient.class);
        MaterialClient materialClient = mock(MaterialClient.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        PurchaseOrderServiceImpl service = new PurchaseOrderServiceImpl(
                orderMapper,
                itemMapper,
                new PurchaseOrderAssembler(),
                supplierClient,
                materialClient,
                transactionTemplate
        );

        TenantContext.setTenantId(1L);
        when(orderMapper.selectById(1L, 1L))
                .thenReturn(Optional.of(buildOrder(1L)))
                .thenReturn(Optional.of(buildCancelledOrder(1L)));
        when(orderMapper.updateStatus(1L, 1L, "CANCELLED", 1L)).thenReturn(1);
        when(itemMapper.selectByOrderId(1L, 1L)).thenReturn(List.of(buildItem(11L)));

        var result = service.cancel(1L);

        assertEquals("CANCELLED", result.getOrderStatus());
        verify(orderMapper).updateStatus(1L, 1L, "CANCELLED", 1L);
    }

    @Test
    void shouldRejectCancelWhenOrderPartiallyReceived() {
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        PurchaseOrderItemMapper itemMapper = mock(PurchaseOrderItemMapper.class);
        SupplierClient supplierClient = mock(SupplierClient.class);
        MaterialClient materialClient = mock(MaterialClient.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        PurchaseOrderServiceImpl service = new PurchaseOrderServiceImpl(
                orderMapper,
                itemMapper,
                new PurchaseOrderAssembler(),
                supplierClient,
                materialClient,
                transactionTemplate
        );

        TenantContext.setTenantId(1L);
        when(orderMapper.selectById(1L, 1L)).thenReturn(Optional.of(buildPartiallyReceivedOrder(1L)));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.cancel(1L));

        assertEquals("Only created purchase order can be cancelled", exception.getMessage());
    }

    private CreatePurchaseOrderRequest buildRequest() {
        CreatePurchaseOrderItemRequest itemRequest = new CreatePurchaseOrderItemRequest();
        itemRequest.setMaterialId(1L);
        itemRequest.setPlanQty(new BigDecimal("10"));
        itemRequest.setUnitPrice(new BigDecimal("12"));

        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest();
        request.setOrderNo("PO-1001");
        request.setSupplierId(1L);
        request.setRemark("测试采购订单");
        request.setItems(List.of(itemRequest));
        return request;
    }

    private PurchaseOrder buildOrder(long id) {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(id);
        order.setTenantId(1L);
        order.setOrderNo("PO-1001");
        order.setSupplierId(1L);
        order.setOrderStatus("CREATED");
        order.setTotalAmount(new BigDecimal("120"));
        order.setRemark("测试采购订单");
        return order;
    }

    private PurchaseOrder buildCancelledOrder(long id) {
        PurchaseOrder order = buildOrder(id);
        order.setOrderStatus("CANCELLED");
        return order;
    }

    private PurchaseOrder buildPartiallyReceivedOrder(long id) {
        PurchaseOrder order = buildOrder(id);
        order.setOrderStatus("PARTIALLY_RECEIVED");
        return order;
    }

    private PurchaseOrderItem buildItem(long id) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setId(id);
        item.setTenantId(1L);
        item.setPurchaseOrderId(1L);
        item.setMaterialId(1L);
        item.setPlanQty(new BigDecimal("10"));
        item.setReceivedQty(BigDecimal.ZERO);
        item.setUnitPrice(new BigDecimal("12"));
        return item;
    }
}
