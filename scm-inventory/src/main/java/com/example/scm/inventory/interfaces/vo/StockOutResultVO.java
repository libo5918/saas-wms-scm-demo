package com.example.scm.inventory.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "库存出库结果展示对象。")
public class StockOutResultVO {

    @Schema(description = "业务类型。")
    private String bizType;

    @Schema(description = "业务单号。")
    private String bizNo;

    @Schema(description = "出库结果明细列表。")
    private List<StockOutLineVO> lines = new ArrayList<>();

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

    public List<StockOutLineVO> getLines() {
        return lines;
    }

    public void setLines(List<StockOutLineVO> lines) {
        this.lines = lines;
    }
}
