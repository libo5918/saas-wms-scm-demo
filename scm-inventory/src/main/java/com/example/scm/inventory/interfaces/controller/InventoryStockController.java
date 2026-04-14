package com.example.scm.inventory.interfaces.controller;

import com.example.scm.common.core.Result;
import com.example.scm.inventory.application.query.InventoryBalanceDTO;
import com.example.scm.inventory.application.query.InventoryTransactionRecordDTO;
import com.example.scm.inventory.application.query.StockAdjustResultDTO;
import com.example.scm.inventory.application.query.StockInResultDTO;
import com.example.scm.inventory.application.query.StockLockResultDTO;
import com.example.scm.inventory.application.query.StockOutResultDTO;
import com.example.scm.inventory.application.query.StockTransferResultDTO;
import com.example.scm.inventory.application.query.StocktakeResultDTO;
import com.example.scm.inventory.application.query.StockUnlockResultDTO;
import com.example.scm.inventory.application.service.InventoryBalanceQueryService;
import com.example.scm.inventory.application.service.InventoryLockedStockOutApplicationService;
import com.example.scm.inventory.application.service.InventoryStockAdjustApplicationService;
import com.example.scm.inventory.application.service.InventoryStockInApplicationService;
import com.example.scm.inventory.application.service.InventoryStockLockApplicationService;
import com.example.scm.inventory.application.service.InventoryStockOutApplicationService;
import com.example.scm.inventory.application.service.InventoryStockTransferApplicationService;
import com.example.scm.inventory.application.service.InventoryStocktakeApplicationService;
import com.example.scm.inventory.application.service.InventoryStockUnlockApplicationService;
import com.example.scm.inventory.application.service.InventoryTransactionRecordQueryService;
import com.example.scm.inventory.interfaces.assembler.InventoryStockAssembler;
import com.example.scm.inventory.interfaces.dto.StockAdjustRequest;
import com.example.scm.inventory.interfaces.dto.StockInRequest;
import com.example.scm.inventory.interfaces.dto.StockLockRequest;
import com.example.scm.inventory.interfaces.dto.StockOutRequest;
import com.example.scm.inventory.interfaces.dto.StockTransferRequest;
import com.example.scm.inventory.interfaces.dto.StocktakeRequest;
import com.example.scm.inventory.interfaces.dto.StockUnlockRequest;
import com.example.scm.inventory.interfaces.vo.InventoryBalanceVO;
import com.example.scm.inventory.interfaces.vo.InventoryTransactionRecordVO;
import com.example.scm.inventory.interfaces.vo.StockAdjustResultVO;
import com.example.scm.inventory.interfaces.vo.StockInResultVO;
import com.example.scm.inventory.interfaces.vo.StockLockResultVO;
import com.example.scm.inventory.interfaces.vo.StockOutResultVO;
import com.example.scm.inventory.interfaces.vo.StockTransferResultVO;
import com.example.scm.inventory.interfaces.vo.StocktakeResultVO;
import com.example.scm.inventory.interfaces.vo.StockUnlockResultVO;
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
@Tag(name = "库存服务", description = "库存入库、出库、锁库、解锁、调整、移库、盘点与查询接口。")
@Slf4j
public class InventoryStockController {

    private final InventoryStockInApplicationService inventoryStockInApplicationService;
    private final InventoryStockAdjustApplicationService inventoryStockAdjustApplicationService;
    private final InventoryStockLockApplicationService inventoryStockLockApplicationService;
    private final InventoryLockedStockOutApplicationService inventoryLockedStockOutApplicationService;
    private final InventoryStockOutApplicationService inventoryStockOutApplicationService;
    private final InventoryStockTransferApplicationService inventoryStockTransferApplicationService;
    private final InventoryStocktakeApplicationService inventoryStocktakeApplicationService;
    private final InventoryStockUnlockApplicationService inventoryStockUnlockApplicationService;
    private final InventoryBalanceQueryService inventoryBalanceQueryService;
    private final InventoryTransactionRecordQueryService inventoryTransactionRecordQueryService;
    private final InventoryStockAssembler inventoryStockAssembler;

    public InventoryStockController(InventoryStockInApplicationService inventoryStockInApplicationService,
                                    InventoryStockAdjustApplicationService inventoryStockAdjustApplicationService,
                                    InventoryStockLockApplicationService inventoryStockLockApplicationService,
                                    InventoryLockedStockOutApplicationService inventoryLockedStockOutApplicationService,
                                    InventoryStockOutApplicationService inventoryStockOutApplicationService,
                                    InventoryStockTransferApplicationService inventoryStockTransferApplicationService,
                                    InventoryStocktakeApplicationService inventoryStocktakeApplicationService,
                                    InventoryStockUnlockApplicationService inventoryStockUnlockApplicationService,
                                    InventoryBalanceQueryService inventoryBalanceQueryService,
                                    InventoryTransactionRecordQueryService inventoryTransactionRecordQueryService,
                                    InventoryStockAssembler inventoryStockAssembler) {
        this.inventoryStockInApplicationService = inventoryStockInApplicationService;
        this.inventoryStockAdjustApplicationService = inventoryStockAdjustApplicationService;
        this.inventoryStockLockApplicationService = inventoryStockLockApplicationService;
        this.inventoryLockedStockOutApplicationService = inventoryLockedStockOutApplicationService;
        this.inventoryStockOutApplicationService = inventoryStockOutApplicationService;
        this.inventoryStockTransferApplicationService = inventoryStockTransferApplicationService;
        this.inventoryStocktakeApplicationService = inventoryStocktakeApplicationService;
        this.inventoryStockUnlockApplicationService = inventoryStockUnlockApplicationService;
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

    /**
     * 直接库存调整接口。
     * 适用于调用方已经明确知道调整方向和调整数量的场景，
     * 例如人工修正、审批后的盘盈盘亏处理、外部系统直接下发调账结果。
     */
    @PostMapping("/adjustments")
    @Operation(summary = "执行库存调整", description = "针对盘盈盘亏或手工修正执行库存调整。")
    public Result<StockAdjustResultVO> adjust(@Valid @RequestBody StockAdjustRequest request) {
        log.info("Receive stock-adjust request, bizType={}, bizNo={}, adjustType={}, itemCount={}",
                request.getBizType(), request.getBizNo(), request.getAdjustType(), request.getItems().size());
        StockAdjustResultDTO result = inventoryStockAdjustApplicationService.adjust(inventoryStockAssembler.toCommand(request));
        return Result.success(inventoryStockAssembler.toResultVO(result));
    }

    @PostMapping("/locks")
    @Operation(summary = "执行库存锁定", description = "根据业务单锁定可用库存并生成库存流水。")
    public Result<StockLockResultVO> lock(@Valid @RequestBody StockLockRequest request) {
        log.info("Receive stock-lock request, bizType={}, bizNo={}, itemCount={}",
                request.getBizType(), request.getBizNo(), request.getItems().size());
        StockLockResultDTO result = inventoryStockLockApplicationService.lock(inventoryStockAssembler.toCommand(request));
        return Result.success(inventoryStockAssembler.toResultVO(result));
    }

    @PostMapping("/stock-outs")
    @Operation(summary = "执行库存出库", description = "根据业务单直接扣减现存和可用库存，并生成库存流水。")
    public Result<StockOutResultVO> stockOut(@Valid @RequestBody StockOutRequest request) {
        log.info("Receive stock-out request, bizType={}, bizNo={}, itemCount={}",
                request.getBizType(), request.getBizNo(), request.getItems().size());
        StockOutResultDTO result = inventoryStockOutApplicationService.stockOut(inventoryStockAssembler.toCommand(request));
        return Result.success(inventoryStockAssembler.toResultVO(result));
    }

    @PostMapping("/locked-stock-outs")
    @Operation(summary = "执行锁定库存出库", description = "针对已锁库业务消耗锁定库存并完成发货出库。")
    public Result<StockOutResultVO> lockedStockOut(@Valid @RequestBody StockOutRequest request) {
        log.info("Receive locked stock-out request, bizType={}, bizNo={}, itemCount={}",
                request.getBizType(), request.getBizNo(), request.getItems().size());
        StockOutResultDTO result = inventoryLockedStockOutApplicationService.stockOut(inventoryStockAssembler.toCommand(request));
        return Result.success(inventoryStockAssembler.toResultVO(result));
    }

    @PostMapping("/transfers")
    @Operation(summary = "执行库存移库", description = "将库存从源仓位转移到目标仓位，并生成移出、移入两条流水。")
    public Result<StockTransferResultVO> transfer(@Valid @RequestBody StockTransferRequest request) {
        log.info("Receive stock-transfer request, bizType={}, bizNo={}, itemCount={}",
                request.getBizType(), request.getBizNo(), request.getItems().size());
        StockTransferResultDTO result = inventoryStockTransferApplicationService.transfer(inventoryStockAssembler.toCommand(request));
        return Result.success(inventoryStockAssembler.toResultVO(result));
    }

    /**
     * 库存盘点接口。
     * 适用于调用方只掌握实盘数量的场景，
     * 系统会自动比较账面库存并推导出调整方向与差异数量。
     */
    @PostMapping("/stocktakes")
    @Operation(summary = "执行库存盘点", description = "按盘点结果对库存差异执行调整，无差异时仅返回盘点结果。")
    public Result<StocktakeResultVO> stocktake(@Valid @RequestBody StocktakeRequest request) {
        log.info("Receive stocktake request, bizType={}, bizNo={}, itemCount={}",
                request.getBizType(), request.getBizNo(), request.getItems().size());
        StocktakeResultDTO result = inventoryStocktakeApplicationService.stocktake(inventoryStockAssembler.toCommand(request));
        return Result.success(inventoryStockAssembler.toResultVO(result));
    }

    @PostMapping("/unlocks")
    @Operation(summary = "执行库存解锁", description = "根据业务单释放已锁定库存并生成库存流水。")
    public Result<StockUnlockResultVO> unlock(@Valid @RequestBody StockUnlockRequest request) {
        log.info("Receive stock-unlock request, bizType={}, bizNo={}, itemCount={}",
                request.getBizType(), request.getBizNo(), request.getItems().size());
        StockUnlockResultDTO result = inventoryStockUnlockApplicationService.unlock(inventoryStockAssembler.toCommand(request));
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
