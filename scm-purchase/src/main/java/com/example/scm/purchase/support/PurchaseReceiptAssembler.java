package com.example.scm.purchase.support;

import com.example.scm.purchase.dto.CreatePurchaseReceiptItemRequest;
import com.example.scm.purchase.dto.CreatePurchaseReceiptRequest;
import com.example.scm.purchase.entity.PurchaseReceipt;
import com.example.scm.purchase.entity.PurchaseReceiptItem;
import com.example.scm.purchase.vo.PurchaseReceiptItemVO;
import com.example.scm.purchase.vo.PurchaseReceiptVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 采购收货对象转换器。
 *
 * <p>把控制层请求、实体对象、返回视图之间的转换集中在这里，
 * 避免 service 里充斥大段 new + set 代码。</p>
 */
@Component
public class PurchaseReceiptAssembler {

    /**
     * 把创建请求转换成新的收货单头实体。
     *
     * <p>新建时只填充当前阶段必须的信息，状态由调用方明确传入。</p>
     */
    public PurchaseReceipt toNewReceipt(Long tenantId, Long operatorId, String status, CreatePurchaseReceiptRequest request) {
        PurchaseReceipt receipt = new PurchaseReceipt();
        receipt.setTenantId(tenantId);
        receipt.setReceiptNo(request.getReceiptNo());
        receipt.setPurchaseOrderId(request.getPurchaseOrderId());
        receipt.setWarehouseId(request.getWarehouseId());
        receipt.setReceiptStatus(status);
        receipt.setFailureReason(null);
        receipt.setCreatedBy(operatorId);
        receipt.setUpdatedBy(operatorId);
        receipt.setDeleted(0);
        return receipt;
    }

    /**
     * 把创建请求中的明细行转换成收货单明细实体。
     */
    public PurchaseReceiptItem toNewReceiptItem(Long tenantId, Long receiptId, CreatePurchaseReceiptItemRequest request) {
        PurchaseReceiptItem item = new PurchaseReceiptItem();
        item.setTenantId(tenantId);
        item.setPurchaseReceiptId(receiptId);
        item.setMaterialId(request.getMaterialId());
        item.setLocationId(request.getLocationId());
        item.setReceiptQty(request.getReceiptQty());
        return item;
    }

    /**
     * 把单头和明细实体组装成返回视图。
     *
     * <p>失败原因会一并透传出去，便于前端和联调调用方直接看到问题。</p>
     */
    public PurchaseReceiptVO toVO(PurchaseReceipt receipt, List<PurchaseReceiptItem> items) {
        PurchaseReceiptVO vo = new PurchaseReceiptVO();
        vo.setId(receipt.getId());
        vo.setReceiptNo(receipt.getReceiptNo());
        vo.setPurchaseOrderId(receipt.getPurchaseOrderId());
        vo.setWarehouseId(receipt.getWarehouseId());
        vo.setReceiptStatus(receipt.getReceiptStatus());
        vo.setFailureReason(receipt.getFailureReason());
        vo.setItems(items.stream().map(this::toItemVO).toList());
        return vo;
    }

    /**
     * 把单条收货明细转换成视图对象。
     */
    private PurchaseReceiptItemVO toItemVO(PurchaseReceiptItem item) {
        PurchaseReceiptItemVO vo = new PurchaseReceiptItemVO();
        vo.setId(item.getId());
        vo.setMaterialId(item.getMaterialId());
        vo.setLocationId(item.getLocationId());
        vo.setReceiptQty(item.getReceiptQty());
        return vo;
    }
}
