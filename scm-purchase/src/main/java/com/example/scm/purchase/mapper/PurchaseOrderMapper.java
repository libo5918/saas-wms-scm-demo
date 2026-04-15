package com.example.scm.purchase.mapper;

import com.example.scm.purchase.entity.PurchaseOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

/**
 * 采购订单头数据访问接口。
 */
@Mapper
public interface PurchaseOrderMapper {

    Optional<PurchaseOrder> selectById(@Param("tenantId") Long tenantId, @Param("id") Long id);

    Optional<PurchaseOrder> selectByOrderNo(@Param("tenantId") Long tenantId, @Param("orderNo") String orderNo);

    List<PurchaseOrder> selectByTenantId(@Param("tenantId") Long tenantId);

    @Insert("""
            INSERT INTO purchase_order(
                tenant_id, order_no, supplier_id, order_status, total_amount, remark, created_by, updated_by, deleted
            )
            VALUES(
                #{tenantId}, #{orderNo}, #{supplierId}, #{orderStatus}, #{totalAmount}, #{remark},
                #{createdBy}, #{updatedBy}, #{deleted}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(PurchaseOrder purchaseOrder);

    @Update("""
            UPDATE purchase_order
            SET order_status = #{orderStatus},
                updated_by = #{operatorId}
            WHERE tenant_id = #{tenantId}
              AND id = #{id}
              AND deleted = 0
            """)
    Integer updateStatus(@Param("tenantId") Long tenantId,
                         @Param("id") Long id,
                         @Param("orderStatus") String orderStatus,
                         @Param("operatorId") Long operatorId);
}
