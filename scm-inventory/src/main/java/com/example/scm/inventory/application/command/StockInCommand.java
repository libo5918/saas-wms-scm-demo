package com.example.scm.inventory.application.command;

import java.util.ArrayList;
import java.util.List;

public class StockInCommand {

    private String bizType;
    private String bizNo;
    private Long operatorId;
    private List<StockInItemCommand> items = new ArrayList<>();

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

    public List<StockInItemCommand> getItems() {
        return items;
    }

    public void setItems(List<StockInItemCommand> items) {
        this.items = items;
    }
}
