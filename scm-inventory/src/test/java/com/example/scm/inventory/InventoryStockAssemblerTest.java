package com.example.scm.inventory;

import com.example.scm.inventory.application.query.InventoryBalanceDTO;
import com.example.scm.inventory.application.query.InventoryTransactionRecordDTO;
import com.example.scm.inventory.application.query.StockAdjustLineResultDTO;
import com.example.scm.inventory.application.query.StockAdjustResultDTO;
import com.example.scm.inventory.application.query.StockInLineResultDTO;
import com.example.scm.inventory.application.query.StockInResultDTO;
import com.example.scm.inventory.application.query.StockLockLineResultDTO;
import com.example.scm.inventory.application.query.StockLockResultDTO;
import com.example.scm.inventory.application.query.StockOutLineResultDTO;
import com.example.scm.inventory.application.query.StockOutResultDTO;
import com.example.scm.inventory.application.query.StockTransferLineResultDTO;
import com.example.scm.inventory.application.query.StockTransferResultDTO;
import com.example.scm.inventory.application.query.StocktakeLineResultDTO;
import com.example.scm.inventory.application.query.StocktakeResultDTO;
import com.example.scm.inventory.application.query.StockUnlockLineResultDTO;
import com.example.scm.inventory.application.query.StockUnlockResultDTO;
import com.example.scm.inventory.interfaces.assembler.InventoryStockAssembler;
import com.example.scm.inventory.interfaces.dto.StockAdjustItemRequest;
import com.example.scm.inventory.interfaces.dto.StockAdjustRequest;
import com.example.scm.inventory.interfaces.dto.StockInItemRequest;
import com.example.scm.inventory.interfaces.dto.StockInRequest;
import com.example.scm.inventory.interfaces.dto.StockLockItemRequest;
import com.example.scm.inventory.interfaces.dto.StockLockRequest;
import com.example.scm.inventory.interfaces.dto.StockOutItemRequest;
import com.example.scm.inventory.interfaces.dto.StockOutRequest;
import com.example.scm.inventory.interfaces.dto.StockTransferItemRequest;
import com.example.scm.inventory.interfaces.dto.StockTransferRequest;
import com.example.scm.inventory.interfaces.dto.StocktakeItemRequest;
import com.example.scm.inventory.interfaces.dto.StocktakeRequest;
import com.example.scm.inventory.interfaces.dto.StockUnlockItemRequest;
import com.example.scm.inventory.interfaces.dto.StockUnlockRequest;
import com.example.scm.inventory.interfaces.vo.StockTransferResultVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryStockAssemblerTest {

    private final InventoryStockAssembler assembler = new InventoryStockAssembler();

    @Test
    void shouldConvertRequestsToCommands() {
        StockInRequest stockInRequest = new StockInRequest();
        stockInRequest.setBizType("PURCHASE_RECEIPT");
        stockInRequest.setBizNo("RCV-001");
        stockInRequest.setOperatorId(1L);
        StockInItemRequest stockInItemRequest = new StockInItemRequest();
        stockInItemRequest.setMaterialId(1001L);
        stockInItemRequest.setWarehouseId(2001L);
        stockInItemRequest.setLocationId(3001L);
        stockInItemRequest.setQuantity(new BigDecimal("5"));
        stockInRequest.setItems(List.of(stockInItemRequest));

        StockAdjustRequest adjustRequest = new StockAdjustRequest();
        adjustRequest.setBizType("MANUAL_ADJUST");
        adjustRequest.setBizNo("ADJ-001");
        adjustRequest.setAdjustType("INCREASE");
        adjustRequest.setOperatorId(1L);
        StockAdjustItemRequest adjustItemRequest = new StockAdjustItemRequest();
        adjustItemRequest.setMaterialId(1001L);
        adjustItemRequest.setWarehouseId(2001L);
        adjustItemRequest.setLocationId(3001L);
        adjustItemRequest.setQuantity(new BigDecimal("2"));
        adjustRequest.setItems(List.of(adjustItemRequest));

        StockOutRequest stockOutRequest = new StockOutRequest();
        stockOutRequest.setBizType("SALES_ORDER");
        stockOutRequest.setBizNo("SO-001");
        stockOutRequest.setOperatorId(1L);
        StockOutItemRequest stockOutItemRequest = new StockOutItemRequest();
        stockOutItemRequest.setMaterialId(1001L);
        stockOutItemRequest.setWarehouseId(2001L);
        stockOutItemRequest.setLocationId(3001L);
        stockOutItemRequest.setQuantity(new BigDecimal("3"));
        stockOutRequest.setItems(List.of(stockOutItemRequest));

        StockLockRequest lockRequest = new StockLockRequest();
        lockRequest.setBizType("SALES_ORDER");
        lockRequest.setBizNo("SO-001");
        lockRequest.setOperatorId(1L);
        StockLockItemRequest lockItemRequest = new StockLockItemRequest();
        lockItemRequest.setMaterialId(1001L);
        lockItemRequest.setWarehouseId(2001L);
        lockItemRequest.setLocationId(3001L);
        lockItemRequest.setQuantity(new BigDecimal("3"));
        lockRequest.setItems(List.of(lockItemRequest));

        StockUnlockRequest unlockRequest = new StockUnlockRequest();
        unlockRequest.setBizType("SALES_ORDER");
        unlockRequest.setBizNo("SO-001");
        unlockRequest.setOperatorId(1L);
        StockUnlockItemRequest unlockItemRequest = new StockUnlockItemRequest();
        unlockItemRequest.setMaterialId(1001L);
        unlockItemRequest.setWarehouseId(2001L);
        unlockItemRequest.setLocationId(3001L);
        unlockItemRequest.setQuantity(new BigDecimal("1"));
        unlockRequest.setItems(List.of(unlockItemRequest));

        StockTransferRequest transferRequest = new StockTransferRequest();
        transferRequest.setBizType("INVENTORY_TRANSFER");
        transferRequest.setBizNo("TRF-001");
        transferRequest.setOperatorId(1L);
        StockTransferItemRequest transferItemRequest = new StockTransferItemRequest();
        transferItemRequest.setMaterialId(1001L);
        transferItemRequest.setFromWarehouseId(2001L);
        transferItemRequest.setFromLocationId(3001L);
        transferItemRequest.setToWarehouseId(2002L);
        transferItemRequest.setToLocationId(3002L);
        transferItemRequest.setQuantity(new BigDecimal("4"));
        transferRequest.setItems(List.of(transferItemRequest));

        StocktakeRequest stocktakeRequest = new StocktakeRequest();
        stocktakeRequest.setBizType("STOCKTAKE");
        stocktakeRequest.setBizNo("STK-001");
        stocktakeRequest.setOperatorId(1L);
        StocktakeItemRequest stocktakeItemRequest = new StocktakeItemRequest();
        stocktakeItemRequest.setMaterialId(1001L);
        stocktakeItemRequest.setWarehouseId(2001L);
        stocktakeItemRequest.setLocationId(3001L);
        stocktakeItemRequest.setCountedQty(new BigDecimal("12"));
        stocktakeRequest.setItems(List.of(stocktakeItemRequest));

        assertEquals(1001L, assembler.toCommand(stockInRequest).getItems().getFirst().getMaterialId());
        assertEquals("INCREASE", assembler.toCommand(adjustRequest).getAdjustType());
        assertEquals(new BigDecimal("3"), assembler.toCommand(stockOutRequest).getItems().getFirst().getQuantity());
        assertEquals(2001L, assembler.toCommand(lockRequest).getItems().getFirst().getWarehouseId());
        assertEquals(new BigDecimal("1"), assembler.toCommand(unlockRequest).getItems().getFirst().getQuantity());
        assertEquals(3002L, assembler.toCommand(transferRequest).getItems().getFirst().getToLocationId());
        assertEquals(new BigDecimal("12"), assembler.toCommand(stocktakeRequest).getItems().getFirst().getCountedQty());
    }

    @Test
    void shouldConvertResultsAndQueriesToViewObjects() {
        StockInLineResultDTO stockInLine = new StockInLineResultDTO();
        stockInLine.setTxnNo("IN-001");
        stockInLine.setMaterialId(1001L);
        stockInLine.setWarehouseId(2001L);
        stockInLine.setLocationId(3001L);
        stockInLine.setQuantity(new BigDecimal("5"));
        stockInLine.setBeforeQty(BigDecimal.ZERO);
        stockInLine.setAfterQty(new BigDecimal("5"));
        StockInResultDTO stockInResult = new StockInResultDTO();
        stockInResult.setBizType("PURCHASE_RECEIPT");
        stockInResult.setBizNo("RCV-001");
        stockInResult.setLines(List.of(stockInLine));

        StockAdjustLineResultDTO adjustLine = new StockAdjustLineResultDTO();
        adjustLine.setTxnNo("ADJ-001");
        adjustLine.setMaterialId(1001L);
        adjustLine.setWarehouseId(2001L);
        adjustLine.setLocationId(3001L);
        adjustLine.setQuantity(new BigDecimal("2"));
        adjustLine.setBeforeQty(new BigDecimal("8"));
        adjustLine.setAfterQty(new BigDecimal("10"));
        StockAdjustResultDTO adjustResult = new StockAdjustResultDTO();
        adjustResult.setBizType("MANUAL_ADJUST");
        adjustResult.setBizNo("ADJ-001");
        adjustResult.setAdjustType("INCREASE");
        adjustResult.setLines(List.of(adjustLine));

        StockOutLineResultDTO stockOutLine = new StockOutLineResultDTO();
        stockOutLine.setTxnNo("OUT-001");
        stockOutLine.setMaterialId(1001L);
        stockOutLine.setWarehouseId(2001L);
        stockOutLine.setLocationId(3001L);
        stockOutLine.setQuantity(new BigDecimal("3"));
        stockOutLine.setBeforeQty(new BigDecimal("10"));
        stockOutLine.setAfterQty(new BigDecimal("7"));
        StockOutResultDTO stockOutResult = new StockOutResultDTO();
        stockOutResult.setBizType("SALES_ORDER");
        stockOutResult.setBizNo("SO-001");
        stockOutResult.setLines(List.of(stockOutLine));

        StockLockLineResultDTO lockLine = new StockLockLineResultDTO();
        lockLine.setTxnNo("LOCK-001");
        lockLine.setMaterialId(1001L);
        lockLine.setWarehouseId(2001L);
        lockLine.setLocationId(3001L);
        lockLine.setQuantity(new BigDecimal("3"));
        lockLine.setBeforeQty(BigDecimal.ZERO);
        lockLine.setAfterQty(new BigDecimal("3"));
        StockLockResultDTO lockResult = new StockLockResultDTO();
        lockResult.setBizType("SALES_ORDER");
        lockResult.setBizNo("SO-001");
        lockResult.setLines(List.of(lockLine));

        StockUnlockLineResultDTO unlockLine = new StockUnlockLineResultDTO();
        unlockLine.setTxnNo("UNLOCK-001");
        unlockLine.setMaterialId(1001L);
        unlockLine.setWarehouseId(2001L);
        unlockLine.setLocationId(3001L);
        unlockLine.setQuantity(new BigDecimal("1"));
        unlockLine.setBeforeQty(new BigDecimal("3"));
        unlockLine.setAfterQty(new BigDecimal("2"));
        StockUnlockResultDTO unlockResult = new StockUnlockResultDTO();
        unlockResult.setBizType("SALES_ORDER");
        unlockResult.setBizNo("SO-001");
        unlockResult.setLines(List.of(unlockLine));

        StockTransferLineResultDTO transferLine = new StockTransferLineResultDTO();
        transferLine.setMoveOutTxnNo("MOVEOUT-001");
        transferLine.setMoveInTxnNo("MOVEIN-001");
        transferLine.setMaterialId(1001L);
        transferLine.setFromWarehouseId(2001L);
        transferLine.setFromLocationId(3001L);
        transferLine.setToWarehouseId(2002L);
        transferLine.setToLocationId(3002L);
        transferLine.setQuantity(new BigDecimal("4"));
        transferLine.setFromBeforeQty(new BigDecimal("10"));
        transferLine.setFromAfterQty(new BigDecimal("6"));
        transferLine.setToBeforeQty(new BigDecimal("1"));
        transferLine.setToAfterQty(new BigDecimal("5"));
        StockTransferResultDTO transferResult = new StockTransferResultDTO();
        transferResult.setBizType("INVENTORY_TRANSFER");
        transferResult.setBizNo("TRF-001");
        transferResult.setLines(List.of(transferLine));

        StocktakeLineResultDTO stocktakeLine = new StocktakeLineResultDTO();
        stocktakeLine.setTxnNo("STKIN-001");
        stocktakeLine.setMaterialId(1001L);
        stocktakeLine.setWarehouseId(2001L);
        stocktakeLine.setLocationId(3001L);
        stocktakeLine.setSystemQty(new BigDecimal("10"));
        stocktakeLine.setCountedQty(new BigDecimal("12"));
        stocktakeLine.setVarianceQty(new BigDecimal("2"));
        stocktakeLine.setAdjustType("INCREASE");
        StocktakeResultDTO stocktakeResult = new StocktakeResultDTO();
        stocktakeResult.setBizType("STOCKTAKE");
        stocktakeResult.setBizNo("STK-001");
        stocktakeResult.setLines(List.of(stocktakeLine));

        InventoryBalanceDTO balanceDTO = new InventoryBalanceDTO();
        balanceDTO.setMaterialId(1001L);
        balanceDTO.setWarehouseId(2001L);
        balanceDTO.setLocationId(3001L);
        balanceDTO.setOnHandQty(new BigDecimal("10"));
        balanceDTO.setLockedQty(new BigDecimal("3"));
        balanceDTO.setAvailableQty(new BigDecimal("7"));
        balanceDTO.setVersion(2L);

        InventoryTransactionRecordDTO txnDTO = new InventoryTransactionRecordDTO();
        txnDTO.setTxnNo("TXN-001");
        txnDTO.setBizType("PURCHASE_RECEIPT");
        txnDTO.setBizNo("RCV-001");
        txnDTO.setMaterialId(1001L);
        txnDTO.setWarehouseId(2001L);
        txnDTO.setLocationId(3001L);
        txnDTO.setTxnDirection("IN");
        txnDTO.setTxnQty(new BigDecimal("5"));
        txnDTO.setBeforeQty(BigDecimal.ZERO);
        txnDTO.setAfterQty(new BigDecimal("5"));

        StockTransferResultVO transferVO = assembler.toResultVO(transferResult);

        assertEquals("RCV-001", assembler.toResultVO(stockInResult).getBizNo());
        assertEquals("INCREASE", assembler.toResultVO(adjustResult).getAdjustType());
        assertEquals("OUT-001", assembler.toResultVO(stockOutResult).getLines().getFirst().getTxnNo());
        assertEquals("LOCK-001", assembler.toResultVO(lockResult).getLines().getFirst().getTxnNo());
        assertEquals("UNLOCK-001", assembler.toResultVO(unlockResult).getLines().getFirst().getTxnNo());
        assertEquals("MOVEIN-001", transferVO.getLines().getFirst().getMoveInTxnNo());
        assertEquals("INCREASE", assembler.toResultVO(stocktakeResult).getLines().getFirst().getAdjustType());
        assertEquals(new BigDecimal("7"), assembler.toBalanceVO(balanceDTO).getAvailableQty());
        assertEquals("IN", assembler.toTxnRecordVO(txnDTO).getTxnDirection());
    }
}
