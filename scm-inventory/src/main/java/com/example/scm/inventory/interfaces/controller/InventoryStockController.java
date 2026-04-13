package com.example.scm.inventory.interfaces.controller;

import com.example.scm.common.core.Result;
import com.example.scm.inventory.application.query.InventoryBalanceDTO;
import com.example.scm.inventory.application.query.InventoryTransactionRecordDTO;
import com.example.scm.inventory.application.query.StockInResultDTO;
import com.example.scm.inventory.application.service.InventoryBalanceQueryService;
import com.example.scm.inventory.application.service.InventoryStockInApplicationService;
import com.example.scm.inventory.application.service.InventoryTransactionRecordQueryService;
import com.example.scm.inventory.interfaces.assembler.InventoryStockAssembler;
import com.example.scm.inventory.interfaces.dto.StockInRequest;
import com.example.scm.inventory.interfaces.vo.InventoryBalanceVO;
import com.example.scm.inventory.interfaces.vo.InventoryTransactionRecordVO;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory-Stock")
@Slf4j
public class InventoryStockController {

    private final InventoryStockInApplicationService inventoryStockInApplicationService;
    private final InventoryBalanceQueryService inventoryBalanceQueryService;
    private final InventoryTransactionRecordQueryService inventoryTransactionRecordQueryService;
    private final InventoryStockAssembler inventoryStockAssembler;

    public InventoryStockController(InventoryStockInApplicationService inventoryStockInApplicationService,
                                    InventoryBalanceQueryService inventoryBalanceQueryService,
                                    InventoryTransactionRecordQueryService inventoryTransactionRecordQueryService,
                                    InventoryStockAssembler inventoryStockAssembler) {
        this.inventoryStockInApplicationService = inventoryStockInApplicationService;
        this.inventoryBalanceQueryService = inventoryBalanceQueryService;
        this.inventoryTransactionRecordQueryService = inventoryTransactionRecordQueryService;
        this.inventoryStockAssembler = inventoryStockAssembler;
    }

    @PostMapping("/stock-ins")
    @Operation(summary = "执行库存入库", description = "根据业务单执行库存入库并生成库存流水。")
    public Result<StockInResultVO> stockIn(@Valid @RequestBody StockInRequest request) {
        log.info("Receive stock-in request, bizType={}, bizNo={}, itemCount={}",
                request.getBizType(), request.getBizNo(), request.getItems().size());
        StockInResultDTO result = inventoryStockInApplicationService.stockIn(inventoryStockAssembler.toCommand(request));
        return Result.success(inventoryStockAssembler.toResultVO(result));
    }

    @GetMapping("/balances")
    @Operation(summary = "查询库存余额", description = "根据物料、仓库和库位查询当前库存余额与版本号。")
    public Result<InventoryBalanceVO> getBalance(@RequestParam("materialId") Long materialId,
                                                 @RequestParam("warehouseId") Long warehouseId,
                                                 @RequestParam("locationId") Long locationId) {
        log.info("Receive inventory balance query, materialId={}, warehouseId={}, locationId={}",
                materialId, warehouseId, locationId);
        InventoryBalanceDTO dto = inventoryBalanceQueryService.getBalance(materialId, warehouseId, locationId);
        return Result.success(inventoryStockAssembler.toBalanceVO(dto));
    }

    @GetMapping("/txn-records")
    @Operation(summary = "按业务单查询库存流水", description = "根据业务类型和业务单号查询库存流水记录。")
    public Result<List<InventoryTransactionRecordVO>> listTxnRecords(@RequestParam("bizType") String bizType,
                                                                     @RequestParam("bizNo") String bizNo) {
        log.info("Receive inventory transaction record query, bizType={}, bizNo={}", bizType, bizNo);
        List<InventoryTransactionRecordDTO> records = inventoryTransactionRecordQueryService.listByBizNo(bizType, bizNo);
        return Result.success(records.stream().map(inventoryStockAssembler::toTxnRecordVO).toList());
    }
}
