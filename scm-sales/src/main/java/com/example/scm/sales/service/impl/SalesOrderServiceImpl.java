package com.example.scm.sales.service.impl;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.sales.client.InventoryReservationClient;
import com.example.scm.sales.dto.CreateSalesOrderItemRequest;
import com.example.scm.sales.dto.CreateSalesOrderRequest;
import com.example.scm.sales.entity.SalesOrder;
import com.example.scm.sales.entity.SalesOrderItem;
import com.example.scm.sales.entity.SalesOrderStatus;
import com.example.scm.sales.mapper.SalesOrderItemMapper;
import com.example.scm.sales.mapper.SalesOrderMapper;
import com.example.scm.sales.service.SalesOrderService;
import com.example.scm.sales.support.SalesOrderAssembler;
import com.example.scm.sales.vo.SalesOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SalesOrderServiceImpl implements SalesOrderService {

    private static final long SYSTEM_OPERATOR_ID = 1L;
    private static final int FAILURE_REASON_MAX_LENGTH = 255;

    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final SalesOrderAssembler salesOrderAssembler;
    private final InventoryReservationClient inventoryReservationClient;
    private final TransactionTemplate transactionTemplate;

    public SalesOrderServiceImpl(SalesOrderMapper salesOrderMapper,
                                 SalesOrderItemMapper salesOrderItemMapper,
                                 SalesOrderAssembler salesOrderAssembler,
                                 InventoryReservationClient inventoryReservationClient,
                                 TransactionTemplate transactionTemplate) {
        this.salesOrderMapper = salesOrderMapper;
        this.salesOrderItemMapper = salesOrderItemMapper;
        this.salesOrderAssembler = salesOrderAssembler;
        this.inventoryReservationClient = inventoryReservationClient;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public SalesOrderVO create(CreateSalesOrderRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        SalesOrder existingOrder = salesOrderMapper.selectByOrderNo(tenantId, request.getOrderNo()).orElse(null);
        if (existingOrder != null) {
            if (toStatus(existingOrder).isLockSuccess()) {
                return getById(existingOrder.getId());
            }
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Order no already exists");
        }
        SalesOrder order = transactionTemplate.execute(status -> createOrderInTransaction(tenantId, request));
        if (order == null) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Create sales order failed");
        }
        return processLock(tenantId, order);
    }

    @Override
    public SalesOrderVO getById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        SalesOrder order = salesOrderMapper.selectById(tenantId, id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Sales order not found"));
        return toVO(order, salesOrderItemMapper.selectByOrderId(tenantId, id));
    }

    @Override
    public SalesOrderVO getByOrderNo(String orderNo) {
        Long tenantId = TenantContext.getRequiredTenantId();
        SalesOrder order = salesOrderMapper.selectByOrderNo(tenantId, orderNo)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Sales order not found"));
        return toVO(order, salesOrderItemMapper.selectByOrderId(tenantId, order.getId()));
    }

    @Override
    public List<SalesOrderVO> list() {
        Long tenantId = TenantContext.getRequiredTenantId();
        return salesOrderMapper.selectByTenantId(tenantId).stream()
                .map(order -> toVO(order, salesOrderItemMapper.selectByOrderId(tenantId, order.getId())))
                .toList();
    }

    @Override
    public SalesOrderVO retryLock(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        SalesOrder order = salesOrderMapper.selectById(tenantId, id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Sales order not found"));
        SalesOrderStatus status = toStatus(order);
        if (status.isLockSuccess()) {
            return getById(order.getId());
        }
        if (!status.canRetryLock()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Only failed sales order can retry lock");
        }
        return processLock(tenantId, order);
    }

    @Override
    public SalesOrderVO ship(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        SalesOrder order = salesOrderMapper.selectById(tenantId, id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Sales order not found"));
        if (!toStatus(order).canShip()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Only locked sales order can ship");
        }
        return processShip(tenantId, order);
    }

    @Override
    public SalesOrderVO retryShip(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        SalesOrder order = salesOrderMapper.selectById(tenantId, id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Sales order not found"));
        SalesOrderStatus status = toStatus(order);
        if (status == SalesOrderStatus.SHIPPED) {
            return getById(order.getId());
        }
        if (!status.canRetryShip()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Only failed sales order can retry ship");
        }
        return processShip(tenantId, order);
    }

    @Override
    public SalesOrderVO cancel(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        SalesOrder order = salesOrderMapper.selectById(tenantId, id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Sales order not found"));
        SalesOrderStatus status = toStatus(order);
        if (!status.canCancel()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Current sales order cannot be cancelled");
        }
        if (status.isLockSuccess()) {
            inventoryReservationClient.unlock(tenantId, SYSTEM_OPERATOR_ID, order, salesOrderItemMapper.selectByOrderId(tenantId, order.getId()));
        }
        updateOrderStatus(tenantId, order.getId(), SalesOrderStatus.CANCELLED, null);
        return getById(order.getId());
    }

    private SalesOrder createOrderInTransaction(Long tenantId, CreateSalesOrderRequest request) {
        SalesOrder order = salesOrderAssembler.toNewOrder(tenantId, SYSTEM_OPERATOR_ID, SalesOrderStatus.CREATED.name(), request);
        salesOrderMapper.insert(order);
        List<SalesOrderItem> items = new ArrayList<>();
        for (CreateSalesOrderItemRequest itemRequest : request.getItems()) {
            SalesOrderItem item = salesOrderAssembler.toNewOrderItem(tenantId, order.getId(), itemRequest);
            salesOrderItemMapper.insert(item);
            items.add(item);
        }
        log.info("Create sales order persisted, tenantId={}, orderId={}, orderNo={}, itemCount={}",
                tenantId, order.getId(), order.getOrderNo(), items.size());
        return order;
    }

    private SalesOrderVO processLock(Long tenantId, SalesOrder order) {
        List<SalesOrderItem> items = salesOrderItemMapper.selectByOrderId(tenantId, order.getId());
        try {
            inventoryReservationClient.lock(tenantId, SYSTEM_OPERATOR_ID, order, items);
            updateOrderStatus(tenantId, order.getId(), SalesOrderStatus.LOCK_SUCCESS, null);
        } catch (BusinessException ex) {
            updateOrderStatus(tenantId, order.getId(), SalesOrderStatus.LOCK_FAILED, truncateFailureReason(ex.getMessage()));
            throw ex;
        }
        return getById(order.getId());
    }

    private SalesOrderVO processShip(Long tenantId, SalesOrder order) {
        List<SalesOrderItem> items = salesOrderItemMapper.selectByOrderId(tenantId, order.getId());
        try {
            inventoryReservationClient.stockOut(tenantId, SYSTEM_OPERATOR_ID, order, items);
            updateOrderStatus(tenantId, order.getId(), SalesOrderStatus.SHIPPED, null);
        } catch (BusinessException ex) {
            updateOrderStatus(tenantId, order.getId(), SalesOrderStatus.SHIP_FAILED, truncateFailureReason(ex.getMessage()));
            throw ex;
        }
        return getById(order.getId());
    }

    private void updateOrderStatus(Long tenantId, Long id, SalesOrderStatus status, String failureReason) {
        Integer affected = transactionTemplate.execute(tx ->
                salesOrderMapper.updateStatus(tenantId, id, status.name(), failureReason, SYSTEM_OPERATOR_ID));
        if (affected == null || affected == 0) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Update sales order status failed");
        }
    }

    private SalesOrderVO toVO(SalesOrder order, List<SalesOrderItem> items) {
        return salesOrderAssembler.toVO(order, items);
    }

    private String truncateFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return null;
        }
        return failureReason.length() <= FAILURE_REASON_MAX_LENGTH ? failureReason : failureReason.substring(0, FAILURE_REASON_MAX_LENGTH);
    }

    private SalesOrderStatus toStatus(SalesOrder order) {
        try {
            return SalesOrderStatus.valueOf(order.getOrderStatus());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Unknown sales order status: " + order.getOrderStatus());
        }
    }
}
