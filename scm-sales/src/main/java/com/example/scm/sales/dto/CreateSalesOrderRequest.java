package com.example.scm.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "创建销售订单请求。")
public class CreateSalesOrderRequest {

    @Schema(description = "销售单号。")
    @NotBlank(message = "orderNo cannot be blank")
    private String orderNo;
    @Schema(description = "仓库ID。")
    @NotNull(message = "warehouseId cannot be null")
    private Long warehouseId;
    @Schema(description = "销售明细列表。")
    @Valid
    @NotEmpty(message = "items cannot be empty")
    private List<CreateSalesOrderItemRequest> items = new ArrayList<>();

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public List<CreateSalesOrderItemRequest> getItems() { return items; }
    public void setItems(List<CreateSalesOrderItemRequest> items) { this.items = items; }
}
