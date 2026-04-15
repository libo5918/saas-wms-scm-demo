package com.example.scm.purchase.service.impl;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.purchase.client.InventoryStockInClient;
import com.example.scm.purchase.client.LocationClient;
import com.example.scm.purchase.client.MaterialClient;
import com.example.scm.purchase.client.SupplierClient;
import com.example.scm.purchase.client.WarehouseClient;
import com.example.scm.purchase.dto.CreatePurchaseReceiptItemRequest;
import com.example.scm.purchase.dto.CreatePurchaseReceiptRequest;
import com.example.scm.purchase.entity.PurchaseOrder;
import com.example.scm.purchase.entity.PurchaseOrderItem;
import com.example.scm.purchase.entity.PurchaseOrderStatus;
import com.example.scm.purchase.entity.PurchaseReceipt;
import com.example.scm.purchase.entity.PurchaseReceiptItem;
import com.example.scm.purchase.entity.PurchaseReceiptStatus;
import com.example.scm.purchase.mapper.PurchaseOrderItemMapper;
import com.example.scm.purchase.mapper.PurchaseOrderMapper;
import com.example.scm.purchase.mapper.PurchaseReceiptItemMapper;
import com.example.scm.purchase.mapper.PurchaseReceiptMapper;
import com.example.scm.purchase.service.PurchaseReceiptService;
import com.example.scm.purchase.support.PurchaseReceiptAssembler;
import com.example.scm.purchase.vo.PurchaseReceiptVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final PurchaseReceiptAssembler purchaseReceiptAssembler;
    private final MaterialClient materialClient;
    private final SupplierClient supplierClient;
    private final WarehouseClient warehouseClient;
    private final LocationClient locationClient;
    private final InventoryStockInClient inventoryStockInClient;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public PurchaseReceiptServiceImpl(PurchaseReceiptMapper purchaseReceiptMapper,
                                      PurchaseReceiptItemMapper purchaseReceiptItemMapper,
                                      PurchaseOrderMapper purchaseOrderMapper,
                                      PurchaseOrderItemMapper purchaseOrderItemMapper,
                                      PurchaseReceiptAssembler purchaseReceiptAssembler,
                                      MaterialClient materialClient,
                                      SupplierClient supplierClient,
                                      WarehouseClient warehouseClient,
                                      LocationClient locationClient,
                                      InventoryStockInClient inventoryStockInClient,
                                      TransactionTemplate transactionTemplate) {
        this.purchaseReceiptMapper = purchaseReceiptMapper;
        this.purchaseReceiptItemMapper = purchaseReceiptItemMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.purchaseReceiptAssembler = purchaseReceiptAssembler;
        this.materialClient = materialClient;
        this.supplierClient = supplierClient;
        this.warehouseClient = warehouseClient;
        this.locationClient = locationClient;
        this.inventoryStockInClient = inventoryStockInClient;
        this.transactionTemplate = transactionTemplate;
    }

    public PurchaseReceiptServiceImpl(PurchaseReceiptMapper purchaseReceiptMapper,
                                      PurchaseReceiptItemMapper purchaseReceiptItemMapper,
                                      PurchaseReceiptAssembler purchaseReceiptAssembler,
                                      MaterialClient materialClient,
                                      SupplierClient supplierClient,
                                      WarehouseClient warehouseClient,
                                      LocationClient locationClient,
                                      InventoryStockInClient inventoryStockInClient,
                                      TransactionTemplate transactionTemplate) {
        this(purchaseReceiptMapper,
                purchaseReceiptItemMapper,
                null,
                null,
                purchaseReceiptAssembler,
                materialClient,
                supplierClient,
                warehouseClient,
                locationClient,
                inventoryStockInClient,
                transactionTemplate);
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

        validateSupplier(tenantId, request.getSupplierId());
        validatePurchaseOrder(tenantId, request);
        validateMaterials(tenantId, request.getItems());
        validateStorage(tenantId, request.getWarehouseId(), request.getItems());

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

    private void validateSupplier(Long tenantId, Long supplierId) {
        supplierClient.validateSupplierEnabled(tenantId, supplierId);
    }

    private void validatePurchaseOrder(Long tenantId, CreatePurchaseReceiptRequest request) {
        if (purchaseOrderMapper == null || purchaseOrderItemMapper == null) {
            return;
        }
        PurchaseOrder purchaseOrder = purchaseOrderMapper.selectById(tenantId, request.getPurchaseOrderId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Purchase order not found"));
        if (!request.getSupplierId().equals(purchaseOrder.getSupplierId())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Supplier does not match purchase order");
        }

        List<PurchaseOrderItem> orderItems = purchaseOrderItemMapper.selectByOrderId(tenantId, purchaseOrder.getId());
        Map<Long, java.math.BigDecimal> materialRemainingQty = new HashMap<>();
        for (PurchaseOrderItem orderItem : orderItems) {
            materialRemainingQty.put(
                    orderItem.getMaterialId(),
                    orderItem.getPlanQty().subtract(orderItem.getReceivedQty())
            );
        }

        Map<Long, java.math.BigDecimal> receiptQtyByMaterial = new HashMap<>();
        for (CreatePurchaseReceiptItemRequest item : request.getItems()) {
            receiptQtyByMaterial.merge(item.getMaterialId(), item.getReceiptQty(), java.math.BigDecimal::add);
        }

        for (Map.Entry<Long, java.math.BigDecimal> receiptItem : receiptQtyByMaterial.entrySet()) {
            Long materialId = receiptItem.getKey();
            java.math.BigDecimal receiptQty = receiptItem.getValue();
            java.math.BigDecimal remainingQty = materialRemainingQty.get(materialId);
            if (remainingQty == null) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Purchase receipt item does not match purchase order");
            }
            if (receiptQty.compareTo(remainingQty) > 0) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Receipt qty exceeds purchase order remaining qty");
            }
        }
    }

    private void validateMaterials(Long tenantId, List<CreatePurchaseReceiptItemRequest> items) {
        for (CreatePurchaseReceiptItemRequest item : items) {
            materialClient.validateMaterialEnabled(tenantId, item.getMaterialId());
        }
    }

    private void validateStorage(Long tenantId, Long warehouseId, List<CreatePurchaseReceiptItemRequest> items) {
        warehouseClient.validateWarehouseEnabled(tenantId, warehouseId);
        for (CreatePurchaseReceiptItemRequest item : items) {
            locationClient.validateLocationEnabled(tenantId, warehouseId, item.getLocationId());
        }
    }

    private PurchaseReceiptVO processStockIn(Long tenantId, PurchaseReceipt receipt) {
        List<PurchaseReceiptItem> items = purchaseReceiptItemMapper.selectByReceiptId(tenantId, receipt.getId());
        try {
            log.info("Call inventory stock-in, tenantId={}, receiptId={}, receiptNo={}",
                    tenantId, receipt.getId(), receipt.getReceiptNo());
            inventoryStockInClient.stockIn(tenantId, SYSTEM_OPERATOR_ID, receipt, items);
            handleStockInSuccess(tenantId, receipt, items);
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

    private void handleStockInSuccess(Long tenantId, PurchaseReceipt receipt, List<PurchaseReceiptItem> items) {
        Integer affected = transactionTemplate.execute(status -> {
            syncPurchaseOrderProgress(tenantId, receipt.getPurchaseOrderId(), items);
            return purchaseReceiptMapper.updateStatus(
                    tenantId,
                    receipt.getId(),
                    PurchaseReceiptStatus.STOCK_IN_SUCCESS.name(),
                    null,
                    SYSTEM_OPERATOR_ID
            );
        });
        if (affected == null || affected == 0) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Update purchase receipt status failed");
        }
    }

    private void syncPurchaseOrderProgress(Long tenantId, Long purchaseOrderId, List<PurchaseReceiptItem> items) {
        // 兼容兜底：
        // 1) 单测里可能使用了不带采购订单 mapper 的构造器
        // 2) 理论上收货单必须有采购订单，但仍做空值保护，避免 NPE
        if (purchaseOrderMapper == null || purchaseOrderItemMapper == null || purchaseOrderId == null) {
            return;
        }

        // 同一收货单里可能出现同一物料多行，先按 materialId 聚合本次收货数量
        // 例如同物料拆到多个库位收货，订单维度只关心该物料本次总收货量
        Map<Long, java.math.BigDecimal> receiptQtyByMaterial = new HashMap<>();
        for (PurchaseReceiptItem item : items) {
            receiptQtyByMaterial.merge(item.getMaterialId(), item.getReceiptQty(), java.math.BigDecimal::add);
        }

        // 逐物料回写采购订单明细的 received_qty。
        // SQL 层带条件：received_qty + incrementQty <= plan_qty
        // 这样可防止超收，且在并发场景下由数据库原子更新兜底。
        for (Map.Entry<Long, java.math.BigDecimal> entry : receiptQtyByMaterial.entrySet()) {
            Integer itemAffected = purchaseOrderItemMapper.increaseReceivedQtyIfWithinPlan(
                    tenantId,
                    purchaseOrderId,
                    entry.getKey(),
                    entry.getValue()
            );
            // 未更新到任何行，通常是超收（或物料不匹配）导致条件不满足
            if (itemAffected == null || itemAffected == 0) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Receipt qty exceeds purchase order remaining qty");
            }
        }

        // 根据明细完成度推导订单状态：
        // 1) 没有未完成行 => 全部收货
        // 2) 仍有未完成行，但至少一行已开始收货 => 部分收货
        // 3) 否则 => 未收货（CREATED）
        Integer unfinishedItemCount = purchaseOrderItemMapper.countUnfinishedItemsByOrderId(tenantId, purchaseOrderId);
        String targetStatus;
        if (unfinishedItemCount != null && unfinishedItemCount == 0) {
            targetStatus = PurchaseOrderStatus.RECEIVED.name();
        } else {
            Integer startedItemCount = purchaseOrderItemMapper.countStartedItemsByOrderId(tenantId, purchaseOrderId);
            targetStatus = startedItemCount != null && startedItemCount > 0
                    ? PurchaseOrderStatus.PARTIALLY_RECEIVED.name()
                    : PurchaseOrderStatus.CREATED.name();
        }

        // 回写采购订单头状态；更新失败说明订单不存在或已被逻辑删除
        Integer orderAffected = purchaseOrderMapper.updateStatus(tenantId, purchaseOrderId, targetStatus, SYSTEM_OPERATOR_ID);
        if (orderAffected == null || orderAffected == 0) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Update purchase order status failed");
        }
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
