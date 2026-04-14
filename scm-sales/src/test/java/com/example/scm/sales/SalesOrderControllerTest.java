package com.example.scm.sales;

import com.example.scm.common.web.GlobalExceptionHandler;
import com.example.scm.common.web.TenantHeaderInterceptor;
import com.example.scm.common.web.WebMvcConfiguration;
import com.example.scm.sales.controller.SalesOrderController;
import com.example.scm.sales.service.SalesOrderService;
import com.example.scm.sales.vo.SalesOrderItemVO;
import com.example.scm.sales.vo.SalesOrderVO;
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

@WebMvcTest(SalesOrderController.class)
@Import({GlobalExceptionHandler.class, TenantHeaderInterceptor.class, WebMvcConfiguration.class})
class SalesOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SalesOrderService salesOrderService;

    @Test
    void shouldCreateRetryAndQueryOrder() throws Exception {
        SalesOrderItemVO item = new SalesOrderItemVO();
        item.setId(11L);
        item.setMaterialId(1L);
        item.setLocationId(3001L);
        item.setSaleQty(new BigDecimal("2"));

        SalesOrderVO order = new SalesOrderVO();
        order.setId(1L);
        order.setOrderNo("SO-001");
        order.setWarehouseId(2001L);
        order.setOrderStatus("LOCK_SUCCESS");
        order.setItems(List.of(item));

        when(salesOrderService.create(any())).thenReturn(order);
        when(salesOrderService.retryLock(1L)).thenReturn(order);
        when(salesOrderService.ship(1L)).thenReturn(order);
        when(salesOrderService.retryShip(1L)).thenReturn(order);
        when(salesOrderService.cancel(1L)).thenReturn(order);
        when(salesOrderService.getById(1L)).thenReturn(order);
        when(salesOrderService.getByOrderNo("SO-001")).thenReturn(order);
        when(salesOrderService.list()).thenReturn(List.of(order));

        String requestBody = """
                {
                  "orderNo":"SO-001",
                  "warehouseId":2001,
                  "items":[
                    {
                      "materialId":1,
                      "locationId":3001,
                      "saleQty":2
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/sales-orders")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNo").value("SO-001"));

        mockMvc.perform(post("/api/v1/sales-orders/1/retry-lock")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").value("SO-001"));

        mockMvc.perform(post("/api/v1/sales-orders/1/ship")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").value("SO-001"));

        mockMvc.perform(post("/api/v1/sales-orders/1/retry-ship")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").value("SO-001"));

        mockMvc.perform(post("/api/v1/sales-orders/1/cancel")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").value("SO-001"));

        mockMvc.perform(get("/api/v1/sales-orders/1")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").value("SO-001"));

        mockMvc.perform(get("/api/v1/sales-orders/by-order-no")
                        .header("X-Tenant-Id", "1")
                        .param("orderNo", "SO-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").value("SO-001"));

        mockMvc.perform(get("/api/v1/sales-orders")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].orderNo").value("SO-001"));
    }
}
