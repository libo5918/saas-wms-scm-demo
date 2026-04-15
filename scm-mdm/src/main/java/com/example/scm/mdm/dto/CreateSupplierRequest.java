package com.example.scm.mdm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "创建供应商请求")
public class CreateSupplierRequest {

    @Schema(description = "供应商编码")
    @NotBlank(message = "supplierCode cannot be blank")
    @Size(max = 64, message = "supplierCode length must be <= 64")
    private String supplierCode;

    @Schema(description = "供应商名称")
    @NotBlank(message = "supplierName cannot be blank")
    @Size(max = 128, message = "supplierName length must be <= 128")
    private String supplierName;

    @Schema(description = "联系人")
    @Size(max = 64, message = "contactName length must be <= 64")
    private String contactName;

    @Schema(description = "联系电话")
    @Size(max = 32, message = "contactPhone length must be <= 32")
    private String contactPhone;

    @Schema(description = "供应商状态")
    @NotNull(message = "status cannot be null")
    private Integer status;

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
