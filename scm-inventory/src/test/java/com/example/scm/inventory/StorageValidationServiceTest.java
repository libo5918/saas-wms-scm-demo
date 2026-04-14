package com.example.scm.inventory;

import com.example.scm.inventory.application.service.StorageValidationService;
import com.example.scm.inventory.integration.LocationClient;
import com.example.scm.inventory.integration.WarehouseClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StorageValidationServiceTest {

    @Mock
    private WarehouseClient warehouseClient;
    @Mock
    private LocationClient locationClient;

    @InjectMocks
    private StorageValidationService storageValidationService;

    @Test
    void shouldValidateWarehouseAndLocation() {
        storageValidationService.validateStorageEnabled(1L, 2001L, 3001L);

        verify(warehouseClient).validateWarehouseEnabled(1L, 2001L);
        verify(locationClient).validateLocationEnabled(1L, 2001L, 3001L);
    }
}
