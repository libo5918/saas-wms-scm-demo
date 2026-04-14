package com.example.scm.sales.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Schema(description = "销售订单视图。")
public class SalesOrderVO {
    @Schema(description = "销售单主键ID。")
    private Long id;
    @Schema(description = "销售单号。")
    private String orderNo;
    @Schema(description = "仓库ID。")
    private Long warehouseId;
    @Schema(description = "销售单状态。")
    private String orderStatus;
    @Schema(description = "库存联动失败原因。")
    private String failureReason;
    @Schema(description = "明细列表。")
    private List<SalesOrderItemVO> items = new ArrayList<>();
}
