package com.example.scm.inventory;

import com.example.scm.common.web.GlobalExceptionHandler;
import com.example.scm.common.web.TenantHeaderInterceptor;
import com.example.scm.common.web.WebMvcConfiguration;
import com.example.scm.inventory.application.query.InventoryBalanceDTO;
import com.example.scm.inventory.application.query.InventoryTransactionRecordDTO;
import com.example.scm.inventory.application.query.StockAdjustLineResultDTO;
import com.example.scm.inventory.application.query.StockAdjustResultDTO;
import com.example.scm.inventory.application.query.StockInLineResultDTO;
import com.example.scm.inventory.application.query.StockInResultDTO;
import com.example.scm.inventory.application.query.StockLockLineResultDTO;
import com.example.scm.inventory.application.query.StockLockResultDTO;
import com.example.scm.inventory.application.query.StockOutLineResultDTO;
import com.example.scm.inventory.application.query.StockOutResultDTO;
import com.example.scm.inventory.application.query.StockTransferLineResultDTO;
import com.example.scm.inventory.application.query.StockTransferResultDTO;
import com.example.scm.inventory.application.query.StocktakeLineResultDTO;
import com.example.scm.inventory.application.query.StocktakeResultDTO;
import com.example.scm.inventory.application.query.StockUnlockLineResultDTO;
import com.example.scm.inventory.application.query.StockUnlockResultDTO;
import com.example.scm.inventory.application.service.InventoryBalanceQueryService;
import com.example.scm.inventory.application.service.InventoryLockedStockOutApplicationService;
import com.example.scm.inventory.application.service.InventoryStockAdjustApplicationService;
import com.example.scm.inventory.application.service.InventoryStockInApplicationService;
import com.example.scm.inventory.application.service.InventoryStockLockApplicationService;
import com.example.scm.inventory.application.service.InventoryStockOutApplicationService;
import com.example.scm.inventory.application.service.InventoryStockTransferApplicationService;
import com.example.scm.inventory.application.service.InventoryStocktakeApplicationService;
import com.example.scm.inventory.application.service.InventoryStockUnlockApplicationService;
import com.example.scm.inventory.application.service.InventoryTransactionRecordQueryService;
import com.example.scm.inventory.interfaces.assembler.InventoryStockAssembler;
import com.example.scm.inventory.interfaces.controller.InventoryStockController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @MockitoBean
    private InventoryStockInApplicationService inventoryStockInApplicationService;

    @MockitoBean
    private InventoryStockAdjustApplicationService inventoryStockAdjustApplicationService;

    @MockitoBean
    private InventoryStockLockApplicationService inventoryStockLockApplicationService;

    @MockitoBean
    private InventoryStockOutApplicationService inventoryStockOutApplicationService;

    @MockitoBean
    private InventoryLockedStockOutApplicationService inventoryLockedStockOutApplicationService;

    @MockitoBean
    private InventoryStockTransferApplicationService inventoryStockTransferApplicationService;

    @MockitoBean
    private InventoryStocktakeApplicationService inventoryStocktakeApplicationService;

    @MockitoBean
    private InventoryStockUnlockApplicationService inventoryStockUnlockApplicationService;

    @MockitoBean
    private InventoryBalanceQueryService inventoryBalanceQueryService;

    @MockitoBean
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
    void shouldAdjustSuccessfully() throws Exception {
        StockAdjustLineResultDTO line = new StockAdjustLineResultDTO();
        line.setTxnNo("ADJIN-TEST-001");
        line.setMaterialId(1001L);
        line.setWarehouseId(2001L);
        line.setLocationId(3001L);
        line.setQuantity(new BigDecimal("2"));
        line.setBeforeQty(new BigDecimal("10"));
        line.setAfterQty(new BigDecimal("12"));

        StockAdjustResultDTO result = new StockAdjustResultDTO();
        result.setBizType("MANUAL_ADJUST");
        result.setBizNo("ADJ-001");
        result.setAdjustType("INCREASE");
        result.setLines(List.of(line));
        when(inventoryStockAdjustApplicationService.adjust(any())).thenReturn(result);

        String requestBody = """
                {
                  "bizType":"MANUAL_ADJUST",
                  "bizNo":"ADJ-001",
                  "adjustType":"INCREASE",
                  "operatorId":1,
                  "items":[
                    {
                      "materialId":1001,
                      "warehouseId":2001,
                      "locationId":3001,
                      "quantity":2
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/inventory/adjustments")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bizNo").value("ADJ-001"))
                .andExpect(jsonPath("$.data.adjustType").value("INCREASE"))
                .andExpect(jsonPath("$.data.lines[0].txnNo").value("ADJIN-TEST-001"));
    }

    @Test
    void shouldStockOutSuccessfully() throws Exception {
        StockOutLineResultDTO line = new StockOutLineResultDTO();
        line.setTxnNo("OUT-TEST-001");
        line.setMaterialId(1001L);
        line.setWarehouseId(2001L);
        line.setLocationId(3001L);
        line.setQuantity(new BigDecimal("5"));
        line.setBeforeQty(new BigDecimal("10"));
        line.setAfterQty(new BigDecimal("5"));

        StockOutResultDTO result = new StockOutResultDTO();
        result.setBizType("SALES_ORDER");
        result.setBizNo("SO-001");
        result.setLines(List.of(line));
        when(inventoryStockOutApplicationService.stockOut(any())).thenReturn(result);

        String requestBody = """
                {
                  "bizType":"SALES_ORDER",
                  "bizNo":"SO-001",
                  "operatorId":1,
                  "items":[
                    {
                      "materialId":1001,
                      "warehouseId":2001,
                      "locationId":3001,
                      "quantity":5
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/inventory/stock-outs")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bizNo").value("SO-001"))
                .andExpect(jsonPath("$.data.lines[0].txnNo").value("OUT-TEST-001"));
    }

    @Test
    void shouldLockedStockOutSuccessfully() throws Exception {
        StockOutLineResultDTO line = new StockOutLineResultDTO();
        line.setTxnNo("OUT-LOCKED-TEST-001");
        line.setMaterialId(1001L);
        line.setWarehouseId(2001L);
        line.setLocationId(3001L);
        line.setQuantity(new BigDecimal("2"));
        line.setBeforeQty(new BigDecimal("10"));
        line.setAfterQty(new BigDecimal("8"));

        StockOutResultDTO result = new StockOutResultDTO();
        result.setBizType("SALES_ORDER");
        result.setBizNo("SO-LOCK-001");
        result.setLines(List.of(line));
        when(inventoryLockedStockOutApplicationService.stockOut(any())).thenReturn(result);

        String requestBody = """
                {
                  "bizType":"SALES_ORDER",
                  "bizNo":"SO-LOCK-001",
                  "operatorId":1,
                  "items":[
                    {
                      "materialId":1001,
                      "warehouseId":2001,
                      "locationId":3001,
                      "quantity":2
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/inventory/locked-stock-outs")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bizNo").value("SO-LOCK-001"))
                .andExpect(jsonPath("$.data.lines[0].txnNo").value("OUT-LOCKED-TEST-001"));
    }

    @Test
    void shouldTransferSuccessfully() throws Exception {
        StockTransferLineResultDTO line = new StockTransferLineResultDTO();
        line.setMoveOutTxnNo("MOVEOUT-TEST-001");
        line.setMoveInTxnNo("MOVEIN-TEST-001");
        line.setMaterialId(1001L);
        line.setFromWarehouseId(2001L);
        line.setFromLocationId(3001L);
        line.setToWarehouseId(2002L);
        line.setToLocationId(3002L);
        line.setQuantity(new BigDecimal("3"));
        line.setFromBeforeQty(new BigDecimal("10"));
        line.setFromAfterQty(new BigDecimal("7"));
        line.setToBeforeQty(new BigDecimal("1"));
        line.setToAfterQty(new BigDecimal("4"));

        StockTransferResultDTO result = new StockTransferResultDTO();
        result.setBizType("INVENTORY_TRANSFER");
        result.setBizNo("TRF-001");
        result.setLines(List.of(line));
        when(inventoryStockTransferApplicationService.transfer(any())).thenReturn(result);

        String requestBody = """
                {
                  "bizType":"INVENTORY_TRANSFER",
                  "bizNo":"TRF-001",
                  "operatorId":1,
                  "items":[
                    {
                      "materialId":1001,
                      "fromWarehouseId":2001,
                      "fromLocationId":3001,
                      "toWarehouseId":2002,
                      "toLocationId":3002,
                      "quantity":3
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/inventory/transfers")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bizNo").value("TRF-001"))
                .andExpect(jsonPath("$.data.lines[0].moveOutTxnNo").value("MOVEOUT-TEST-001"))
                .andExpect(jsonPath("$.data.lines[0].moveInTxnNo").value("MOVEIN-TEST-001"));
    }

    @Test
    void shouldStocktakeSuccessfully() throws Exception {
        StocktakeLineResultDTO line = new StocktakeLineResultDTO();
        line.setTxnNo("STKIN-TEST-001");
        line.setMaterialId(1001L);
        line.setWarehouseId(2001L);
        line.setLocationId(3001L);
        line.setSystemQty(new BigDecimal("10"));
        line.setCountedQty(new BigDecimal("12"));
        line.setVarianceQty(new BigDecimal("2"));
        line.setAdjustType("INCREASE");

        StocktakeResultDTO result = new StocktakeResultDTO();
        result.setBizType("STOCKTAKE");
        result.setBizNo("STK-001");
        result.setLines(List.of(line));
        when(inventoryStocktakeApplicationService.stocktake(any())).thenReturn(result);

        String requestBody = """
                {
                  "bizType":"STOCKTAKE",
                  "bizNo":"STK-001",
                  "operatorId":1,
                  "items":[
                    {
                      "materialId":1001,
                      "warehouseId":2001,
                      "locationId":3001,
                      "countedQty":12
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/inventory/stocktakes")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bizNo").value("STK-001"))
                .andExpect(jsonPath("$.data.lines[0].txnNo").value("STKIN-TEST-001"))
                .andExpect(jsonPath("$.data.lines[0].adjustType").value("INCREASE"));
    }

    @Test
    void shouldLockSuccessfully() throws Exception {
        StockLockLineResultDTO line = new StockLockLineResultDTO();
        line.setTxnNo("LOCK-TEST-001");
        line.setMaterialId(1001L);
        line.setWarehouseId(2001L);
        line.setLocationId(3001L);
        line.setQuantity(new BigDecimal("3"));
        line.setBeforeQty(BigDecimal.ZERO);
        line.setAfterQty(new BigDecimal("3"));

        StockLockResultDTO result = new StockLockResultDTO();
        result.setBizType("SALES_ORDER");
        result.setBizNo("SO-LOCK-001");
        result.setLines(List.of(line));
        when(inventoryStockLockApplicationService.lock(any())).thenReturn(result);

        String requestBody = """
                {
                  "bizType":"SALES_ORDER",
                  "bizNo":"SO-LOCK-001",
                  "operatorId":1,
                  "items":[
                    {
                      "materialId":1001,
                      "warehouseId":2001,
                      "locationId":3001,
                      "quantity":3
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/inventory/locks")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bizNo").value("SO-LOCK-001"))
                .andExpect(jsonPath("$.data.lines[0].txnNo").value("LOCK-TEST-001"));
    }

    @Test
    void shouldUnlockSuccessfully() throws Exception {
        StockUnlockLineResultDTO line = new StockUnlockLineResultDTO();
        line.setTxnNo("UNLOCK-TEST-001");
        line.setMaterialId(1001L);
        line.setWarehouseId(2001L);
        line.setLocationId(3001L);
        line.setQuantity(new BigDecimal("2"));
        line.setBeforeQty(new BigDecimal("3"));
        line.setAfterQty(new BigDecimal("1"));

        StockUnlockResultDTO result = new StockUnlockResultDTO();
        result.setBizType("SALES_ORDER");
        result.setBizNo("SO-LOCK-001");
        result.setLines(List.of(line));
        when(inventoryStockUnlockApplicationService.unlock(any())).thenReturn(result);

        String requestBody = """
                {
                  "bizType":"SALES_ORDER",
                  "bizNo":"SO-LOCK-001",
                  "operatorId":1,
                  "items":[
                    {
                      "materialId":1001,
                      "warehouseId":2001,
                      "locationId":3001,
                      "quantity":2
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/inventory/unlocks")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bizNo").value("SO-LOCK-001"))
                .andExpect(jsonPath("$.data.lines[0].txnNo").value("UNLOCK-TEST-001"));
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
