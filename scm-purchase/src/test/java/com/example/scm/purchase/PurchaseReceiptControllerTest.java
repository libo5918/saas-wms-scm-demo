package com.example.scm.purchase;

import com.example.scm.common.web.GlobalExceptionHandler;
import com.example.scm.common.web.TenantHeaderInterceptor;
import com.example.scm.common.web.WebMvcConfiguration;
import com.example.scm.purchase.controller.PurchaseReceiptController;
import com.example.scm.purchase.service.PurchaseReceiptService;
import com.example.scm.purchase.vo.PurchaseReceiptItemVO;
import com.example.scm.purchase.vo.PurchaseReceiptVO;
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

@WebMvcTest(PurchaseReceiptController.class)
@Import({GlobalExceptionHandler.class, TenantHeaderInterceptor.class, WebMvcConfiguration.class})
class PurchaseReceiptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PurchaseReceiptService purchaseReceiptService;

    @Test
    void shouldCreateRetryAndQueryReceipt() throws Exception {
        PurchaseReceiptItemVO item = new PurchaseReceiptItemVO();
        item.setId(11L);
        item.setMaterialId(1001L);
        item.setLocationId(3001L);
        item.setReceiptQty(new BigDecimal("12"));

        PurchaseReceiptVO receipt = new PurchaseReceiptVO();
        receipt.setId(1L);
        receipt.setReceiptNo("RCV-001");
        receipt.setPurchaseOrderId(5001L);
        receipt.setSupplierId(1L);
        receipt.setWarehouseId(2001L);
        receipt.setReceiptStatus("STOCK_IN_SUCCESS");
        receipt.setFailureReason(null);
        receipt.setItems(List.of(item));

        when(purchaseReceiptService.create(any())).thenReturn(receipt);
        when(purchaseReceiptService.retryStockIn(1L)).thenReturn(receipt);
        when(purchaseReceiptService.cancel(1L)).thenReturn(receipt);
        when(purchaseReceiptService.getById(1L)).thenReturn(receipt);
        when(purchaseReceiptService.getByReceiptNo("RCV-001")).thenReturn(receipt);
        when(purchaseReceiptService.list()).thenReturn(List.of(receipt));

        String requestBody = """
                {
                  "receiptNo":"RCV-001",
                  "purchaseOrderId":5001,
                  "supplierId":1,
                  "warehouseId":2001,
                  "items":[
                    {
                      "materialId":1001,
                      "locationId":3001,
                      "receiptQty":12
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/purchase-receipts")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.supplierId").value(1))
                .andExpect(jsonPath("$.data.receiptStatus").value("STOCK_IN_SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].materialId").value(1001));

        mockMvc.perform(post("/api/v1/purchase-receipts/1/retry-stock-in")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.receiptStatus").value("STOCK_IN_SUCCESS"));

        mockMvc.perform(post("/api/v1/purchase-receipts/1/cancel")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        mockMvc.perform(get("/api/v1/purchase-receipts/1")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.receiptNo").value("RCV-001"));

        mockMvc.perform(get("/api/v1/purchase-receipts/by-receipt-no")
                        .header("X-Tenant-Id", "1")
                        .param("receiptNo", "RCV-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.receiptNo").value("RCV-001"));

        mockMvc.perform(get("/api/v1/purchase-receipts")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].receiptNo").value("RCV-001"));
    }
}
