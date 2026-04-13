package com.example.scm.mdm.mapper;

import com.example.scm.mdm.entity.Material;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

/**
 * 物料数据访问接口，负责对 `mdm_material` 表执行查询和写入。
 */
@Mapper
public interface MaterialMapper {

    /**
     * 按租户查询全部未删除物料。
     */
    List<Material> selectByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 按租户和主键查询单个物料。
     */
    Optional<Material> selectById(@Param("tenantId") Long tenantId, @Param("id") Long id);

    /**
     * 按租户和物料编码查询物料，用于唯一性校验。
     */
    Optional<Material> selectByCode(@Param("tenantId") Long tenantId, @Param("materialCode") String materialCode);

    /**
     * 新增物料记录并回填主键。
     */
    @Insert("""
            INSERT INTO mdm_material(tenant_id, material_code, material_name, material_spec, unit, material_type, status,
                                     created_by, updated_by, deleted)
            VALUES(#{tenantId}, #{materialCode}, #{materialName}, #{materialSpec}, #{unit}, #{materialType}, #{status},
                   #{createdBy}, #{updatedBy}, #{deleted})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Material material);

    /**
     * 更新物料基础属性。
     */
    @Update("""
            UPDATE mdm_material
            SET material_name = #{materialName}, material_spec = #{materialSpec}, unit = #{unit},
                material_type = #{materialType}, status = #{status}, updated_by = #{updatedBy}
            WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0
            """)
    int update(Material material);

    /**
     * 逻辑删除物料。
     */
    @Update("""
            UPDATE mdm_material
            SET deleted = 1, updated_by = #{operatorId}
            WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0
            """)
    int softDelete(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("operatorId") Long operatorId);
}
