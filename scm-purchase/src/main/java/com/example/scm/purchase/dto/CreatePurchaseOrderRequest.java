package com.example.scm.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建采购订单请求。
 */
@Schema(description = "创建采购订单请求。")
public class CreatePurchaseOrderRequest {

    @Schema(description = "采购订单号")
    @NotBlank(message = "orderNo cannot be blank")
    private String orderNo;

    @Schema(description = "供应商ID")
    @NotNull(message = "supplierId cannot be null")
    private Long supplierId;

    @Schema(description = "采购订单明细列表")
    @Valid
    @NotEmpty(message = "items cannot be empty")
    private List<CreatePurchaseOrderItemRequest> items = new ArrayList<>();

    @Schema(description = "备注")
    private String remark;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public List<CreatePurchaseOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CreatePurchaseOrderItemRequest> items) {
        this.items = items;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
