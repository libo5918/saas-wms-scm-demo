package com.example.scm.purchase;

import com.example.scm.purchase.client.InventoryStockInClient;
import com.example.scm.purchase.entity.PurchaseReceipt;
import com.example.scm.purchase.entity.PurchaseReceiptItem;
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

class InventoryStockInClientTest {

    @Test
    void shouldPostStockInRequestWithExpectedPayload() {
        InventoryStockInClient client = new InventoryStockInClient(new RestTemplateBuilder(), "http://inventory-service");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        PurchaseReceipt receipt = new PurchaseReceipt();
        receipt.setReceiptNo("RCV-2001");
        receipt.setWarehouseId(2001L);

        PurchaseReceiptItem item = new PurchaseReceiptItem();
        item.setMaterialId(1001L);
        item.setLocationId(3001L);
        item.setReceiptQty(new BigDecimal("12"));

        server.expect(requestTo("http://inventory-service/api/v1/inventory/stock-ins"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Tenant-Id", "1"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "bizType":"PURCHASE_RECEIPT",
                          "bizNo":"RCV-2001",
                          "operatorId":1,
                          "items":[
                            {
                              "materialId":1001,
                              "warehouseId":2001,
                              "locationId":3001,
                              "quantity":12
                            }
                          ]
                        }
                        """, true))
                .andRespond(withSuccess("""
                        {"success":true,"code":"200","message":"OK","data":null}
                        """, MediaType.APPLICATION_JSON));

        client.stockIn(1L, 1L, receipt, List.of(item));

        server.verify();
    }
}
