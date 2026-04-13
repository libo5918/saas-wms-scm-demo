package com.example.scm.inventory;

import com.example.scm.common.web.GlobalExceptionHandler;
import com.example.scm.common.web.TenantHeaderInterceptor;
import com.example.scm.common.web.WebMvcConfiguration;
import com.example.scm.inventory.application.query.InventoryBalanceDTO;
import com.example.scm.inventory.application.query.InventoryTransactionRecordDTO;
import com.example.scm.inventory.application.query.StockInLineResultDTO;
import com.example.scm.inventory.application.query.StockInResultDTO;
import com.example.scm.inventory.application.service.InventoryBalanceQueryService;
import com.example.scm.inventory.application.service.InventoryStockInApplicationService;
import com.example.scm.inventory.application.service.InventoryTransactionRecordQueryService;
import com.example.scm.inventory.interfaces.assembler.InventoryStockAssembler;
import com.example.scm.inventory.interfaces.controller.InventoryStockController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryStockController.class)
@Import({GlobalExceptionHandler.class, TenantHeaderInterceptor.class, WebMvcConfiguration.class, InventoryStockAssembler.class})
class InventoryStockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryStockInApplicationService inventoryStockInApplicationService;

    @MockBean
    private InventoryBalanceQueryService inventoryBalanceQueryService;

    @MockBean
    private InventoryTransactionRecordQueryService inventoryTransactionRecordQueryService;

    @Test
    void shouldStockInSuccessfully() throws Exception {
        StockInLineResultDTO line = new StockInLineResultDTO();
        line.setTxnNo("IN-TEST-001");
        line.setMaterialId(1001L);
        line.setWarehouseId(2001L);
        line.setLocationId(3001L);
        line.setQuantity(new BigDecimal("10"));
        line.setBeforeQty(BigDecimal.ZERO);
        line.setAfterQty(new BigDecimal("10"));

        StockInResultDTO result = new StockInResultDTO();
        result.setBizType("PURCHASE_RECEIPT");
        result.setBizNo("RCV-001");
        result.setLines(List.of(line));
        when(inventoryStockInApplicationService.stockIn(any())).thenReturn(result);

        String requestBody = """
                {
                  "bizType":"PURCHASE_RECEIPT",
                  "bizNo":"RCV-001",
                  "operatorId":1,
                  "items":[
                    {
                      "materialId":1001,
                      "warehouseId":2001,
                      "locationId":3001,
                      "quantity":10
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/inventory/stock-ins")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bizNo").value("RCV-001"))
                .andExpect(jsonPath("$.data.lines[0].txnNo").value("IN-TEST-001"));
    }

    @Test
    void shouldQueryBalanceSuccessfully() throws Exception {
        InventoryBalanceDTO balance = new InventoryBalanceDTO();
        balance.setMaterialId(1001L);
        balance.setWarehouseId(2001L);
        balance.setLocationId(3001L);
        balance.setOnHandQty(new BigDecimal("10"));
        balance.setLockedQty(BigDecimal.ZERO);
        balance.setAvailableQty(new BigDecimal("10"));
        balance.setVersion(1L);
        when(inventoryBalanceQueryService.getBalance(1001L, 2001L, 3001L)).thenReturn(balance);

        mockMvc.perform(get("/api/v1/inventory/balances")
                        .header("X-Tenant-Id", "1")
                        .param("materialId", "1001")
                        .param("warehouseId", "2001")
                        .param("locationId", "3001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableQty").value(10));
    }

    @Test
    void shouldQueryTxnRecordsSuccessfully() throws Exception {
        InventoryTransactionRecordDTO record = new InventoryTransactionRecordDTO();
        record.setTxnNo("IN-TEST-001");
        record.setBizType("PURCHASE_RECEIPT");
        record.setBizNo("RCV-001");
        record.setMaterialId(1001L);
        record.setWarehouseId(2001L);
        record.setLocationId(3001L);
        record.setTxnDirection("IN");
        record.setTxnQty(new BigDecimal("10"));
        record.setBeforeQty(BigDecimal.ZERO);
        record.setAfterQty(new BigDecimal("10"));
        when(inventoryTransactionRecordQueryService.listByBizNo("PURCHASE_RECEIPT", "RCV-001")).thenReturn(List.of(record));

        mockMvc.perform(get("/api/v1/inventory/txn-records")
                        .header("X-Tenant-Id", "1")
                        .param("bizType", "PURCHASE_RECEIPT")
                        .param("bizNo", "RCV-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].txnNo").value("IN-TEST-001"))
                .andExpect(jsonPath("$.data[0].bizNo").value("RCV-001"));
    }
}
