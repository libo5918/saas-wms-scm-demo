package com.example.scm.inventory.interfaces.controller;

import com.example.scm.common.core.Result;
import com.example.scm.inventory.application.query.InventoryBalanceDTO;
import com.example.scm.inventory.application.query.StockInResultDTO;
import com.example.scm.inventory.application.service.InventoryBalanceQueryService;
import com.example.scm.inventory.application.service.InventoryStockInApplicationService;
import com.example.scm.inventory.interfaces.assembler.InventoryStockAssembler;
import com.example.scm.inventory.interfaces.dto.StockInRequest;
import com.example.scm.inventory.interfaces.vo.InventoryBalanceVO;
import com.example.scm.inventory.interfaces.vo.StockInResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory-Stock")
@Slf4j
public class InventoryStockController {

    private final InventoryStockInApplicationService inventoryStockInApplicationService;
    private final InventoryBalanceQueryService inventoryBalanceQueryService;
    private final InventoryStockAssembler inventoryStockAssembler;

    public InventoryStockController(InventoryStockInApplicationService inventoryStockInApplicationService,
                                    InventoryBalanceQueryService inventoryBalanceQueryService,
                                    InventoryStockAssembler inventoryStockAssembler) {
        this.inventoryStockInApplicationService = inventoryStockInApplicationService;
        this.inventoryBalanceQueryService = inventoryBalanceQueryService;
        this.inventoryStockAssembler = inventoryStockAssembler;
    }

    @PostMapping("/stock-ins")
    @Operation(summary = "Execute stock-in", description = "Increase stock by business order and generate inventory transaction records.")
    public Result<StockInResultVO> stockIn(@Valid @RequestBody StockInRequest request) {
        log.info("Receive stock-in request, bizType={}, bizNo={}, itemCount={}",
                request.getBizType(), request.getBizNo(), request.getItems().size());
        StockInResultDTO result = inventoryStockInApplicationService.stockIn(inventoryStockAssembler.toCommand(request));
        return Result.success(inventoryStockAssembler.toResultVO(result));
    }

    @GetMapping("/balances")
    @Operation(summary = "Query inventory balance", description = "Query available stock and version by material, warehouse and location.")
    public Result<InventoryBalanceVO> getBalance(@RequestParam("materialId") Long materialId,
                                                 @RequestParam("warehouseId") Long warehouseId,
                                                 @RequestParam("locationId") Long locationId) {
        log.info("Receive inventory balance query, materialId={}, warehouseId={}, locationId={}",
                materialId, warehouseId, locationId);
        InventoryBalanceDTO dto = inventoryBalanceQueryService.getBalance(materialId, warehouseId, locationId);
        return Result.success(inventoryStockAssembler.toBalanceVO(dto));
    }
}
