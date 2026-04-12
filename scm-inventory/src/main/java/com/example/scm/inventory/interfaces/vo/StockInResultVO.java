package com.example.scm.inventory.interfaces.vo;

import java.util.ArrayList;
import java.util.List;

public class StockInResultVO {

    private String bizType;
    private String bizNo;
    private List<StockInLineVO> lines = new ArrayList<>();

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

    public List<StockInLineVO> getLines() {
        return lines;
    }

    public void setLines(List<StockInLineVO> lines) {
        this.lines = lines;
    }
}
