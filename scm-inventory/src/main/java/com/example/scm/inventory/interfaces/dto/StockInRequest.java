package com.example.scm.inventory.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StockInRequest {

    @NotBlank(message = "bizType cannot be blank")
    private String bizType;

    @NotBlank(message = "bizNo cannot be blank")
    private String bizNo;

    @NotNull(message = "operatorId cannot be null")
    private Long operatorId;

    @Valid
    @NotEmpty(message = "items cannot be empty")
    private List<StockInItemRequest> items = new ArrayList<>();

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public List<StockInItemRequest> getItems() {
        return items;
    }

    public void setItems(List<StockInItemRequest> items) {
        this.items = items;
    }
}
