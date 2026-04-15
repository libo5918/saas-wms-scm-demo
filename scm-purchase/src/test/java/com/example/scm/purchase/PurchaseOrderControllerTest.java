package com.example.scm.purchase;

import com.example.scm.common.web.GlobalExceptionHandler;
import com.example.scm.common.web.TenantHeaderInterceptor;
import com.example.scm.common.web.WebMvcConfiguration;
import com.example.scm.purchase.controller.PurchaseOrderController;
import com.example.scm.purchase.service.PurchaseOrderService;
import com.example.scm.purchase.vo.PurchaseOrderItemVO;
import com.example.scm.purchase.vo.PurchaseOrderVO;
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

@WebMvcTest(PurchaseOrderController.class)
@Import({GlobalExceptionHandler.class, TenantHeaderInterceptor.class, WebMvcConfiguration.class})
class PurchaseOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PurchaseOrderService purchaseOrderService;

    @Test
    void shouldCreateAndQueryPurchaseOrder() throws Exception {
        PurchaseOrderItemVO item = new PurchaseOrderItemVO();
        item.setId(11L);
        item.setMaterialId(1L);
        item.setPlanQty(new BigDecimal("20"));
        item.setReceivedQty(BigDecimal.ZERO);
        item.setUnitPrice(new BigDecimal("10"));

        PurchaseOrderVO order = new PurchaseOrderVO();
        order.setId(1L);
        order.setOrderNo("PO-001");
        order.setSupplierId(1L);
        order.setOrderStatus("CREATED");
        order.setTotalAmount(new BigDecimal("200"));
        order.setRemark("首单采购");
        order.setItems(List.of(item));

        when(purchaseOrderService.create(any())).thenReturn(order);
        when(purchaseOrderService.getById(1L)).thenReturn(order);
        when(purchaseOrderService.getByOrderNo("PO-001")).thenReturn(order);
        when(purchaseOrderService.list()).thenReturn(List.of(order));
        when(purchaseOrderService.cancel(1L)).thenReturn(order);

        String requestBody = """
                {
                  "orderNo":"PO-001",
                  "supplierId":1,
                  "remark":"首单采购",
                  "items":[
                    {
                      "materialId":1,
                      "planQty":20,
                      "unitPrice":10
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/purchase-orders")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.supplierId").value(1))
                .andExpect(jsonPath("$.data.orderStatus").value("CREATED"))
                .andExpect(jsonPath("$.data.items[0].materialId").value(1));

        mockMvc.perform(get("/api/v1/purchase-orders/1")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNo").value("PO-001"));

        mockMvc.perform(get("/api/v1/purchase-orders/by-order-no")
                        .header("X-Tenant-Id", "1")
                        .param("orderNo", "PO-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNo").value("PO-001"));

        mockMvc.perform(get("/api/v1/purchase-orders")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].orderNo").value("PO-001"));

        mockMvc.perform(post("/api/v1/purchase-orders/1/cancel")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNo").value("PO-001"));
    }
}
