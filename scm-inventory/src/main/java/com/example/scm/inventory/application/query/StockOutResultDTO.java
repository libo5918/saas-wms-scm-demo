package com.example.scm.inventory.application.query;

import java.util.ArrayList;
import java.util.List;

public class StockOutResultDTO {

    private String bizType;
    private String bizNo;
    private List<StockOutLineResultDTO> lines = new ArrayList<>();

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

    public List<StockOutLineResultDTO> getLines() {
        return lines;
    }

    public void setLines(List<StockOutLineResultDTO> lines) {
        this.lines = lines;
    }
}
