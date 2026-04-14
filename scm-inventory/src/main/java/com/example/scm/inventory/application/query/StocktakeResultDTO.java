package com.example.scm.inventory.application.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Schema(description = "库存盘点结果 DTO")
public class StocktakeResultDTO {

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务单号")
    private String bizNo;

    @Schema(description = "盘点结果明细")
    private List<StocktakeLineResultDTO> lines = new ArrayList<>();
}
