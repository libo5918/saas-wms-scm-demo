package com.example.scm.inventory.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "库存解锁结果展示对象。")
public class StockUnlockResultVO {
    @Schema(description = "业务类型。")
    private String bizType;
    @Schema(description = "业务单号。")
    private String bizNo;
    @Schema(description = "解锁结果明细列表。")
    private List<StockUnlockLineVO> lines = new ArrayList<>();
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public List<StockUnlockLineVO> getLines() { return lines; }
    public void setLines(List<StockUnlockLineVO> lines) { this.lines = lines; }
}
