package com.example.scm.mdm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateMaterialRequest {

    @NotBlank(message = "materialName cannot be blank")
    @Size(max = 128, message = "materialName length must be <= 128")
    private String materialName;

    @Size(max = 255, message = "materialSpec length must be <= 255")
    private String materialSpec;

    @NotBlank(message = "unit cannot be blank")
    @Size(max = 32, message = "unit length must be <= 32")
    private String unit;

    @NotBlank(message = "materialType cannot be blank")
    @Size(max = 32, message = "materialType length must be <= 32")
    private String materialType;

    @NotNull(message = "status cannot be null")
    private Integer status;

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
