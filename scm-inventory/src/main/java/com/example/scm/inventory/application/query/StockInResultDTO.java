package com.example.scm.inventory.application.query;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "库存入库结果 DTO。")
public class StockInResultDTO {

    @Schema(description = "业务类型。")
    private String bizType;

    @Schema(description = "业务单号。")
    private String bizNo;

    @Schema(description = "入库结果明细列表。")
    private List<StockInLineResultDTO> lines = new ArrayList<>();

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

    public List<StockInLineResultDTO> getLines() {
        return lines;
    }

    public void setLines(List<StockInLineResultDTO> lines) {
        this.lines = lines;
    }
}
