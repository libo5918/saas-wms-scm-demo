package com.example.scm.purchase.service.impl;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.purchase.client.InventoryStockInClient;
import com.example.scm.purchase.dto.CreatePurchaseReceiptItemRequest;
import com.example.scm.purchase.dto.CreatePurchaseReceiptRequest;
import com.example.scm.purchase.entity.PurchaseReceipt;
import com.example.scm.purchase.entity.PurchaseReceiptItem;
import com.example.scm.purchase.entity.PurchaseReceiptStatus;
import com.example.scm.purchase.mapper.PurchaseReceiptItemMapper;
import com.example.scm.purchase.mapper.PurchaseReceiptMapper;
import com.example.scm.purchase.service.PurchaseReceiptService;
import com.example.scm.purchase.support.PurchaseReceiptAssembler;
import com.example.scm.purchase.vo.PurchaseReceiptVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 采购收货应用服务实现。
 *
 * <p>当前类负责一条完整的收货主链路：</p>
 * <p>1. 校验收货单号是否已存在。</p>
 * <p>2. 先把收货单头和明细落到采购库。</p>
 * <p>3. 再同步调用库存服务做入库。</p>
 * <p>4. 最后根据库存结果回写收货单状态。</p>
 *
 * <p>之所以拆成两段事务，是为了在库存调用失败时仍然保留收货单现场，
 * 便于查看失败原因和后续补偿，而不是整单回滚消失。</p>
 */
@Service
@Slf4j
public class PurchaseReceiptServiceImpl implements PurchaseReceiptService {

    private static final long SYSTEM_OPERATOR_ID = 1L;
    private static final int FAILURE_REASON_MAX_LENGTH = 255;

    private final PurchaseReceiptMapper purchaseReceiptMapper;
    private final PurchaseReceiptItemMapper purchaseReceiptItemMapper;
    private final PurchaseReceiptAssembler purchaseReceiptAssembler;
    private final InventoryStockInClient inventoryStockInClient;
    private final TransactionTemplate transactionTemplate;

    public PurchaseReceiptServiceImpl(PurchaseReceiptMapper purchaseReceiptMapper,
                                      PurchaseReceiptItemMapper purchaseReceiptItemMapper,
                                      PurchaseReceiptAssembler purchaseReceiptAssembler,
                                      InventoryStockInClient inventoryStockInClient,
                                      TransactionTemplate transactionTemplate) {
        this.purchaseReceiptMapper = purchaseReceiptMapper;
        this.purchaseReceiptItemMapper = purchaseReceiptItemMapper;
        this.purchaseReceiptAssembler = purchaseReceiptAssembler;
        this.inventoryStockInClient = inventoryStockInClient;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public PurchaseReceiptVO create(CreatePurchaseReceiptRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Start create purchase receipt, tenantId={}, receiptNo={}", tenantId, request.getReceiptNo());

        PurchaseReceipt existingReceipt = purchaseReceiptMapper.selectByReceiptNo(tenantId, request.getReceiptNo()).orElse(null);
        if (existingReceipt != null) {
            if (toStatus(existingReceipt).isStockInSuccess()) {
                log.info("Skip duplicate purchase receipt create, tenantId={}, receiptId={}, receiptNo={}, status={}",
                        tenantId, existingReceipt.getId(), existingReceipt.getReceiptNo(), existingReceipt.getReceiptStatus());
                return getById(existingReceipt.getId());
            }
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Receipt no already exists");
        }

        PurchaseReceipt receipt = transactionTemplate.execute(status -> createReceiptInTransaction(tenantId, request));
        if (receipt == null) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Create purchase receipt failed");
        }
        return processStockIn(tenantId, receipt);
    }

    @Override
    public PurchaseReceiptVO getById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Query purchase receipt detail, tenantId={}, receiptId={}", tenantId, id);
        PurchaseReceipt receipt = purchaseReceiptMapper.selectById(tenantId, id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Purchase receipt not found"));
        return toVO(receipt, purchaseReceiptItemMapper.selectByReceiptId(tenantId, id));
    }

    @Override
    public PurchaseReceiptVO getByReceiptNo(String receiptNo) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Query purchase receipt detail by receiptNo, tenantId={}, receiptNo={}", tenantId, receiptNo);
        PurchaseReceipt receipt = purchaseReceiptMapper.selectByReceiptNo(tenantId, receiptNo)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Purchase receipt not found"));
        return toVO(receipt, purchaseReceiptItemMapper.selectByReceiptId(tenantId, receipt.getId()));
    }

    @Override
    public List<PurchaseReceiptVO> list() {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Query purchase receipt list, tenantId={}", tenantId);
        return purchaseReceiptMapper.selectByTenantId(tenantId).stream()
                .map(receipt -> toVO(receipt, purchaseReceiptItemMapper.selectByReceiptId(tenantId, receipt.getId())))
                .toList();
    }

    @Override
    public PurchaseReceiptVO retryStockIn(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Start retry purchase receipt stock-in, tenantId={}, receiptId={}", tenantId, id);

        PurchaseReceipt receipt = purchaseReceiptMapper.selectById(tenantId, id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Purchase receipt not found"));

        PurchaseReceiptStatus receiptStatus = toStatus(receipt);
        if (receiptStatus.isStockInSuccess()) {
            log.info("Skip retry for successful purchase receipt, tenantId={}, receiptId={}, receiptNo={}",
                    tenantId, receipt.getId(), receipt.getReceiptNo());
            return toVO(receipt, purchaseReceiptItemMapper.selectByReceiptId(tenantId, receipt.getId()));
        }
        if (!receiptStatus.canRetryStockIn()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Only failed purchase receipt can retry stock-in");
        }

        return processStockIn(tenantId, receipt);
    }

    @Override
    public PurchaseReceiptVO cancel(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Start cancel purchase receipt, tenantId={}, receiptId={}", tenantId, id);

        PurchaseReceipt receipt = purchaseReceiptMapper.selectById(tenantId, id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Purchase receipt not found"));

        PurchaseReceiptStatus receiptStatus = toStatus(receipt);
        if (!receiptStatus.canCancel()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Only pending or failed purchase receipt can be cancelled");
        }

        updateReceiptStatus(tenantId, receipt.getId(), PurchaseReceiptStatus.CANCELLED, null);
        log.info("Cancel purchase receipt success, tenantId={}, receiptId={}, receiptNo={}",
                tenantId, receipt.getId(), receipt.getReceiptNo());
        return getById(receipt.getId());
    }

    private PurchaseReceipt createReceiptInTransaction(Long tenantId, CreatePurchaseReceiptRequest request) {
        PurchaseReceipt receipt = purchaseReceiptAssembler.toNewReceipt(tenantId, SYSTEM_OPERATOR_ID, PurchaseReceiptStatus.CREATED.name(), request);
        purchaseReceiptMapper.insert(receipt);

        List<PurchaseReceiptItem> items = new ArrayList<>();
        for (CreatePurchaseReceiptItemRequest itemRequest : request.getItems()) {
            PurchaseReceiptItem item = purchaseReceiptAssembler.toNewReceiptItem(tenantId, receipt.getId(), itemRequest);
            purchaseReceiptItemMapper.insert(item);
            items.add(item);
        }
        log.info("Create purchase receipt persisted, tenantId={}, receiptId={}, receiptNo={}, itemCount={}",
                tenantId, receipt.getId(), receipt.getReceiptNo(), items.size());
        return receipt;
    }

    private PurchaseReceiptVO processStockIn(Long tenantId, PurchaseReceipt receipt) {
        List<PurchaseReceiptItem> items = purchaseReceiptItemMapper.selectByReceiptId(tenantId, receipt.getId());
        try {
            log.info("Call inventory stock-in, tenantId={}, receiptId={}, receiptNo={}",
                    tenantId, receipt.getId(), receipt.getReceiptNo());
            inventoryStockInClient.stockIn(tenantId, SYSTEM_OPERATOR_ID, receipt, items);
            updateReceiptStatus(tenantId, receipt.getId(), PurchaseReceiptStatus.STOCK_IN_SUCCESS, null);
            log.info("Purchase receipt stock-in success, tenantId={}, receiptId={}, receiptNo={}, itemCount={}",
                    tenantId, receipt.getId(), receipt.getReceiptNo(), items.size());
        } catch (BusinessException ex) {
            String failureReason = truncateFailureReason(ex.getMessage());
            updateReceiptStatus(tenantId, receipt.getId(), PurchaseReceiptStatus.STOCK_IN_FAILED, failureReason);
            log.error("Purchase receipt stock-in failed, tenantId={}, receiptId={}, receiptNo={}, reason={}",
                    tenantId, receipt.getId(), receipt.getReceiptNo(), failureReason, ex);
            throw ex;
        }
        return getById(receipt.getId());
    }

    private void updateReceiptStatus(Long tenantId, Long receiptId, PurchaseReceiptStatus receiptStatus, String failureReason) {
        Integer affected = transactionTemplate.execute(status ->
                purchaseReceiptMapper.updateStatus(tenantId, receiptId, receiptStatus.name(), failureReason, SYSTEM_OPERATOR_ID));
        if (affected == null || affected == 0) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Update purchase receipt status failed");
        }
    }

    private String truncateFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return null;
        }
        return failureReason.length() <= FAILURE_REASON_MAX_LENGTH
                ? failureReason
                : failureReason.substring(0, FAILURE_REASON_MAX_LENGTH);
    }

    private PurchaseReceiptVO toVO(PurchaseReceipt receipt, List<PurchaseReceiptItem> items) {
        return purchaseReceiptAssembler.toVO(receipt, items);
    }

    private PurchaseReceiptStatus toStatus(PurchaseReceipt receipt) {
        try {
            return PurchaseReceiptStatus.valueOf(receipt.getReceiptStatus());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Unknown purchase receipt status: " + receipt.getReceiptStatus());
        }
    }
}
