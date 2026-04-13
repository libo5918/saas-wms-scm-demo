package com.example.scm.inventory.domain.inventory.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "库存入库领域事件，预留给后续异步通知或事件总线。")
public class InventoryStockInEvent {

    private Long tenantId;
    private String bizType;
    private String bizNo;
    private String txnNo;
    private Long materialId;
    private Long warehouseId;
    private Long locationId;
    private BigDecimal quantity;
    private BigDecimal afterQty;
}
