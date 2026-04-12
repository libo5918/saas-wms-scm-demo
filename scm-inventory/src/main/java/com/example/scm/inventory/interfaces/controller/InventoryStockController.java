package com.example.scm.inventory.interfaces.controller;

import com.example.scm.common.core.Result;
import com.example.scm.inventory.application.command.StockInCommand;
import com.example.scm.inventory.application.command.StockInItemCommand;
import com.example.scm.inventory.application.query.InventoryBalanceDTO;
import com.example.scm.inventory.application.query.StockInLineResultDTO;
import com.example.scm.inventory.application.query.StockInResultDTO;
import com.example.scm.inventory.application.service.InventoryBalanceQueryService;
import com.example.scm.inventory.application.service.InventoryStockInApplicationService;
import com.example.scm.inventory.interfaces.dto.StockInItemRequest;
import com.example.scm.inventory.interfaces.dto.StockInRequest;
import com.example.scm.inventory.interfaces.vo.InventoryBalanceVO;
import com.example.scm.inventory.interfaces.vo.StockInLineVO;
import com.example.scm.inventory.interfaces.vo.StockInResultVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryStockController {

    private final InventoryStockInApplicationService inventoryStockInApplicationService;
    private final InventoryBalanceQueryService inventoryBalanceQueryService;

    public InventoryStockController(InventoryStockInApplicationService inventoryStockInApplicationService,
                                    InventoryBalanceQueryService inventoryBalanceQueryService) {
        this.inventoryStockInApplicationService = inventoryStockInApplicationService;
        this.inventoryBalanceQueryService = inventoryBalanceQueryService;
    }

    @PostMapping("/stock-ins")
    public Result<StockInResultVO> stockIn(@Valid @RequestBody StockInRequest request) {
        StockInCommand command = new StockInCommand();
        command.setBizType(request.getBizType());
        command.setBizNo(request.getBizNo());
        command.setOperatorId(request.getOperatorId());
        for (StockInItemRequest item : request.getItems()) {
            StockInItemCommand itemCommand = new StockInItemCommand();
            itemCommand.setMaterialId(item.getMaterialId());
            itemCommand.setWarehouseId(item.getWarehouseId());
            itemCommand.setLocationId(item.getLocationId());
            itemCommand.setQuantity(item.getQuantity());
            command.getItems().add(itemCommand);
        }

        StockInResultDTO result = inventoryStockInApplicationService.stockIn(command);
        StockInResultVO vo = new StockInResultVO();
        vo.setBizType(result.getBizType());
        vo.setBizNo(result.getBizNo());
        for (StockInLineResultDTO line : result.getLines()) {
            StockInLineVO lineVO = new StockInLineVO();
            lineVO.setTxnNo(line.getTxnNo());
            lineVO.setMaterialId(line.getMaterialId());
            lineVO.setWarehouseId(line.getWarehouseId());
            lineVO.setLocationId(line.getLocationId());
            lineVO.setQuantity(line.getQuantity());
            lineVO.setBeforeQty(line.getBeforeQty());
            lineVO.setAfterQty(line.getAfterQty());
            vo.getLines().add(lineVO);
        }
        return Result.success(vo);
    }

    @GetMapping("/balances")
    public Result<InventoryBalanceVO> getBalance(@RequestParam("materialId") Long materialId,
                                                 @RequestParam("warehouseId") Long warehouseId,
                                                 @RequestParam("locationId") Long locationId) {
        InventoryBalanceDTO dto = inventoryBalanceQueryService.getBalance(materialId, warehouseId, locationId);
        InventoryBalanceVO vo = new InventoryBalanceVO();
        vo.setMaterialId(dto.getMaterialId());
        vo.setWarehouseId(dto.getWarehouseId());
        vo.setLocationId(dto.getLocationId());
        vo.setOnHandQty(dto.getOnHandQty());
        vo.setLockedQty(dto.getLockedQty());
        vo.setAvailableQty(dto.getAvailableQty());
        vo.setVersion(dto.getVersion());
        return Result.success(vo);
    }
}
