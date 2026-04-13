package com.example.scm.purchase.support;

import com.example.scm.purchase.dto.CreatePurchaseReceiptItemRequest;
import com.example.scm.purchase.dto.CreatePurchaseReceiptRequest;
import com.example.scm.purchase.entity.PurchaseReceipt;
import com.example.scm.purchase.entity.PurchaseReceiptItem;
import com.example.scm.purchase.vo.PurchaseReceiptItemVO;
import com.example.scm.purchase.vo.PurchaseReceiptVO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PurchaseReceiptAssembler {

    public PurchaseReceipt toNewReceipt(Long tenantId, Long operatorId, String status, CreatePurchaseReceiptRequest request) {
        PurchaseReceipt receipt = new PurchaseReceipt();
        receipt.setTenantId(tenantId);
        receipt.setReceiptNo(request.getReceiptNo());
        receipt.setPurchaseOrderId(request.getPurchaseOrderId());
        receipt.setWarehouseId(request.getWarehouseId());
        receipt.setReceiptStatus(status);
        receipt.setCreatedBy(operatorId);
        receipt.setUpdatedBy(operatorId);
        receipt.setDeleted(0);
        return receipt;
    }

    public PurchaseReceiptItem toNewReceiptItem(Long tenantId, Long receiptId, CreatePurchaseReceiptItemRequest request) {
        PurchaseReceiptItem item = new PurchaseReceiptItem();
        item.setTenantId(tenantId);
        item.setPurchaseReceiptId(receiptId);
        item.setMaterialId(request.getMaterialId());
        item.setLocationId(request.getLocationId());
        item.setReceiptQty(request.getReceiptQty());
        return item;
    }

    public PurchaseReceiptVO toVO(PurchaseReceipt receipt, List<PurchaseReceiptItem> items) {
        PurchaseReceiptVO vo = new PurchaseReceiptVO();
        vo.setId(receipt.getId());
        vo.setReceiptNo(receipt.getReceiptNo());
        vo.setPurchaseOrderId(receipt.getPurchaseOrderId());
        vo.setWarehouseId(receipt.getWarehouseId());
        vo.setReceiptStatus(receipt.getReceiptStatus());
        vo.setItems(items.stream().map(this::toItemVO).toList());
        return vo;
    }

    private PurchaseReceiptItemVO toItemVO(PurchaseReceiptItem item) {
        PurchaseReceiptItemVO vo = new PurchaseReceiptItemVO();
        vo.setId(item.getId());
        vo.setMaterialId(item.getMaterialId());
        vo.setLocationId(item.getLocationId());
        vo.setReceiptQty(item.getReceiptQty());
        return vo;
    }
}
