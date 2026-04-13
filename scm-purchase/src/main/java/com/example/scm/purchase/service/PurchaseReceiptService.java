package com.example.scm.purchase.service;

import com.example.scm.purchase.dto.CreatePurchaseReceiptRequest;
import com.example.scm.purchase.vo.PurchaseReceiptVO;

import java.util.List;

/**
 * 采购收货应用服务接口。
 */
public interface PurchaseReceiptService {

    /**
     * 创建采购收货单。
     */
    PurchaseReceiptVO create(CreatePurchaseReceiptRequest request);

    /**
     * 查询采购收货单详情。
     */
    PurchaseReceiptVO getById(Long id);

    /**
     * 查询采购收货单列表。
     */
    List<PurchaseReceiptVO> list();
}
