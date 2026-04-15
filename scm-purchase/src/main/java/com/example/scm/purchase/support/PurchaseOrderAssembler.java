package com.example.scm.purchase.support;

import com.example.scm.purchase.dto.CreatePurchaseOrderItemRequest;
import com.example.scm.purchase.dto.CreatePurchaseOrderRequest;
import com.example.scm.purchase.entity.PurchaseOrder;
import com.example.scm.purchase.entity.PurchaseOrderItem;
import com.example.scm.purchase.vo.PurchaseOrderItemVO;
import com.example.scm.purchase.vo.PurchaseOrderVO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 采购订单对象转换器。
 */
@Component
public class PurchaseOrderAssembler {

    public PurchaseOrder toNewOrder(Long tenantId, Long operatorId, String status, CreatePurchaseOrderRequest request, BigDecimal totalAmount) {
        PurchaseOrder order = new PurchaseOrder();
        order.setTenantId(tenantId);
        order.setOrderNo(request.getOrderNo());
        order.setSupplierId(request.getSupplierId());
        order.setOrderStatus(status);
        order.setTotalAmount(totalAmount);
        order.setRemark(request.getRemark());
        order.setCreatedBy(operatorId);
        order.setUpdatedBy(operatorId);
        order.setDeleted(0);
        return order;
    }

    public PurchaseOrderItem toNewOrderItem(Long tenantId, Long orderId, CreatePurchaseOrderItemRequest request) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setTenantId(tenantId);
        item.setPurchaseOrderId(orderId);
        item.setMaterialId(request.getMaterialId());
        item.setPlanQty(request.getPlanQty());
        item.setReceivedQty(BigDecimal.ZERO);
        item.setUnitPrice(request.getUnitPrice());
        return item;
    }

    public PurchaseOrderVO toVO(PurchaseOrder order, List<PurchaseOrderItem> items) {
        PurchaseOrderVO vo = new PurchaseOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setSupplierId(order.getSupplierId());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setRemark(order.getRemark());
        vo.setItems(items.stream().map(this::toItemVO).toList());
        return vo;
    }

    private PurchaseOrderItemVO toItemVO(PurchaseOrderItem item) {
        PurchaseOrderItemVO vo = new PurchaseOrderItemVO();
        vo.setId(item.getId());
        vo.setMaterialId(item.getMaterialId());
        vo.setPlanQty(item.getPlanQty());
        vo.setReceivedQty(item.getReceivedQty());
        vo.setUnitPrice(item.getUnitPrice());
        return vo;
    }
}
