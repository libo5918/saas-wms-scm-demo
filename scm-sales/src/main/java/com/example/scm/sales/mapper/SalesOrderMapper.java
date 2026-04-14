package com.example.scm.sales.mapper;

import com.example.scm.sales.entity.SalesOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SalesOrderMapper {

    Optional<SalesOrder> selectById(@Param("tenantId") Long tenantId, @Param("id") Long id);

    Optional<SalesOrder> selectByOrderNo(@Param("tenantId") Long tenantId, @Param("orderNo") String orderNo);

    List<SalesOrder> selectByTenantId(@Param("tenantId") Long tenantId);

    @Insert("""
            INSERT INTO sales_order(
                tenant_id, order_no, warehouse_id, order_status, failure_reason, created_by, updated_by, deleted
            ) VALUES (
                #{tenantId}, #{orderNo}, #{warehouseId}, #{orderStatus}, #{failureReason}, #{createdBy}, #{updatedBy}, #{deleted}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SalesOrder salesOrder);

    @Update("""
            UPDATE sales_order
            SET order_status = #{orderStatus},
                failure_reason = #{failureReason},
                updated_by = #{updatedBy},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0
            """)
    int updateStatus(@Param("tenantId") Long tenantId,
                     @Param("id") Long id,
                     @Param("orderStatus") String orderStatus,
                     @Param("failureReason") String failureReason,
                     @Param("updatedBy") Long updatedBy);
}
