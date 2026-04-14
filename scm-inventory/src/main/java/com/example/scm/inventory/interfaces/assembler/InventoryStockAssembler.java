package com.example.scm.inventory.interfaces.assembler;

import com.example.scm.inventory.application.command.StockInCommand;
import com.example.scm.inventory.application.command.StockInItemCommand;
import com.example.scm.inventory.application.command.StockLockCommand;
import com.example.scm.inventory.application.command.StockLockItemCommand;
import com.example.scm.inventory.application.command.StockOutCommand;
import com.example.scm.inventory.application.command.StockOutItemCommand;
import com.example.scm.inventory.application.command.StockUnlockCommand;
import com.example.scm.inventory.application.command.StockUnlockItemCommand;
import com.example.scm.inventory.application.query.InventoryBalanceDTO;
import com.example.scm.inventory.application.query.InventoryTransactionRecordDTO;
import com.example.scm.inventory.application.query.StockInLineResultDTO;
import com.example.scm.inventory.application.query.StockInResultDTO;
import com.example.scm.inventory.application.query.StockLockLineResultDTO;
import com.example.scm.inventory.application.query.StockLockResultDTO;
import com.example.scm.inventory.application.query.StockOutLineResultDTO;
import com.example.scm.inventory.application.query.StockOutResultDTO;
import com.example.scm.inventory.application.query.StockUnlockLineResultDTO;
import com.example.scm.inventory.application.query.StockUnlockResultDTO;
import com.example.scm.inventory.interfaces.dto.StockInItemRequest;
import com.example.scm.inventory.interfaces.dto.StockInRequest;
import com.example.scm.inventory.interfaces.dto.StockLockItemRequest;
import com.example.scm.inventory.interfaces.dto.StockLockRequest;
import com.example.scm.inventory.interfaces.dto.StockOutItemRequest;
import com.example.scm.inventory.interfaces.dto.StockOutRequest;
import com.example.scm.inventory.interfaces.dto.StockUnlockItemRequest;
import com.example.scm.inventory.interfaces.dto.StockUnlockRequest;
import com.example.scm.inventory.interfaces.vo.InventoryBalanceVO;
import com.example.scm.inventory.interfaces.vo.InventoryTransactionRecordVO;
import com.example.scm.inventory.interfaces.vo.StockInLineVO;
import com.example.scm.inventory.interfaces.vo.StockInResultVO;
import com.example.scm.inventory.interfaces.vo.StockLockLineVO;
import com.example.scm.inventory.interfaces.vo.StockLockResultVO;
import com.example.scm.inventory.interfaces.vo.StockOutLineVO;
import com.example.scm.inventory.interfaces.vo.StockOutResultVO;
import com.example.scm.inventory.interfaces.vo.StockUnlockLineVO;
import com.example.scm.inventory.interfaces.vo.StockUnlockResultVO;
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

    public StockOutCommand toCommand(StockOutRequest request) {
        StockOutCommand command = new StockOutCommand();
        command.setBizType(request.getBizType());
        command.setBizNo(request.getBizNo());
        command.setOperatorId(request.getOperatorId());
        for (StockOutItemRequest item : request.getItems()) {
            command.getItems().add(toItemCommand(item));
        }
        return command;
    }

    public StockLockCommand toCommand(StockLockRequest request) {
        StockLockCommand command = new StockLockCommand();
        command.setBizType(request.getBizType());
        command.setBizNo(request.getBizNo());
        command.setOperatorId(request.getOperatorId());
        for (StockLockItemRequest item : request.getItems()) {
            command.getItems().add(toItemCommand(item));
        }
        return command;
    }

    public StockUnlockCommand toCommand(StockUnlockRequest request) {
        StockUnlockCommand command = new StockUnlockCommand();
        command.setBizType(request.getBizType());
        command.setBizNo(request.getBizNo());
        command.setOperatorId(request.getOperatorId());
        for (StockUnlockItemRequest item : request.getItems()) {
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

    public StockOutResultVO toResultVO(StockOutResultDTO result) {
        StockOutResultVO vo = new StockOutResultVO();
        vo.setBizType(result.getBizType());
        vo.setBizNo(result.getBizNo());
        for (StockOutLineResultDTO line : result.getLines()) {
            vo.getLines().add(toLineVO(line));
        }
        return vo;
    }

    public StockLockResultVO toResultVO(StockLockResultDTO result) {
        StockLockResultVO vo = new StockLockResultVO();
        vo.setBizType(result.getBizType());
        vo.setBizNo(result.getBizNo());
        for (StockLockLineResultDTO line : result.getLines()) {
            vo.getLines().add(toLineVO(line));
        }
        return vo;
    }

    public StockUnlockResultVO toResultVO(StockUnlockResultDTO result) {
        StockUnlockResultVO vo = new StockUnlockResultVO();
        vo.setBizType(result.getBizType());
        vo.setBizNo(result.getBizNo());
        for (StockUnlockLineResultDTO line : result.getLines()) {
            vo.getLines().add(toLineVO(line));
        }
        return vo;
    }

    public InventoryTransactionRecordVO toTxnRecordVO(InventoryTransactionRecordDTO dto) {
        InventoryTransactionRecordVO vo = new InventoryTransactionRecordVO();
        vo.setTxnNo(dto.getTxnNo());
        vo.setBizType(dto.getBizType());
        vo.setBizNo(dto.getBizNo());
        vo.setMaterialId(dto.getMaterialId());
        vo.setWarehouseId(dto.getWarehouseId());
        vo.setLocationId(dto.getLocationId());
        vo.setTxnDirection(dto.getTxnDirection());
        vo.setTxnQty(dto.getTxnQty());
        vo.setBeforeQty(dto.getBeforeQty());
        vo.setAfterQty(dto.getAfterQty());
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

    private StockOutItemCommand toItemCommand(StockOutItemRequest item) {
        StockOutItemCommand command = new StockOutItemCommand();
        command.setMaterialId(item.getMaterialId());
        command.setWarehouseId(item.getWarehouseId());
        command.setLocationId(item.getLocationId());
        command.setQuantity(item.getQuantity());
        return command;
    }

    private StockLockItemCommand toItemCommand(StockLockItemRequest item) {
        StockLockItemCommand command = new StockLockItemCommand();
        command.setMaterialId(item.getMaterialId());
        command.setWarehouseId(item.getWarehouseId());
        command.setLocationId(item.getLocationId());
        command.setQuantity(item.getQuantity());
        return command;
    }

    private StockUnlockItemCommand toItemCommand(StockUnlockItemRequest item) {
        StockUnlockItemCommand command = new StockUnlockItemCommand();
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

    private StockOutLineVO toLineVO(StockOutLineResultDTO line) {
        StockOutLineVO vo = new StockOutLineVO();
        vo.setTxnNo(line.getTxnNo());
        vo.setMaterialId(line.getMaterialId());
        vo.setWarehouseId(line.getWarehouseId());
        vo.setLocationId(line.getLocationId());
        vo.setQuantity(line.getQuantity());
        vo.setBeforeQty(line.getBeforeQty());
        vo.setAfterQty(line.getAfterQty());
        return vo;
    }

    private StockLockLineVO toLineVO(StockLockLineResultDTO line) {
        StockLockLineVO vo = new StockLockLineVO();
        vo.setTxnNo(line.getTxnNo());
        vo.setMaterialId(line.getMaterialId());
        vo.setWarehouseId(line.getWarehouseId());
        vo.setLocationId(line.getLocationId());
        vo.setQuantity(line.getQuantity());
        vo.setBeforeQty(line.getBeforeQty());
        vo.setAfterQty(line.getAfterQty());
        return vo;
    }

    private StockUnlockLineVO toLineVO(StockUnlockLineResultDTO line) {
        StockUnlockLineVO vo = new StockUnlockLineVO();
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
