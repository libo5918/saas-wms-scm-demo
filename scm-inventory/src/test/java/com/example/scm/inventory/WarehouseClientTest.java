package com.example.scm.inventory;

import com.example.scm.common.core.BusinessException;
import com.example.scm.inventory.integration.WarehouseClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WarehouseClientTest {

    @Test
    void shouldValidateEnabledWarehouse() {
        WarehouseClient warehouseClient = new WarehouseClient(new RestTemplateBuilder(), "http://127.0.0.1:18082");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(warehouseClient, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(once(), requestTo("http://127.0.0.1:18082/api/v1/warehouses/2001"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Tenant-Id", "1"))
                .andRespond(withSuccess("""
                        {"success":true,"code":"200","message":"OK","data":{"id":2001,"status":1}}
                        """, MediaType.APPLICATION_JSON));

        warehouseClient.validateWarehouseEnabled(1L, 2001L);

        server.verify();
    }

    @Test
    void shouldRejectDisabledWarehouse() {
        WarehouseClient warehouseClient = new WarehouseClient(new RestTemplateBuilder(), "http://127.0.0.1:18082");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(warehouseClient, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(once(), requestTo("http://127.0.0.1:18082/api/v1/warehouses/2001"))
                .andRespond(withSuccess("""
                        {"success":true,"code":"200","message":"OK","data":{"id":2001,"status":0}}
                        """, MediaType.APPLICATION_JSON));

        assertThrows(BusinessException.class, () -> warehouseClient.validateWarehouseEnabled(1L, 2001L));

        server.verify();
    }
}
