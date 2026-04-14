package com.example.scm.mdm;

import com.example.scm.common.web.GlobalExceptionHandler;
import com.example.scm.common.web.TenantHeaderInterceptor;
import com.example.scm.common.web.WebMvcConfiguration;
import com.example.scm.mdm.controller.WarehouseController;
import com.example.scm.mdm.service.WarehouseService;
import com.example.scm.mdm.vo.WarehouseVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WarehouseController.class)
@Import({GlobalExceptionHandler.class, TenantHeaderInterceptor.class, WebMvcConfiguration.class})
class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WarehouseService warehouseService;

    @Test
    void shouldCreateAndQueryWarehouse() throws Exception {
        WarehouseVO warehouse = new WarehouseVO();
        warehouse.setId(1L);
        warehouse.setWarehouseCode("WH-TEST-001");
        warehouse.setWarehouseName("测试仓");
        warehouse.setWarehouseType("FINISHED");
        warehouse.setStatus(1);

        when(warehouseService.create(any())).thenReturn(warehouse);
        when(warehouseService.getById(1L)).thenReturn(warehouse);
        when(warehouseService.list()).thenReturn(List.of(warehouse));

        String requestBody = """
                {
                  "warehouseCode":"WH-TEST-001",
                  "warehouseName":"测试仓",
                  "warehouseType":"FINISHED",
                  "status":1
                }
                """;

        mockMvc.perform(post("/api/v1/warehouses")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.warehouseCode").value("WH-TEST-001"));

        mockMvc.perform(get("/api/v1/warehouses/1")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.warehouseName").value("测试仓"));

        mockMvc.perform(get("/api/v1/warehouses")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].warehouseCode").value("WH-TEST-001"));
    }

    @Test
    void shouldReturnBadRequestWhenWarehouseCodeMissing() throws Exception {
        String requestBody = """
                {
                  "warehouseName":"测试仓",
                  "status":1
                }
                """;

        mockMvc.perform(post("/api/v1/warehouses")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));
    }
}
