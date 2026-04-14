package com.example.scm.mdm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "修改仓库请求")
public class UpdateWarehouseRequest {

    @Schema(description = "仓库名称")
    @NotBlank(message = "warehouseName cannot be blank")
    @Size(max = 128, message = "warehouseName length must be <= 128")
    private String warehouseName;

    @Schema(description = "仓库类型")
    @Size(max = 32, message = "warehouseType length must be <= 32")
    private String warehouseType;

    @Schema(description = "联系人")
    @Size(max = 64, message = "contactName length must be <= 64")
    private String contactName;

    @Schema(description = "联系电话")
    @Size(max = 32, message = "contactPhone length must be <= 32")
    private String contactPhone;

    @Schema(description = "仓库地址")
    @Size(max = 255, message = "address length must be <= 255")
    private String address;

    @Schema(description = "仓库状态")
    @NotNull(message = "status cannot be null")
    private Integer status;

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getWarehouseType() {
        return warehouseType;
    }

    public void setWarehouseType(String warehouseType) {
        this.warehouseType = warehouseType;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
