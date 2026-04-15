package com.example.scm.purchase.mapper;

import com.example.scm.purchase.entity.PurchaseReceipt;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

/**
 * 采购收货单头数据访问接口。
 *
 * <p>这里负责的都是单头表 purchase_receipt 的直接读写，不包含业务编排。</p>
 */
@Mapper
public interface PurchaseReceiptMapper {

    /**
     * 按租户和主键查询收货单。
     *
     * <p>用于详情查询和创建后回查。</p>
     */
    Optional<PurchaseReceipt> selectById(@Param("tenantId") Long tenantId, @Param("id") Long id);

    /**
     * 按租户和收货单号查询收货单。
     *
     * <p>主要用于收货侧幂等和重复单号校验。</p>
     */
    Optional<PurchaseReceipt> selectByReceiptNo(@Param("tenantId") Long tenantId, @Param("receiptNo") String receiptNo);

    /**
     * 查询租户下全部收货单。
     *
     * <p>当前用于列表页和本地联调查看。</p>
     */
    List<PurchaseReceipt> selectByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 新增收货单头并回填主键。
     *
     * <p>初始插入时状态通常为 CREATED，failureReason 为空。</p>
     */
    @Insert("""
            INSERT INTO purchase_receipt(
                tenant_id, receipt_no, purchase_order_id, supplier_id, warehouse_id, receipt_status, failure_reason,
                created_by, updated_by, deleted
            )
            VALUES(
                #{tenantId}, #{receiptNo}, #{purchaseOrderId}, #{supplierId}, #{warehouseId}, #{receiptStatus}, #{failureReason},
                #{createdBy}, #{updatedBy}, #{deleted}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(PurchaseReceipt purchaseReceipt);

    /**
     * 更新收货单状态和失败原因。
     *
     * <p>这个方法同时用于：</p>
     * <p>1. 库存入库成功后回写 STOCK_IN_SUCCESS。</p>
     * <p>2. 库存入库失败后回写 STOCK_IN_FAILED 和 failureReason。</p>
     */
    @Update("""
            UPDATE purchase_receipt
            SET receipt_status = #{receiptStatus},
                failure_reason = #{failureReason},
                updated_by = #{updatedBy},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0
            """)
    int updateStatus(@Param("tenantId") Long tenantId,
                     @Param("id") Long id,
                     @Param("receiptStatus") String receiptStatus,
                     @Param("failureReason") String failureReason,
                     @Param("updatedBy") Long updatedBy);
}
