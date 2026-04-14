package com.example.scm.mdm.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "物料返回视图。")
public class MaterialVO {

    @Schema(description = "物料主键ID。")
    private Long id;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getMaterialSpec() {
        return materialSpec;
    }

    public void setMaterialSpec(String materialSpec) {
        this.materialSpec = materialSpec;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
