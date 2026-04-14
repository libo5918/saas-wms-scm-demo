package com.example.scm.mdm.mapper;

import com.example.scm.mdm.entity.Location;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

/**
 * 库位数据访问接口，负责对 `mdm_location` 表执行查询和写入。
 */
@Mapper
public interface LocationMapper {

    /**
     * 按租户查询全部未删除库位。
     */
    List<Location> selectByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 按租户和仓库查询库位列表。
     */
    List<Location> selectByWarehouseId(@Param("tenantId") Long tenantId, @Param("warehouseId") Long warehouseId);

    /**
     * 按租户和主键查询单个库位。
     */
    Optional<Location> selectById(@Param("tenantId") Long tenantId, @Param("id") Long id);

    /**
     * 按租户、仓库和库位编码查询库位，用于唯一性校验。
     */
    Optional<Location> selectByCode(@Param("tenantId") Long tenantId,
                                    @Param("warehouseId") Long warehouseId,
                                    @Param("locationCode") String locationCode);

    /**
     * 新增库位记录并回填主键。
     */
    @Insert("""
            INSERT INTO mdm_location(tenant_id, warehouse_id, location_code, location_name, location_type,
                                     status, created_by, updated_by, deleted)
            VALUES(#{tenantId}, #{warehouseId}, #{locationCode}, #{locationName}, #{locationType},
                   #{status}, #{createdBy}, #{updatedBy}, #{deleted})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Location location);

    /**
     * 更新库位基础属性。
     */
    @Update("""
            UPDATE mdm_location
            SET location_name = #{locationName}, location_type = #{locationType}, status = #{status},
                updated_by = #{updatedBy}
            WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0
            """)
    int update(Location location);

    /**
     * 逻辑删除库位。
     */
    @Update("""
            UPDATE mdm_location
            SET deleted = 1, updated_by = #{operatorId}
            WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0
            """)
    int softDelete(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("operatorId") Long operatorId);
}
