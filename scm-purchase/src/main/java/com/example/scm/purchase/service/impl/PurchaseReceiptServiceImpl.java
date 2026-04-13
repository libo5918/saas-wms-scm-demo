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

    /**
     * 当前阶段的系统操作人。
     */
    private static final long SYSTEM_OPERATOR_ID = 1L;

    /**
     * 新创建但尚未完成库存联动的状态。
     */
    private static final String CREATED_STATUS = "CREATED";

    /**
     * 库存入库成功后的状态。
     */
    private static final String STOCK_IN_SUCCESS_STATUS = "STOCK_IN_SUCCESS";

    /**
     * 库存入库失败后的状态。
     */
    private static final String STOCK_IN_FAILED_STATUS = "STOCK_IN_FAILED";

    /**
     * 失败原因在数据库中的最大长度。
     */
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

    /**
     * 创建收货单并联动库存。
     *
     * <p>这里先做收货侧幂等控制：</p>
     * <p>1. 如果同一单号已经成功入库，直接返回已有结果，避免重复联动库存。</p>
     * <p>2. 如果同一单号存在但尚未成功，则判定为重复创建并拒绝。</p>
     *
     * <p>真正的库存调用失败时，会把收货单回写为 STOCK_IN_FAILED，
     * 同时保留 failureReason，最后再把异常继续抛给上层。</p>
     */
    @Override
    public PurchaseReceiptVO create(CreatePurchaseReceiptRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        log.info("Start create purchase receipt, tenantId={}, receiptNo={}", tenantId, request.getReceiptNo());

        PurchaseReceipt existingReceipt = purchaseReceiptMapper.selectByReceiptNo(tenantId, request.getReceiptNo()).orElse(null);
        if (existingReceipt != null) {
            if (STOCK_IN_SUCCESS_STATUS.equals(existingReceipt.getReceiptStatus())) {
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
        List<PurchaseReceiptItem> items = purchaseReceiptItemMapper.selectByReceiptId(tenantId, receipt.getId());

        try {
            log.info("Call inventory stock-in after receipt creation, tenantId={}, receiptId={}, receiptNo={}",
                    tenantId, receipt.getId(), receipt.getReceiptNo());
            inventoryStockInClient.stockIn(tenantId, SYSTEM_OPERATOR_ID, receipt, items);
            updateReceiptStatus(tenantId, receipt.getId(), STOCK_IN_SUCCESS_STATUS, null);
            log.info("Create purchase receipt and stock-in success, tenantId={}, receiptId={}, receiptNo={}, itemCount={}",
                    tenantId, receipt.getId(), receipt.getReceiptNo(), items.size());
        } catch (BusinessException ex) {
            String failureReason = truncateFailureReason(ex.getMessage());
            updateReceiptStatus(tenantId, receipt.getId(), STOCK_IN_FAILED_STATUS, failureReason);
            log.error("Create purchase receipt but stock-in failed, tenantId={}, receiptId={}, receiptNo={}, reason={}",
                    tenantId, receipt.getId(), receipt.getReceiptNo(), failureReason, ex);
            throw ex;
        }

        return getById(receipt.getId());
    }

    /**
     * 查询收货单详情。
     *
     * <p>详情会返回单头、状态、失败原因和全部明细。</p>
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
     * 在本地事务内创建收货单头和明细。
     *
     * <p>这一步只负责采购库落单，不做远程库存调用。</p>
     */
    private PurchaseReceipt createReceiptInTransaction(Long tenantId, CreatePurchaseReceiptRequest request) {
        PurchaseReceipt receipt = purchaseReceiptAssembler.toNewReceipt(tenantId, SYSTEM_OPERATOR_ID, CREATED_STATUS, request);
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

    /**
     * 回写收货单状态。
     *
     * <p>单独放在事务模板里执行，确保无论库存成功还是失败，
     * 收货单状态都能稳定落库。</p>
     */
    private void updateReceiptStatus(Long tenantId, Long receiptId, String receiptStatus, String failureReason) {
        Integer affected = transactionTemplate.execute(status ->
                purchaseReceiptMapper.updateStatus(tenantId, receiptId, receiptStatus, failureReason, SYSTEM_OPERATOR_ID));
        if (affected == null || affected == 0) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Update purchase receipt status failed");
        }
    }

    /**
     * 截断失败原因，避免超出数据库字段长度。
     */
    private String truncateFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return null;
        }
        return failureReason.length() <= FAILURE_REASON_MAX_LENGTH
                ? failureReason
                : failureReason.substring(0, FAILURE_REASON_MAX_LENGTH);
    }

    /**
     * 把单头和明细转换成返回对象。
     */
    private PurchaseReceiptVO toVO(PurchaseReceipt receipt, List<PurchaseReceiptItem> items) {
        return purchaseReceiptAssembler.toVO(receipt, items);
    }
}
