package com.example.scm.mdm.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "物料主数据实体，对应租户下的物料档案。")
public class Material {

    @Schema(description = "物料主键ID。")
    private Long id;

    @Schema(description = "租户ID。")
    private Long tenantId;

    @Schema(description = "物料编码。")
    private String materialCode;

    @Schema(description = "物料名称。")
    private String materialName;

    @Schema(description = "物料规格。")
    private String materialSpec;

    @Schema(description = "计量单位。")
    private String unit;

    @Schema(description = "物料类型。")
    private String materialType;

    @Schema(description = "物料状态。")
    private Integer status;

    @Schema(description = "创建人。")
    private Long createdBy;

    @Schema(description = "创建时间。")
    private LocalDateTime createdAt;

    @Schema(description = "更新人。")
    private Long updatedBy;

    @Schema(description = "更新时间。")
    private LocalDateTime updatedAt;

    @Schema(description = "逻辑删除标记。")
    private Integer deleted;
}
