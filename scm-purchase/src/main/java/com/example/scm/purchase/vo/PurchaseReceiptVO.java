package com.example.scm.purchase.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 采购收货单返回视图。
 *
 * <p>这个对象是控制器返回给前端或联调方的视图模型，包含：</p>
 * <p>1. 单头基础信息。</p>
 * <p>2. 当前收货状态。</p>
 * <p>3. 失败时的失败原因。</p>
 * <p>4. 明细行列表。</p>
 */
@Getter
@Setter
@Schema(description = "采购收货单视图对象，返回收货单头和明细信息。")
public class PurchaseReceiptVO {

    /**
     * 收货单主键。
     */
    private Long id;

    /**
     * 收货单号。
     */
    private String receiptNo;

    /**
     * 采购订单ID。
     */
    private Long purchaseOrderId;

    /**
     * 仓库ID。
     */
    private Long warehouseId;

    /**
     * 收货单状态。
     */
    private String receiptStatus;

    /**
     * 入库失败原因。
     *
     * <p>成功时为空，失败时可直接展示给联调人员排查。</p>
     */
    private String failureReason;

    /**
     * 收货明细列表。
     */
    private List<PurchaseReceiptItemVO> items = new ArrayList<>();
}
