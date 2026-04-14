package com.example.scm.inventory.application.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Schema(description = "库存调整命令。")
public class StockAdjustCommand {

    @Schema(description = "业务类型。")
    private String bizType;

    @Schema(description = "业务单号。")
    private String bizNo;

    @Schema(description = "调整类型，INCREASE 或 DECREASE。")
    private String adjustType;

    @Schema(description = "操作人。")
    private Long operatorId;

    @Schema(description = "调整明细列表。")
    private List<StockAdjustItemCommand> items = new ArrayList<>();
}
