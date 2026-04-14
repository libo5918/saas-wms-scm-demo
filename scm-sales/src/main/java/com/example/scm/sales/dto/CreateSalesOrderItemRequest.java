package com.example.scm.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "创建销售订单明细请求。")
public class CreateSalesOrderItemRequest {

    @Schema(description = "物料ID。")
    @NotNull(message = "materialId cannot be null")
    private Long materialId;
    @Schema(description = "库位ID。")
    @NotNull(message = "locationId cannot be null")
    private Long locationId;
    @Schema(description = "销售数量。")
    @NotNull(message = "saleQty cannot be null")
    @Positive(message = "saleQty must be greater than zero")
    private BigDecimal saleQty;

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public BigDecimal getSaleQty() { return saleQty; }
    public void setSaleQty(BigDecimal saleQty) { this.saleQty = saleQty; }
}
