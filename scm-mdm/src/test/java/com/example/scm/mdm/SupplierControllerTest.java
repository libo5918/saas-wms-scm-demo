package com.example.scm.mdm;

import com.example.scm.common.web.GlobalExceptionHandler;
import com.example.scm.common.web.TenantHeaderInterceptor;
import com.example.scm.common.web.WebMvcConfiguration;
import com.example.scm.mdm.controller.SupplierController;
import com.example.scm.mdm.service.SupplierService;
import com.example.scm.mdm.vo.SupplierVO;
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

@WebMvcTest(SupplierController.class)
@Import({GlobalExceptionHandler.class, TenantHeaderInterceptor.class, WebMvcConfiguration.class})
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupplierService supplierService;

    @Test
    void shouldCreateAndQuerySupplier() throws Exception {
        SupplierVO supplier = new SupplierVO();
        supplier.setId(1L);
        supplier.setSupplierCode("SUP-001");
        supplier.setSupplierName("测试供应商");
        supplier.setStatus(1);

        when(supplierService.create(any())).thenReturn(supplier);
        when(supplierService.getById(1L)).thenReturn(supplier);
        when(supplierService.list()).thenReturn(List.of(supplier));

        String requestBody = """
                {
                  "supplierCode":"SUP-001",
                  "supplierName":"测试供应商",
                  "contactName":"张三",
                  "contactPhone":"13800000001",
                  "status":1
                }
                """;

        mockMvc.perform(post("/api/v1/suppliers")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.supplierCode").value("SUP-001"));

        mockMvc.perform(get("/api/v1/suppliers/1")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.supplierName").value("测试供应商"));

        mockMvc.perform(get("/api/v1/suppliers")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].supplierCode").value("SUP-001"));
    }
}
