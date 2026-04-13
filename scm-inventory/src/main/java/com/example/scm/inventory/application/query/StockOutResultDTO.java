package com.example.scm.inventory.application.query;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "库存出库结果 DTO。")
public class StockOutResultDTO {

    @Schema(description = "业务类型。")
    private String bizType;

    @Schema(description = "业务单号。")
    private String bizNo;

    @Schema(description = "出库结果明细列表。")
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
