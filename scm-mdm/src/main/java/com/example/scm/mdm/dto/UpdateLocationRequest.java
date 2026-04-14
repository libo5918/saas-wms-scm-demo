package com.example.scm.mdm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "修改库位请求")
public class UpdateLocationRequest {

    @Schema(description = "库位名称")
    @NotBlank(message = "locationName cannot be blank")
    @Size(max = 128, message = "locationName length must be <= 128")
    private String locationName;

    @Schema(description = "库位类型")
    @Size(max = 32, message = "locationType length must be <= 32")
    private String locationType;

    @Schema(description = "库位状态")
    @NotNull(message = "status cannot be null")
    private Integer status;

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
