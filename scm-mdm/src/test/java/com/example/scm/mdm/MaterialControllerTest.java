package com.example.scm.mdm;

import com.example.scm.common.web.GlobalExceptionHandler;
import com.example.scm.common.web.TenantHeaderInterceptor;
import com.example.scm.common.web.WebMvcConfiguration;
import com.example.scm.mdm.controller.MaterialController;
import com.example.scm.mdm.service.MaterialService;
import com.example.scm.mdm.vo.MaterialVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaterialController.class)
@Import({GlobalExceptionHandler.class, TenantHeaderInterceptor.class, WebMvcConfiguration.class})
class MaterialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaterialService materialService;

    @Test
    void shouldCreateAndQueryMaterial() throws Exception {
        MaterialVO material = new MaterialVO();
        material.setId(1L);
        material.setMaterialCode("MAT-TEST-001");
        material.setMaterialName("TEST-MATERIAL");
        material.setUnit("PCS");
        material.setMaterialType("RAW");
        material.setStatus(1);

        when(materialService.create(any())).thenReturn(material);
        when(materialService.getById(1L)).thenReturn(material);
        when(materialService.list()).thenReturn(List.of(material));

        String requestBody = """
                {
                  "materialCode":"MAT-TEST-001",
                  "materialName":"TEST-MATERIAL",
                  "unit":"PCS",
                  "materialType":"RAW",
                  "status":1
                }
                """;

        mockMvc.perform(post("/api/v1/materials")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.materialCode").value("MAT-TEST-001"));

        mockMvc.perform(get("/api/v1/materials/1")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.materialName").value("TEST-MATERIAL"));

        mockMvc.perform(get("/api/v1/materials")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].materialCode").value("MAT-TEST-001"));
    }

    @Test
    void shouldReturnBadRequestWhenMaterialCodeMissing() throws Exception {
        String requestBody = """
                {
                  "materialName":"TEST-MATERIAL",
                  "unit":"PCS",
                  "materialType":"RAW",
                  "status":1
                }
                """;

        mockMvc.perform(post("/api/v1/materials")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));
    }
}
