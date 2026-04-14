package com.example.scm.mdm;

import com.example.scm.common.web.GlobalExceptionHandler;
import com.example.scm.common.web.TenantHeaderInterceptor;
import com.example.scm.common.web.WebMvcConfiguration;
import com.example.scm.mdm.controller.LocationController;
import com.example.scm.mdm.service.LocationService;
import com.example.scm.mdm.vo.LocationVO;
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

@WebMvcTest(LocationController.class)
@Import({GlobalExceptionHandler.class, TenantHeaderInterceptor.class, WebMvcConfiguration.class})
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationService locationService;

    @Test
    void shouldCreateAndQueryLocation() throws Exception {
        LocationVO location = new LocationVO();
        location.setId(1L);
        location.setWarehouseId(1L);
        location.setLocationCode("LOC-TEST-001");
        location.setLocationName("测试库位");
        location.setLocationType("PICK");
        location.setStatus(1);

        when(locationService.create(any())).thenReturn(location);
        when(locationService.getById(1L)).thenReturn(location);
        when(locationService.list(null)).thenReturn(List.of(location));

        String requestBody = """
                {
                  "warehouseId":1,
                  "locationCode":"LOC-TEST-001",
                  "locationName":"测试库位",
                  "locationType":"PICK",
                  "status":1
                }
                """;

        mockMvc.perform(post("/api/v1/locations")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.locationCode").value("LOC-TEST-001"));

        mockMvc.perform(get("/api/v1/locations/1")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.locationName").value("测试库位"));

        mockMvc.perform(get("/api/v1/locations")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].locationCode").value("LOC-TEST-001"));
    }

    @Test
    void shouldReturnBadRequestWhenWarehouseIdMissing() throws Exception {
        String requestBody = """
                {
                  "locationCode":"LOC-TEST-001",
                  "locationName":"测试库位",
                  "status":1
                }
                """;

        mockMvc.perform(post("/api/v1/locations")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));
    }
}
