package com.example.scm.inventory.application.command;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "库存解锁应用命令。")
public class StockUnlockCommand {

    @Schema(description = "业务类型。")
    private String bizType;

    @Schema(description = "业务单号。")
    private String bizNo;

    @Schema(description = "操作人ID。")
    private Long operatorId;

    @Schema(description = "解锁明细列表。")
    private List<StockUnlockItemCommand> items = new ArrayList<>();

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

    public List<StockUnlockItemCommand> getItems() {
        return items;
    }

    public void setItems(List<StockUnlockItemCommand> items) {
        this.items = items;
    }
}
