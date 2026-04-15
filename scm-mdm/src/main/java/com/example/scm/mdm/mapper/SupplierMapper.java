package com.example.scm.mdm.mapper;

import com.example.scm.mdm.entity.Supplier;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

/**
 * 供应商数据访问接口，负责对 `mdm_supplier` 表执行查询和写入。
 */
@Mapper
public interface SupplierMapper {

    List<Supplier> selectByTenantId(@Param("tenantId") Long tenantId);

    Optional<Supplier> selectById(@Param("tenantId") Long tenantId, @Param("id") Long id);

    Optional<Supplier> selectByCode(@Param("tenantId") Long tenantId, @Param("supplierCode") String supplierCode);

    @Insert("""
            INSERT INTO mdm_supplier(tenant_id, supplier_code, supplier_name, contact_name, contact_phone, status,
                                     created_by, updated_by, deleted)
            VALUES(#{tenantId}, #{supplierCode}, #{supplierName}, #{contactName}, #{contactPhone}, #{status},
                   #{createdBy}, #{updatedBy}, #{deleted})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Supplier supplier);

    @Update("""
            UPDATE mdm_supplier
            SET supplier_name = #{supplierName}, contact_name = #{contactName}, contact_phone = #{contactPhone},
                status = #{status}, updated_by = #{updatedBy}
            WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0
            """)
    int update(Supplier supplier);

    @Update("""
            UPDATE mdm_supplier
            SET deleted = 1, updated_by = #{operatorId}
            WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0
            """)
    int softDelete(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("operatorId") Long operatorId);
}
