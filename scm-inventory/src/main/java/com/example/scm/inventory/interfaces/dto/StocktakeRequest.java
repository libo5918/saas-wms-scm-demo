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
@Schema(description = "库存盘点请求")
public class StocktakeRequest {

    @NotBlank
    @Schema(description = "业务类型", example = "STOCKTAKE")
    private String bizType;

    @NotBlank
    @Schema(description = "业务单号", example = "STK-001")
    private String bizNo;

    @NotNull
    @Schema(description = "操作人", example = "1")
    private Long operatorId;

    @Valid
    @NotEmpty
    @Schema(description = "盘点明细")
    private List<StocktakeItemRequest> items = new ArrayList<>();
}
