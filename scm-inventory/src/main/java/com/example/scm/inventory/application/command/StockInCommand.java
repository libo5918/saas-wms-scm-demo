package com.example.scm.inventory.application.command;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "库存入库应用命令，承接接口层入参后进入应用服务编排。")
public class StockInCommand {

    @Schema(description = "业务类型，例如 PURCHASE_RECEIPT。")
    private String bizType;

    @Schema(description = "业务单号，用于关联来源单据和幂等校验。")
    private String bizNo;

    @Schema(description = "操作人ID。")
    private Long operatorId;

    @Schema(description = "入库明细列表。")
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
