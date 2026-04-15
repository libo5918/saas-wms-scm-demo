package com.example.scm.purchase.service.impl;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.purchase.client.MaterialClient;
import com.example.scm.purchase.client.SupplierClient;
import com.example.scm.purchase.dto.CreatePurchaseOrderItemRequest;
import com.example.scm.purchase.dto.CreatePurchaseOrderRequest;
import com.example.scm.purchase.entity.PurchaseOrder;
import com.example.scm.purchase.entity.PurchaseOrderItem;
import com.example.scm.purchase.entity.PurchaseOrderStatus;
import com.example.scm.purchase.mapper.PurchaseOrderItemMapper;
import com.example.scm.purchase.mapper.PurchaseOrderMapper;
import com.example.scm.purchase.service.PurchaseOrderService;
import com.example.scm.purchase.support.PurchaseOrderAssembler;
import com.example.scm.purchase.vo.PurchaseOrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 采购订单应用服务实现。
 */
@Service
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private static final long SYSTEM_OPERATOR_ID = 1L;

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final PurchaseOrderAssembler purchaseOrderAssembler;
    private final SupplierClient supplierClient;
    private final MaterialClient materialClient;
    private final TransactionTemplate transactionTemplate;

    public PurchaseOrderServiceImpl(PurchaseOrderMapper purchaseOrderMapper,
                                    PurchaseOrderItemMapper purchaseOrderItemMapper,
                                    PurchaseOrderAssembler purchaseOrderAssembler,
                                    SupplierClient supplierClient,
                                    MaterialClient materialClient,
                                    TransactionTemplate transactionTemplate) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.purchaseOrderAssembler = purchaseOrderAssembler;
        this.supplierClient = supplierClient;
        this.materialClient = materialClient;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public PurchaseOrderVO create(CreatePurchaseOrderRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        PurchaseOrder existingOrder = purchaseOrderMapper.selectByOrderNo(tenantId, request.getOrderNo()).orElse(null);
        if (existingOrder != null) {
            if (PurchaseOrderStatus.CANCELLED.name().equals(existingOrder.getOrderStatus())) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Order no already exists");
            }
            return getById(existingOrder.getId());
        }

        supplierClient.validateSupplierEnabled(tenantId, request.getSupplierId());
        validateMaterials(tenantId, request.getItems());

        BigDecimal totalAmount = calculateTotalAmount(request.getItems());
        PurchaseOrder order = transactionTemplate.execute(status -> createOrderInTransaction(tenantId, request, totalAmount));
        if (order == null) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Create purchase order failed");
        }
        return getById(order.getId());
    }

    @Override
    public PurchaseOrderVO getById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        PurchaseOrder order = purchaseOrderMapper.selectById(tenantId, id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Purchase order not found"));
        return purchaseOrderAssembler.toVO(order, purchaseOrderItemMapper.selectByOrderId(tenantId, id));
    }

    @Override
    public PurchaseOrderVO getByOrderNo(String orderNo) {
        Long tenantId = TenantContext.getRequiredTenantId();
        PurchaseOrder order = purchaseOrderMapper.selectByOrderNo(tenantId, orderNo)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Purchase order not found"));
        return purchaseOrderAssembler.toVO(order, purchaseOrderItemMapper.selectByOrderId(tenantId, order.getId()));
    }

    @Override
    public List<PurchaseOrderVO> list() {
        Long tenantId = TenantContext.getRequiredTenantId();
        return purchaseOrderMapper.selectByTenantId(tenantId).stream()
                .map(order -> purchaseOrderAssembler.toVO(order, purchaseOrderItemMapper.selectByOrderId(tenantId, order.getId())))
                .toList();
    }

    @Override
    public PurchaseOrderVO cancel(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        PurchaseOrder order = purchaseOrderMapper.selectById(tenantId, id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Purchase order not found"));

        PurchaseOrderStatus currentStatus;
        try {
            currentStatus = PurchaseOrderStatus.valueOf(order.getOrderStatus());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Unknown purchase order status: " + order.getOrderStatus());
        }

        if (currentStatus != PurchaseOrderStatus.CREATED) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Only created purchase order can be cancelled");
        }

        Integer affected = purchaseOrderMapper.updateStatus(tenantId, id, PurchaseOrderStatus.CANCELLED.name(), SYSTEM_OPERATOR_ID);
        if (affected == null || affected == 0) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Cancel purchase order failed");
        }
        return getById(id);
    }

    private PurchaseOrder createOrderInTransaction(Long tenantId, CreatePurchaseOrderRequest request, BigDecimal totalAmount) {
        PurchaseOrder order = purchaseOrderAssembler.toNewOrder(tenantId, SYSTEM_OPERATOR_ID, PurchaseOrderStatus.CREATED.name(), request, totalAmount);
        purchaseOrderMapper.insert(order);
        List<PurchaseOrderItem> items = new ArrayList<>();
        for (CreatePurchaseOrderItemRequest itemRequest : request.getItems()) {
            PurchaseOrderItem item = purchaseOrderAssembler.toNewOrderItem(tenantId, order.getId(), itemRequest);
            purchaseOrderItemMapper.insert(item);
            items.add(item);
        }
        return order;
    }

    private void validateMaterials(Long tenantId, List<CreatePurchaseOrderItemRequest> items) {
        for (CreatePurchaseOrderItemRequest item : items) {
            materialClient.validateMaterialEnabled(tenantId, item.getMaterialId());
        }
    }

    private BigDecimal calculateTotalAmount(List<CreatePurchaseOrderItemRequest> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (CreatePurchaseOrderItemRequest item : items) {
            total = total.add(item.getPlanQty().multiply(item.getUnitPrice()));
        }
        return total;
    }
}
