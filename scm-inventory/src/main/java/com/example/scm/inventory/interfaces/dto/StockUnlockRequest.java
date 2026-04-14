package com.example.scm.inventory.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "库存解锁请求。")
public class StockUnlockRequest {
    @Schema(description = "业务类型。")
    @NotBlank(message = "bizType cannot be blank")
    private String bizType;
    @Schema(description = "业务单号。")
    @NotBlank(message = "bizNo cannot be blank")
    private String bizNo;
    @Schema(description = "操作人ID。")
    @NotNull(message = "operatorId cannot be null")
    private Long operatorId;
    @Schema(description = "解锁明细列表。")
    @Valid
    @NotEmpty(message = "items cannot be empty")
    private List<StockUnlockItemRequest> items = new ArrayList<>();

    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public List<StockUnlockItemRequest> getItems() { return items; }
    public void setItems(List<StockUnlockItemRequest> items) { this.items = items; }
}
