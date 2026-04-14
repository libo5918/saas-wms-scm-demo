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
@Schema(description = "库存移库请求。")
public class StockTransferRequest {

    @NotBlank
    @Schema(description = "业务类型。", example = "INVENTORY_TRANSFER")
    private String bizType;

    @NotBlank
    @Schema(description = "业务单号。", example = "TRF-001")
    private String bizNo;

    @NotNull
    @Schema(description = "操作人。", example = "1")
    private Long operatorId;

    @Valid
    @NotEmpty
    @Schema(description = "移库明细列表。")
    private List<StockTransferItemRequest> items = new ArrayList<>();
}
