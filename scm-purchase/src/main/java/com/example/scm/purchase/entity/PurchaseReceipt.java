package com.example.scm.purchase.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 采购收货单单头实体。
 *
 * <p>这个对象表示一笔采购到货在采购域中的业务单头，会被用于：</p>
 * <p>1. 持久化收货单基础信息，例如收货单号、采购订单、仓库。</p>
 * <p>2. 跟踪收货和库存联动的处理状态。</p>
 * <p>3. 在库存联动失败时保留失败原因，便于排障和后续补偿。</p>
 */
@Getter
@Setter
@Schema(description = "采购收货单实体，表示一次到货收货的单头信息。")
public class PurchaseReceipt {

    /**
     * 收货单主键。
     */
    private Long id;

    /**
     * 租户ID，用于隔离不同租户的数据。
     */
    private Long tenantId;

    /**
     * 收货单号，作为采购收货业务的唯一业务标识。
     */
    private String receiptNo;

    /**
     * 关联的采购订单ID。
     */
    private Long purchaseOrderId;

    /**
     * 本次收货入到哪个仓库。
     */
    private Long warehouseId;

    /**
     * 收货单当前状态。
     *
     * <p>当前版本主要会出现 CREATED、STOCK_IN_SUCCESS、STOCK_IN_FAILED。</p>
     */
    private String receiptStatus;

    /**
     * 库存联动失败原因。
     *
     * <p>只有库存入库失败时才会记录，成功时为 null。</p>
     */
    private String failureReason;

    /**
     * 创建人。
     */
    private Long createdBy;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 最后更新人。
     */
    private Long updatedBy;

    /**
     * 最后更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记。
     */
    private Integer deleted;
}
