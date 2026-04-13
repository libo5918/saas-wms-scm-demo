package com.example.scm.purchase.mapper;

import com.example.scm.purchase.entity.PurchaseReceiptItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 采购收货单明细数据访问接口。
 */
@Mapper
public interface PurchaseReceiptItemMapper {

    /**
     * 按租户和收货单主键查询明细。
     */
    List<PurchaseReceiptItem> selectByReceiptId(@Param("tenantId") Long tenantId, @Param("purchaseReceiptId") Long purchaseReceiptId);

    /**
     * 新增收货单明细并回填主键。
     */
    @Insert("""
            INSERT INTO purchase_receipt_item(tenant_id, purchase_receipt_id, material_id, location_id, receipt_qty)
            VALUES(#{tenantId}, #{purchaseReceiptId}, #{materialId}, #{locationId}, #{receiptQty})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(PurchaseReceiptItem purchaseReceiptItem);
}
