package com.example.scm.mdm.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "物料主数据实体，对应租户下的物料档案。")
public class Material {

    private Long id;
    private Long tenantId;
    private String materialCode;
    private String materialName;
    private String materialSpec;
    private String unit;
    private String materialType;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
