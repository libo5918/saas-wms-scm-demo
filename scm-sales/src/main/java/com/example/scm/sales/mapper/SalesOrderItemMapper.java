package com.example.scm.sales.mapper;

import com.example.scm.sales.entity.SalesOrderItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SalesOrderItemMapper {

    @Insert("""
            INSERT INTO sales_order_item(tenant_id, sales_order_id, material_id, location_id, sale_qty)
            VALUES(#{tenantId}, #{salesOrderId}, #{materialId}, #{locationId}, #{saleQty})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SalesOrderItem item);

    @Select("""
            SELECT id, tenant_id, sales_order_id, material_id, location_id, sale_qty, created_at
            FROM sales_order_item
            WHERE sales_order_id = #{salesOrderId} AND tenant_id = #{tenantId}
            ORDER BY id ASC
            """)
    List<SalesOrderItem> selectByOrderId(@Param("tenantId") Long tenantId, @Param("salesOrderId") Long salesOrderId);
}
