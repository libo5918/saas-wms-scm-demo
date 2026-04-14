package com.example.scm.mdm.mapper;

import com.example.scm.mdm.entity.Warehouse;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

/**
 * 仓库数据访问接口，负责对 `mdm_warehouse` 表执行查询和写入。
 */
@Mapper
public interface WarehouseMapper {

    /**
     * 按租户查询全部未删除仓库。
     */
    List<Warehouse> selectByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 按租户和主键查询单个仓库。
     */
    Optional<Warehouse> selectById(@Param("tenantId") Long tenantId, @Param("id") Long id);

    /**
     * 按租户和仓库编码查询仓库，用于唯一性校验。
     */
    Optional<Warehouse> selectByCode(@Param("tenantId") Long tenantId, @Param("warehouseCode") String warehouseCode);

    /**
     * 新增仓库记录并回填主键。
     */
    @Insert("""
            INSERT INTO mdm_warehouse(tenant_id, warehouse_code, warehouse_name, warehouse_type, contact_name,
                                      contact_phone, address, status, created_by, updated_by, deleted)
            VALUES(#{tenantId}, #{warehouseCode}, #{warehouseName}, #{warehouseType}, #{contactName},
                   #{contactPhone}, #{address}, #{status}, #{createdBy}, #{updatedBy}, #{deleted})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Warehouse warehouse);

    /**
     * 更新仓库基础属性。
     */
    @Update("""
            UPDATE mdm_warehouse
            SET warehouse_name = #{warehouseName}, warehouse_type = #{warehouseType}, contact_name = #{contactName},
                contact_phone = #{contactPhone}, address = #{address}, status = #{status}, updated_by = #{updatedBy}
            WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0
            """)
    int update(Warehouse warehouse);

    /**
     * 逻辑删除仓库。
     */
    @Update("""
            UPDATE mdm_warehouse
            SET deleted = 1, updated_by = #{operatorId}
            WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0
            """)
    int softDelete(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("operatorId") Long operatorId);
}
