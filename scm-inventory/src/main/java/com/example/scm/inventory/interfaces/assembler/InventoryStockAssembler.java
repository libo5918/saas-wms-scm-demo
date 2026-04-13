package com.example.scm.inventory.interfaces.assembler;

import com.example.scm.inventory.application.command.StockInCommand;
import com.example.scm.inventory.application.command.StockInItemCommand;
import com.example.scm.inventory.application.query.InventoryBalanceDTO;
import com.example.scm.inventory.application.query.StockInLineResultDTO;
import com.example.scm.inventory.application.query.StockInResultDTO;
import com.example.scm.inventory.interfaces.dto.StockInItemRequest;
import com.example.scm.inventory.interfaces.dto.StockInRequest;
import com.example.scm.inventory.interfaces.vo.InventoryBalanceVO;
import com.example.scm.inventory.interfaces.vo.StockInLineVO;
import com.example.scm.inventory.interfaces.vo.StockInResultVO;
import org.springframework.stereotype.Component;

@Component
public class InventoryStockAssembler {

    public StockInCommand toCommand(StockInRequest request) {
        StockInCommand command = new StockInCommand();
        command.setBizType(request.getBizType());
        command.setBizNo(request.getBizNo());
        command.setOperatorId(request.getOperatorId());
        for (StockInItemRequest item : request.getItems()) {
            command.getItems().add(toItemCommand(item));
        }
        return command;
    }

    public StockInResultVO toResultVO(StockInResultDTO result) {
        StockInResultVO vo = new StockInResultVO();
        vo.setBizType(result.getBizType());
        vo.setBizNo(result.getBizNo());
        for (StockInLineResultDTO line : result.getLines()) {
            vo.getLines().add(toLineVO(line));
        }
        return vo;
    }

    public InventoryBalanceVO toBalanceVO(InventoryBalanceDTO dto) {
        InventoryBalanceVO vo = new InventoryBalanceVO();
        vo.setMaterialId(dto.getMaterialId());
        vo.setWarehouseId(dto.getWarehouseId());
        vo.setLocationId(dto.getLocationId());
        vo.setOnHandQty(dto.getOnHandQty());
        vo.setLockedQty(dto.getLockedQty());
        vo.setAvailableQty(dto.getAvailableQty());
        vo.setVersion(dto.getVersion());
        return vo;
    }

    private StockInItemCommand toItemCommand(StockInItemRequest item) {
        StockInItemCommand command = new StockInItemCommand();
        command.setMaterialId(item.getMaterialId());
        command.setWarehouseId(item.getWarehouseId());
        command.setLocationId(item.getLocationId());
        command.setQuantity(item.getQuantity());
        return command;
    }

    private StockInLineVO toLineVO(StockInLineResultDTO line) {
        StockInLineVO vo = new StockInLineVO();
        vo.setTxnNo(line.getTxnNo());
        vo.setMaterialId(line.getMaterialId());
        vo.setWarehouseId(line.getWarehouseId());
        vo.setLocationId(line.getLocationId());
        vo.setQuantity(line.getQuantity());
        vo.setBeforeQty(line.getBeforeQty());
        vo.setAfterQty(line.getAfterQty());
        return vo;
    }
}
