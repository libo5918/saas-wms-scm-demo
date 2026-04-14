package com.example.scm.inventory.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Schema(description = "库存移库结果视图。")
public class StockTransferResultVO {

    @Schema(description = "业务类型。")
    private String bizType;

    @Schema(description = "业务单号。")
    private String bizNo;

    @Schema(description = "移库结果明细。")
    private List<StockTransferLineVO> lines = new ArrayList<>();
}
