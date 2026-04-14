package com.example.scm.sales.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "销售订单明细视图。")
public class SalesOrderItemVO {
    @Schema(description = "明细主键ID。")
    private Long id;
    @Schema(description = "物料ID。")
    private Long materialId;
    @Schema(description = "库位ID。")
    private Long locationId;
    @Schema(description = "销售数量。")
    private BigDecimal saleQty;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public BigDecimal getSaleQty() { return saleQty; }
    public void setSaleQty(BigDecimal saleQty) { this.saleQty = saleQty; }
}
