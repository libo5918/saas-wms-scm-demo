package com.example.scm.inventory.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Schema(description = "库存调整请求。")
public class StockAdjustRequest {

    @NotBlank
    @Schema(description = "业务类型。", example = "MANUAL_ADJUST")
    private String bizType;

    @NotBlank
    @Schema(description = "业务单号。", example = "ADJ-001")
    private String bizNo;

    @NotBlank
    @Schema(description = "调整类型，INCREASE 或 DECREASE。", example = "INCREASE")
    private String adjustType;

    @NotNull
    @Schema(description = "操作人。", example = "1")
    private Long operatorId;

    @Valid
    @NotEmpty
    @Schema(description = "调整明细列表。")
    private List<StockAdjustItemRequest> items = new ArrayList<>();
}
