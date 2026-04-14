package com.example.scm.sales;

import com.example.scm.sales.client.InventoryReservationClient;
import com.example.scm.sales.entity.SalesOrder;
import com.example.scm.sales.entity.SalesOrderItem;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InventoryReservationClientTest {

    @Test
    void shouldPostLockRequestWithExpectedPayload() {
        InventoryReservationClient client = new InventoryReservationClient(new RestTemplateBuilder(), "http://inventory-service");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        SalesOrder order = buildOrder();
        SalesOrderItem item = buildItem();

        server.expect(requestTo("http://inventory-service/api/v1/inventory/locks"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Tenant-Id", "1"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedPayload(), true))
                .andRespond(withSuccess("""
                        {"success":true,"code":"200","message":"OK","data":null}
                        """, MediaType.APPLICATION_JSON));

        client.lock(1L, 1L, order, List.of(item));

        server.verify();
    }

    @Test
    void shouldPostLockedStockOutRequestWithExpectedPayload() {
        InventoryReservationClient client = new InventoryReservationClient(new RestTemplateBuilder(), "http://inventory-service");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        server.expect(requestTo("http://inventory-service/api/v1/inventory/locked-stock-outs"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Tenant-Id", "1"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedPayload(), true))
                .andRespond(withSuccess("""
                        {"success":true,"code":"200","message":"OK","data":null}
                        """, MediaType.APPLICATION_JSON));

        client.stockOut(1L, 1L, buildOrder(), List.of(buildItem()));

        server.verify();
    }

    @Test
    void shouldPostUnlockRequestWithExpectedPayload() {
        InventoryReservationClient client = new InventoryReservationClient(new RestTemplateBuilder(), "http://inventory-service");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        server.expect(requestTo("http://inventory-service/api/v1/inventory/unlocks"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Tenant-Id", "1"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedPayload(), true))
                .andRespond(withSuccess("""
                        {"success":true,"code":"200","message":"OK","data":null}
                        """, MediaType.APPLICATION_JSON));

        client.unlock(1L, 1L, buildOrder(), List.of(buildItem()));

        server.verify();
    }

    private SalesOrder buildOrder() {
        SalesOrder order = new SalesOrder();
        order.setOrderNo("SO-2001");
        order.setWarehouseId(2001L);
        return order;
    }

    private SalesOrderItem buildItem() {
        SalesOrderItem item = new SalesOrderItem();
        item.setMaterialId(1001L);
        item.setLocationId(3001L);
        item.setSaleQty(new BigDecimal("5"));
        return item;
    }

    private String expectedPayload() {
        return """
                {
                  "bizType":"SALES_ORDER",
                  "bizNo":"SO-2001",
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
    }
}
