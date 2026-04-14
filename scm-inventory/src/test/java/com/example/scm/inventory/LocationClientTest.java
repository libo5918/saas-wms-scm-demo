package com.example.scm.inventory;

import com.example.scm.common.core.BusinessException;
import com.example.scm.inventory.integration.LocationClient;
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

class LocationClientTest {

    @Test
    void shouldValidateEnabledLocationBelongsToWarehouse() {
        LocationClient locationClient = new LocationClient(new RestTemplateBuilder(), "http://127.0.0.1:18082");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(locationClient, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(once(), requestTo("http://127.0.0.1:18082/api/v1/locations/3001"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Tenant-Id", "1"))
                .andRespond(withSuccess("""
                        {"success":true,"code":"200","message":"OK","data":{"id":3001,"warehouseId":2001,"status":1}}
                        """, MediaType.APPLICATION_JSON));

        locationClient.validateLocationEnabled(1L, 2001L, 3001L);

        server.verify();
    }

    @Test
    void shouldRejectLocationFromAnotherWarehouse() {
        LocationClient locationClient = new LocationClient(new RestTemplateBuilder(), "http://127.0.0.1:18082");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(locationClient, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(once(), requestTo("http://127.0.0.1:18082/api/v1/locations/3001"))
                .andRespond(withSuccess("""
                        {"success":true,"code":"200","message":"OK","data":{"id":3001,"warehouseId":2002,"status":1}}
                        """, MediaType.APPLICATION_JSON));

        assertThrows(BusinessException.class, () -> locationClient.validateLocationEnabled(1L, 2001L, 3001L));

        server.verify();
    }
}
