package com.example.scm.purchase.service.impl;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.purchase.client.InventoryStockInClient;
import com.example.scm.purchase.dto.CreatePurchaseReceiptItemRequest;
import com.example.scm.purchase.dto.CreatePurchaseReceiptRequest;
import com.example.scm.purchase.entity.PurchaseReceipt;
import com.example.scm.purchase.entity.PurchaseReceiptItem;
import com.example.scm.purchase.mapper.PurchaseReceiptItemMapper;
import com.example.scm.purchase.mapper.PurchaseReceiptMapper;
import com.example.scm.purchase.service.PurchaseReceiptService;
import com.example.scm.purchase.support.PurchaseReceiptAssembler;
import com.example.scm.purchase.vo.PurchaseReceiptVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 采购收货应用服务实现，负责收货单头明细落库和视图组装。
 */
@Service
@Slf4j
public class PurchaseReceiptServiceImpl implements PurchaseReceiptService {

    private static final long SYSTEM_OPERATOR_ID = 1L;
    private static final String CREATED_STATUS = "CREATED";
    private static final String STOCK_IN_SUCCESS_STATUS = "STOCK_IN_SUCCESS";

    private final PurchaseReceiptMapper purchaseReceiptMapper;
    private final PurchaseReceiptItemMapper purchaseReceiptItemMapper;
    private final PurchaseReceiptAssembler purchaseReceiptAssembler;
    private final InventoryStockInClient inventoryStockInClient;

    public PurchaseReceiptServiceImpl(PurchaseReceiptMapper purchaseReceiptMapper,
                                      PurchaseReceiptItemMapper purchaseReceiptItemMapper,
                                      PurchaseReceiptAssembler purchaseReceiptAssembler,
                                      InventoryStockInClient inventoryStockInClient) {
        this.purchaseReceiptMapper = purchaseReceiptMapper;
        this.purchaseReceiptItemMapper = purchaseReceiptItemMapper;
        this.purchaseReceiptAssembler = purchaseReceiptAssembler;
        this.inventoryStockInClient = inventoryStockInClient;
    }

    /**
     * 创建收货单头及其明细，并同步触发库存入库。
     */
    @Override
    @Transactional
    public PurchaseReceiptVO create(CreatePurchaseReceiptRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Start create purchase receipt, tenantId={}, receiptNo={}", tenantId, request.getReceiptNo());
        purchaseReceiptMapper.selectByReceiptNo(tenantId, request.getReceiptNo()).ifPresent(receipt -> {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Receipt no already exists");
        });

        PurchaseReceipt receipt = purchaseReceiptAssembler.toNewReceipt(tenantId, SYSTEM_OPERATOR_ID, CREATED_STATUS, request);
        purchaseReceiptMapper.insert(receipt);

        List<PurchaseReceiptItem> items = new ArrayList<>();
        for (CreatePurchaseReceiptItemRequest itemRequest : request.getItems()) {
            PurchaseReceiptItem item = purchaseReceiptAssembler.toNewReceiptItem(tenantId, receipt.getId(), itemRequest);
            purchaseReceiptItemMapper.insert(item);
            items.add(item);
        }

        log.info("Call inventory stock-in after receipt creation, tenantId={}, receiptId={}, receiptNo={}",
                tenantId, receipt.getId(), receipt.getReceiptNo());
        inventoryStockInClient.stockIn(tenantId, SYSTEM_OPERATOR_ID, receipt, items);

        int affected = purchaseReceiptMapper.updateStatus(tenantId, receipt.getId(), STOCK_IN_SUCCESS_STATUS, SYSTEM_OPERATOR_ID);
        if (affected == 0) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Update purchase receipt status failed");
        }
        receipt.setReceiptStatus(STOCK_IN_SUCCESS_STATUS);
        log.info("Create purchase receipt and stock-in success, tenantId={}, receiptId={}, receiptNo={}, itemCount={}",
                tenantId, receipt.getId(), receipt.getReceiptNo(), items.size());
        return getById(receipt.getId());
    }

    /**
     * 查询收货单详情。
     */
    @Override
    public PurchaseReceiptVO getById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Query purchase receipt detail, tenantId={}, receiptId={}", tenantId, id);
        PurchaseReceipt receipt = purchaseReceiptMapper.selectById(tenantId, id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Purchase receipt not found"));
        return toVO(receipt, purchaseReceiptItemMapper.selectByReceiptId(tenantId, id));
    }

    /**
     * 查询当前租户下的收货单列表。
     */
    @Override
    public List<PurchaseReceiptVO> list() {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Query purchase receipt list, tenantId={}", tenantId);
        return purchaseReceiptMapper.selectByTenantId(tenantId).stream()
                .map(receipt -> toVO(receipt, purchaseReceiptItemMapper.selectByReceiptId(tenantId, receipt.getId())))
                .toList();
    }

    /**
     * 把单头和明细转换成返回对象。
     */
    private PurchaseReceiptVO toVO(PurchaseReceipt receipt, List<PurchaseReceiptItem> items) {
        return purchaseReceiptAssembler.toVO(receipt, items);
    }
}
