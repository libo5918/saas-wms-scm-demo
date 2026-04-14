package com.example.scm.purchase;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.TenantContext;
import com.example.scm.purchase.client.InventoryStockInClient;
import com.example.scm.purchase.client.MaterialClient;
import com.example.scm.purchase.dto.CreatePurchaseReceiptItemRequest;
import com.example.scm.purchase.dto.CreatePurchaseReceiptRequest;
import com.example.scm.purchase.entity.PurchaseReceipt;
import com.example.scm.purchase.entity.PurchaseReceiptItem;
import com.example.scm.purchase.entity.PurchaseReceiptStatus;
import com.example.scm.purchase.mapper.PurchaseReceiptItemMapper;
import com.example.scm.purchase.mapper.PurchaseReceiptMapper;
import com.example.scm.purchase.service.impl.PurchaseReceiptServiceImpl;
import com.example.scm.purchase.support.PurchaseReceiptAssembler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
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
        MaterialClient materialClient = mock(MaterialClient.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        PurchaseReceiptServiceImpl service = new PurchaseReceiptServiceImpl(
                receiptMapper,
                itemMapper,
                new PurchaseReceiptAssembler(),
                materialClient,
                inventoryClient,
                transactionTemplate
        );

        TenantContext.setTenantId(1L);
        CreatePurchaseReceiptRequest request = buildRequest("RCV-1001");

        when(receiptMapper.selectByReceiptNo(1L, "RCV-1001")).thenReturn(Optional.empty());
        when(transactionTemplate.execute(any())).thenAnswer(executeTransactionCallback());
        when(itemMapper.selectByReceiptId(1L, 1L)).thenReturn(List.of(buildItem(1L, 1L, 3001L, "8")));
        when(receiptMapper.updateStatus(1L, 1L, PurchaseReceiptStatus.STOCK_IN_SUCCESS.name(), null, 1L)).thenReturn(1);

        ArgumentCaptor<PurchaseReceipt> receiptCaptor = ArgumentCaptor.forClass(PurchaseReceipt.class);
        when(receiptMapper.selectById(1L, 1L)).thenReturn(Optional.of(buildReceipt(1L, "RCV-1001", PurchaseReceiptStatus.STOCK_IN_SUCCESS.name(), null)));
        org.mockito.Mockito.doAnswer(invocation -> {
            PurchaseReceipt receipt = invocation.getArgument(0);
            receipt.setId(1L);
            return null;
        }).when(receiptMapper).insert(receiptCaptor.capture());

        service.create(request);

        verify(inventoryClient, times(1)).stockIn(eq(1L), eq(1L), any(PurchaseReceipt.class), any(List.class));
        verify(receiptMapper).updateStatus(1L, 1L, PurchaseReceiptStatus.STOCK_IN_SUCCESS.name(), null, 1L);
        assertEquals(PurchaseReceiptStatus.CREATED.name(), receiptCaptor.getValue().getReceiptStatus());
    }

    @Test
    void shouldUpdateReceiptToFailedWhenInventoryStockInFails() {
        PurchaseReceiptMapper receiptMapper = mock(PurchaseReceiptMapper.class);
        PurchaseReceiptItemMapper itemMapper = mock(PurchaseReceiptItemMapper.class);
        InventoryStockInClient inventoryClient = mock(InventoryStockInClient.class);
        MaterialClient materialClient = mock(MaterialClient.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        PurchaseReceiptServiceImpl service = new PurchaseReceiptServiceImpl(
                receiptMapper,
                itemMapper,
                new PurchaseReceiptAssembler(),
                materialClient,
                inventoryClient,
                transactionTemplate
        );

        TenantContext.setTenantId(1L);
        CreatePurchaseReceiptRequest request = buildRequest("RCV-1002");

        when(receiptMapper.selectByReceiptNo(1L, "RCV-1002")).thenReturn(Optional.empty());
        when(transactionTemplate.execute(any())).thenAnswer(executeTransactionCallback());
        when(itemMapper.selectByReceiptId(1L, 2L)).thenReturn(List.of(buildItem(11L, 2L, 3001L, "5")));
        when(receiptMapper.updateStatus(1L, 2L, PurchaseReceiptStatus.STOCK_IN_FAILED.name(), "Inventory unavailable", 1L)).thenReturn(1);
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
        verify(receiptMapper).updateStatus(1L, 2L, PurchaseReceiptStatus.STOCK_IN_FAILED.name(), "Inventory unavailable", 1L);
    }

    @Test
    void shouldRetryFailedReceiptToSuccess() {
        PurchaseReceiptMapper receiptMapper = mock(PurchaseReceiptMapper.class);
        PurchaseReceiptItemMapper itemMapper = mock(PurchaseReceiptItemMapper.class);
        InventoryStockInClient inventoryClient = mock(InventoryStockInClient.class);
        MaterialClient materialClient = mock(MaterialClient.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        PurchaseReceiptServiceImpl service = new PurchaseReceiptServiceImpl(
                receiptMapper,
                itemMapper,
                new PurchaseReceiptAssembler(),
                materialClient,
                inventoryClient,
                transactionTemplate
        );

        TenantContext.setTenantId(1L);

        PurchaseReceipt failedReceipt = buildReceipt(3L, "RCV-1003", PurchaseReceiptStatus.STOCK_IN_FAILED.name(), "Inventory unavailable");
        when(receiptMapper.selectById(1L, 3L))
                .thenReturn(Optional.of(failedReceipt))
                .thenReturn(Optional.of(buildReceipt(3L, "RCV-1003", PurchaseReceiptStatus.STOCK_IN_SUCCESS.name(), null)));
        when(itemMapper.selectByReceiptId(1L, 3L)).thenReturn(List.of(buildItem(31L, 3L, 3001L, "6")));
        when(transactionTemplate.execute(any())).thenAnswer(executeTransactionCallback());
        when(receiptMapper.updateStatus(1L, 3L, PurchaseReceiptStatus.STOCK_IN_SUCCESS.name(), null, 1L)).thenReturn(1);

        service.retryStockIn(3L);

        verify(inventoryClient).stockIn(eq(1L), eq(1L), any(PurchaseReceipt.class), any(List.class));
        verify(receiptMapper).updateStatus(1L, 3L, PurchaseReceiptStatus.STOCK_IN_SUCCESS.name(), null, 1L);
    }

    @Test
    void shouldKeepFailedStatusWhenRetryStockInFails() {
        PurchaseReceiptMapper receiptMapper = mock(PurchaseReceiptMapper.class);
        PurchaseReceiptItemMapper itemMapper = mock(PurchaseReceiptItemMapper.class);
        InventoryStockInClient inventoryClient = mock(InventoryStockInClient.class);
        MaterialClient materialClient = mock(MaterialClient.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        PurchaseReceiptServiceImpl service = new PurchaseReceiptServiceImpl(
                receiptMapper,
                itemMapper,
                new PurchaseReceiptAssembler(),
                materialClient,
                inventoryClient,
                transactionTemplate
        );

        TenantContext.setTenantId(1L);

        when(receiptMapper.selectById(1L, 4L)).thenReturn(Optional.of(buildReceipt(4L, "RCV-1004", PurchaseReceiptStatus.STOCK_IN_FAILED.name(), "Old reason")));
        when(itemMapper.selectByReceiptId(1L, 4L)).thenReturn(List.of(buildItem(41L, 4L, 3001L, "7")));
        when(transactionTemplate.execute(any())).thenAnswer(executeTransactionCallback());
        when(receiptMapper.updateStatus(1L, 4L, PurchaseReceiptStatus.STOCK_IN_FAILED.name(), "Inventory unavailable", 1L)).thenReturn(1);
        doThrow(new BusinessException("500", "Inventory unavailable"))
                .when(inventoryClient)
                .stockIn(eq(1L), eq(1L), any(PurchaseReceipt.class), any(List.class));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.retryStockIn(4L));

        assertEquals("Inventory unavailable", exception.getMessage());
        verify(receiptMapper).updateStatus(1L, 4L, PurchaseReceiptStatus.STOCK_IN_FAILED.name(), "Inventory unavailable", 1L);
    }

    @Test
    void shouldSkipRetryForSuccessfulReceipt() {
        PurchaseReceiptMapper receiptMapper = mock(PurchaseReceiptMapper.class);
        PurchaseReceiptItemMapper itemMapper = mock(PurchaseReceiptItemMapper.class);
        InventoryStockInClient inventoryClient = mock(InventoryStockInClient.class);
        MaterialClient materialClient = mock(MaterialClient.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        PurchaseReceiptServiceImpl service = new PurchaseReceiptServiceImpl(
                receiptMapper,
                itemMapper,
                new PurchaseReceiptAssembler(),
                materialClient,
                inventoryClient,
                transactionTemplate
        );

        TenantContext.setTenantId(1L);

        when(receiptMapper.selectById(1L, 5L)).thenReturn(Optional.of(buildReceipt(5L, "RCV-1005", PurchaseReceiptStatus.STOCK_IN_SUCCESS.name(), null)));
        when(itemMapper.selectByReceiptId(1L, 5L)).thenReturn(List.of(buildItem(51L, 5L, 3001L, "4")));

        service.retryStockIn(5L);

        verify(inventoryClient, never()).stockIn(eq(1L), eq(1L), any(PurchaseReceipt.class), any(List.class));
        verify(receiptMapper, never()).updateStatus(eq(1L), eq(5L), any(), any(), eq(1L));
    }

    @Test
    void shouldCancelFailedReceipt() {
        PurchaseReceiptMapper receiptMapper = mock(PurchaseReceiptMapper.class);
        PurchaseReceiptItemMapper itemMapper = mock(PurchaseReceiptItemMapper.class);
        InventoryStockInClient inventoryClient = mock(InventoryStockInClient.class);
        MaterialClient materialClient = mock(MaterialClient.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        PurchaseReceiptServiceImpl service = new PurchaseReceiptServiceImpl(
                receiptMapper,
                itemMapper,
                new PurchaseReceiptAssembler(),
                materialClient,
                inventoryClient,
                transactionTemplate
        );

        TenantContext.setTenantId(1L);

        when(receiptMapper.selectById(1L, 6L))
                .thenReturn(Optional.of(buildReceipt(6L, "RCV-1006", PurchaseReceiptStatus.STOCK_IN_FAILED.name(), "Inventory unavailable")))
                .thenReturn(Optional.of(buildReceipt(6L, "RCV-1006", PurchaseReceiptStatus.CANCELLED.name(), null)));
        when(itemMapper.selectByReceiptId(1L, 6L)).thenReturn(List.of(buildItem(61L, 6L, 3001L, "3")));
        when(transactionTemplate.execute(any())).thenAnswer(executeTransactionCallback());
        when(receiptMapper.updateStatus(1L, 6L, PurchaseReceiptStatus.CANCELLED.name(), null, 1L)).thenReturn(1);

        service.cancel(6L);

        verify(receiptMapper).updateStatus(1L, 6L, PurchaseReceiptStatus.CANCELLED.name(), null, 1L);
        verify(inventoryClient, never()).stockIn(eq(1L), eq(1L), any(PurchaseReceipt.class), any(List.class));
    }

    @Test
    void shouldRejectCancelForSuccessfulReceipt() {
        PurchaseReceiptMapper receiptMapper = mock(PurchaseReceiptMapper.class);
        PurchaseReceiptItemMapper itemMapper = mock(PurchaseReceiptItemMapper.class);
        InventoryStockInClient inventoryClient = mock(InventoryStockInClient.class);
        MaterialClient materialClient = mock(MaterialClient.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        PurchaseReceiptServiceImpl service = new PurchaseReceiptServiceImpl(
                receiptMapper,
                itemMapper,
                new PurchaseReceiptAssembler(),
                materialClient,
                inventoryClient,
                transactionTemplate
        );

        TenantContext.setTenantId(1L);

        when(receiptMapper.selectById(1L, 7L)).thenReturn(Optional.of(buildReceipt(7L, "RCV-1007", PurchaseReceiptStatus.STOCK_IN_SUCCESS.name(), null)));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.cancel(7L));

        assertEquals("Only pending or failed purchase receipt can be cancelled", exception.getMessage());
        verify(receiptMapper, never()).updateStatus(eq(1L), eq(7L), any(), any(), eq(1L));
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

    private <T> Answer<T> executeTransactionCallback() {
        return invocation -> invocation.<org.springframework.transaction.support.TransactionCallback<T>>getArgument(0)
                .doInTransaction(null);
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
