package com.example.scm.sales.support;

import com.example.scm.sales.dto.CreateSalesOrderItemRequest;
import com.example.scm.sales.dto.CreateSalesOrderRequest;
import com.example.scm.sales.entity.SalesOrder;
import com.example.scm.sales.entity.SalesOrderItem;
import com.example.scm.sales.vo.SalesOrderItemVO;
import com.example.scm.sales.vo.SalesOrderVO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SalesOrderAssembler {

    public SalesOrder toNewOrder(Long tenantId, Long operatorId, String orderStatus, CreateSalesOrderRequest request) {
        SalesOrder order = new SalesOrder();
        order.setTenantId(tenantId);
        order.setOrderNo(request.getOrderNo());
        order.setWarehouseId(request.getWarehouseId());
        order.setOrderStatus(orderStatus);
        order.setFailureReason(null);
        order.setCreatedBy(operatorId);
        order.setUpdatedBy(operatorId);
        order.setDeleted(0);
        return order;
    }

    public SalesOrderItem toNewOrderItem(Long tenantId, Long salesOrderId, CreateSalesOrderItemRequest request) {
        SalesOrderItem item = new SalesOrderItem();
        item.setTenantId(tenantId);
        item.setSalesOrderId(salesOrderId);
        item.setMaterialId(request.getMaterialId());
        item.setLocationId(request.getLocationId());
        item.setSaleQty(request.getSaleQty());
        return item;
    }

    public SalesOrderVO toVO(SalesOrder order, List<SalesOrderItem> items) {
        SalesOrderVO vo = new SalesOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setWarehouseId(order.getWarehouseId());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setFailureReason(order.getFailureReason());
        vo.setItems(items.stream().map(this::toItemVO).toList());
        return vo;
    }

    private SalesOrderItemVO toItemVO(SalesOrderItem item) {
        SalesOrderItemVO vo = new SalesOrderItemVO();
        vo.setId(item.getId());
        vo.setMaterialId(item.getMaterialId());
        vo.setLocationId(item.getLocationId());
        vo.setSaleQty(item.getSaleQty());
        return vo;
    }
}
