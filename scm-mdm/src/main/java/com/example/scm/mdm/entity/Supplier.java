package com.example.scm.mdm.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "供应商主数据实体")
public class Supplier {

    @Schema(description = "供应商主键ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "供应商编码")
    private String supplierCode;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "供应商状态")
    private Integer status;

    @Schema(description = "创建人")
    private Long createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新人")
    private Long updatedBy;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "逻辑删除标记")
    private Integer deleted;
}
