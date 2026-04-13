package com.example.scm.purchase.service;

import com.example.scm.purchase.dto.CreatePurchaseReceiptRequest;
import com.example.scm.purchase.vo.PurchaseReceiptVO;

import java.util.List;

/**
 * 采购收货应用服务接口。
 *
 * <p>对外暴露收货单创建、查询和失败单据重试入库能力。</p>
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

    /**
     * 对失败的收货单重新触发库存入库。
     *
     * <p>该动作会在原单据上继续推进，而不是新建一张收货单。</p>
     */
    PurchaseReceiptVO retryStockIn(Long id);
}
