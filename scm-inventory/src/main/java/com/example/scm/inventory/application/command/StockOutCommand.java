package com.example.scm.inventory.application.command;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "库存出库应用命令，承接接口层入参后进入应用服务编排。")
public class StockOutCommand {

    @Schema(description = "业务类型，例如 SALES_ORDER。")
    private String bizType;

    @Schema(description = "业务单号，用于关联来源单据和幂等校验。")
    private String bizNo;

    @Schema(description = "操作人ID。")
    private Long operatorId;

    @Schema(description = "出库明细列表。")
    private List<StockOutItemCommand> items = new ArrayList<>();

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

    public List<StockOutItemCommand> getItems() {
        return items;
    }

    public void setItems(List<StockOutItemCommand> items) {
        this.items = items;
    }
}
