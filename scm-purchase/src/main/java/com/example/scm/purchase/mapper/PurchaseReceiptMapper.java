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
 */
@Mapper
public interface PurchaseReceiptMapper {

    /**
     * 按租户和主键查询收货单头。
     */
    Optional<PurchaseReceipt> selectById(@Param("tenantId") Long tenantId, @Param("id") Long id);

    /**
     * 按租户和收货单号查询收货单头。
     */
    Optional<PurchaseReceipt> selectByReceiptNo(@Param("tenantId") Long tenantId, @Param("receiptNo") String receiptNo);

    /**
     * 查询租户下全部收货单头。
     */
    List<PurchaseReceipt> selectByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 新增收货单头并回填主键。
     */
    @Insert("""
            INSERT INTO purchase_receipt(tenant_id, receipt_no, purchase_order_id, warehouse_id, receipt_status,
                                         created_by, updated_by, deleted)
            VALUES(#{tenantId}, #{receiptNo}, #{purchaseOrderId}, #{warehouseId}, #{receiptStatus},
                   #{createdBy}, #{updatedBy}, #{deleted})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(PurchaseReceipt purchaseReceipt);

    /**
     * 更新收货单状态。
     */
    @Update("""
            UPDATE purchase_receipt
            SET receipt_status = #{receiptStatus}, updated_by = #{updatedBy}
            WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0
            """)
    int updateStatus(@Param("tenantId") Long tenantId,
                     @Param("id") Long id,
                     @Param("receiptStatus") String receiptStatus,
                     @Param("updatedBy") Long updatedBy);
}
