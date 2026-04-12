package com.example.scm.mdm.mapper;

import com.example.scm.mdm.entity.Material;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

@Mapper
public interface MaterialMapper {

    List<Material> selectByTenantId(@Param("tenantId") Long tenantId);

    Optional<Material> selectById(@Param("tenantId") Long tenantId, @Param("id") Long id);

    Optional<Material> selectByCode(@Param("tenantId") Long tenantId, @Param("materialCode") String materialCode);

    @Insert("""
            INSERT INTO mdm_material(tenant_id, material_code, material_name, material_spec, unit, material_type, status,
                                     created_by, updated_by, deleted)
            VALUES(#{tenantId}, #{materialCode}, #{materialName}, #{materialSpec}, #{unit}, #{materialType}, #{status},
                   #{createdBy}, #{updatedBy}, #{deleted})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Material material);

    @Update("""
            UPDATE mdm_material
            SET material_name = #{materialName}, material_spec = #{materialSpec}, unit = #{unit},
                material_type = #{materialType}, status = #{status}, updated_by = #{updatedBy}
            WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0
            """)
    int update(Material material);

    @Update("""
            UPDATE mdm_material
            SET deleted = 1, updated_by = #{operatorId}
            WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0
            """)
    int softDelete(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("operatorId") Long operatorId);
}
