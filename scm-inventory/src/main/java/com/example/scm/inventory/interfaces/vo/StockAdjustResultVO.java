package com.example.scm.inventory.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Schema(description = "库存调整结果视图。")
public class StockAdjustResultVO {

    @Schema(description = "业务类型。")
    private String bizType;

    @Schema(description = "业务单号。")
    private String bizNo;

    @Schema(description = "调整类型。")
    private String adjustType;

    @Schema(description = "调整结果明细。")
    private List<StockAdjustLineVO> lines = new ArrayList<>();
}
