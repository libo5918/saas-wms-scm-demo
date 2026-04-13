package com.example.scm.purchase;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.TenantContext;
import com.example.scm.purchase.client.InventoryStockInClient;
import com.example.scm.purchase.dto.CreatePurchaseReceiptItemRequest;
import com.example.scm.purchase.dto.CreatePurchaseReceiptRequest;
import com.example.scm.purchase.entity.PurchaseReceipt;
import com.example.scm.purchase.entity.PurchaseReceiptItem;
import com.example.scm.purchase.mapper.PurchaseReceiptItemMapper;
import com.example.scm.purchase.mapper.PurchaseReceiptMapper;
import com.example.scm.purchase.service.impl.PurchaseReceiptServiceImpl;
import com.example.scm.purchase.support.PurchaseReceiptAssembler;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseReceiptServiceImplTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldUpdateReceiptToSuccessAfterInventoryStockIn() {
        PurchaseReceiptMapper receiptMapper = mock(PurchaseReceiptMapper.class);
        PurchaseReceiptItemMapper itemMapper = mock(PurchaseReceiptItemMapper.class);
        InventoryStockInClient inventoryClient = mock(InventoryStockInClient.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        PurchaseReceiptServiceImpl service = new PurchaseReceiptServiceImpl(
                receiptMapper,
                itemMapper,
                new PurchaseReceiptAssembler(),
                inventoryClient,
                transactionTemplate
        );

        TenantContext.setTenantId(1L);
        CreatePurchaseReceiptRequest request = buildRequest("RCV-1001");

        when(receiptMapper.selectByReceiptNo(1L, "RCV-1001")).thenReturn(Optional.empty());
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class).doInTransaction(null));
        when(itemMapper.selectByReceiptId(1L, 1L)).thenReturn(List.of(buildItem(1L, 1L, 3001L, "8")));
        when(receiptMapper.updateStatus(1L, 1L, "STOCK_IN_SUCCESS", null, 1L)).thenReturn(1);

        ArgumentCaptor<PurchaseReceipt> receiptCaptor = ArgumentCaptor.forClass(PurchaseReceipt.class);
        when(receiptMapper.selectById(1L, 1L)).thenReturn(Optional.of(buildReceipt(1L, "RCV-1001", "STOCK_IN_SUCCESS", null)));
        org.mockito.Mockito.doAnswer(invocation -> {
            PurchaseReceipt receipt = invocation.getArgument(0);
            receipt.setId(1L);
            return null;
        }).when(receiptMapper).insert(receiptCaptor.capture());

        service.create(request);

        verify(inventoryClient, times(1)).stockIn(eq(1L), eq(1L), any(PurchaseReceipt.class), any(List.class));
        verify(receiptMapper).updateStatus(1L, 1L, "STOCK_IN_SUCCESS", null, 1L);
        assertEquals("CREATED", receiptCaptor.getValue().getReceiptStatus());
    }

    @Test
    void shouldUpdateReceiptToFailedWhenInventoryStockInFails() {
        PurchaseReceiptMapper receiptMapper = mock(PurchaseReceiptMapper.class);
        PurchaseReceiptItemMapper itemMapper = mock(PurchaseReceiptItemMapper.class);
        InventoryStockInClient inventoryClient = mock(InventoryStockInClient.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        PurchaseReceiptServiceImpl service = new PurchaseReceiptServiceImpl(
                receiptMapper,
                itemMapper,
                new PurchaseReceiptAssembler(),
                inventoryClient,
                transactionTemplate
        );

        TenantContext.setTenantId(1L);
        CreatePurchaseReceiptRequest request = buildRequest("RCV-1002");

        when(receiptMapper.selectByReceiptNo(1L, "RCV-1002")).thenReturn(Optional.empty());
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class).doInTransaction(null));
        when(itemMapper.selectByReceiptId(1L, 2L)).thenReturn(List.of(buildItem(11L, 2L, 3001L, "5")));
        when(receiptMapper.updateStatus(1L, 2L, "STOCK_IN_FAILED", "Inventory unavailable", 1L)).thenReturn(1);
        org.mockito.Mockito.doAnswer(invocation -> {
            PurchaseReceipt receipt = invocation.getArgument(0);
            receipt.setId(2L);
            return null;
        }).when(receiptMapper).insert(any(PurchaseReceipt.class));
        doThrow(new BusinessException("500", "Inventory unavailable"))
                .when(inventoryClient)
                .stockIn(eq(1L), eq(1L), any(PurchaseReceipt.class), any(List.class));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals("Inventory unavailable", exception.getMessage());
        verify(receiptMapper).updateStatus(1L, 2L, "STOCK_IN_FAILED", "Inventory unavailable", 1L);
    }

    private CreatePurchaseReceiptRequest buildRequest(String receiptNo) {
        CreatePurchaseReceiptItemRequest itemRequest = new CreatePurchaseReceiptItemRequest();
        itemRequest.setMaterialId(1L);
        itemRequest.setLocationId(3001L);
        itemRequest.setReceiptQty(new BigDecimal("8"));

        CreatePurchaseReceiptRequest request = new CreatePurchaseReceiptRequest();
        request.setReceiptNo(receiptNo);
        request.setPurchaseOrderId(5001L);
        request.setWarehouseId(2001L);
        request.setItems(List.of(itemRequest));
        return request;
    }

    private PurchaseReceipt buildReceipt(Long id, String receiptNo, String receiptStatus, String failureReason) {
        PurchaseReceipt receipt = new PurchaseReceipt();
        receipt.setId(id);
        receipt.setTenantId(1L);
        receipt.setReceiptNo(receiptNo);
        receipt.setPurchaseOrderId(5001L);
        receipt.setWarehouseId(2001L);
        receipt.setReceiptStatus(receiptStatus);
        receipt.setFailureReason(failureReason);
        return receipt;
    }

    private PurchaseReceiptItem buildItem(Long id, Long receiptId, Long locationId, String qty) {
        PurchaseReceiptItem item = new PurchaseReceiptItem();
        item.setId(id);
        item.setTenantId(1L);
        item.setPurchaseReceiptId(receiptId);
        item.setMaterialId(1L);
        item.setLocationId(locationId);
        item.setReceiptQty(new BigDecimal(qty));
        return item;
    }
}
