package com.example.scm.purchase.service;

import com.example.scm.purchase.dto.CreatePurchaseOrderRequest;
import com.example.scm.purchase.vo.PurchaseOrderVO;

import java.util.List;

/**
 * 采购订单服务接口。
 */
public interface PurchaseOrderService {

    PurchaseOrderVO create(CreatePurchaseOrderRequest request);

    PurchaseOrderVO getById(Long id);

    PurchaseOrderVO getByOrderNo(String orderNo);

    List<PurchaseOrderVO> list();

    PurchaseOrderVO cancel(Long id);
}
