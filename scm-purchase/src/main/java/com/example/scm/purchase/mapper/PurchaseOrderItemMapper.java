package com.example.scm.purchase.mapper;

import com.example.scm.purchase.entity.PurchaseOrderItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 采购订单明细数据访问接口。
 */
@Mapper
public interface PurchaseOrderItemMapper {

    List<PurchaseOrderItem> selectByOrderId(@Param("tenantId") Long tenantId, @Param("purchaseOrderId") Long purchaseOrderId);

    @Insert("""
            INSERT INTO purchase_order_item(
                tenant_id, purchase_order_id, material_id, plan_qty, received_qty, unit_price
            )
            VALUES(
                #{tenantId}, #{purchaseOrderId}, #{materialId}, #{planQty}, #{receivedQty}, #{unitPrice}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(PurchaseOrderItem purchaseOrderItem);

    /**
     * 在不超出计划数量的前提下，递增采购订单明细的已收数量。
     *
     * <p>关键约束：{@code received_qty + incrementQty <= plan_qty}</p>
     * <p>语义说明：</p>
     * <p>1. 更新成功（返回 1）：本次回写有效，且未超收。</p>
     * <p>2. 更新失败（返回 0）：通常表示已超收，或该订单明细不存在。</p>
     *
     * <p>该判断放在 SQL 条件中，由数据库原子执行，可避免并发下的“先查后改”超收问题。</p>
     */
    @Update("""
            UPDATE purchase_order_item
            SET received_qty = received_qty + #{incrementQty}
            WHERE tenant_id = #{tenantId}
              AND purchase_order_id = #{purchaseOrderId}
              AND material_id = #{materialId}
              AND received_qty + #{incrementQty} <= plan_qty
            """)
    Integer increaseReceivedQtyIfWithinPlan(@Param("tenantId") Long tenantId,
                                            @Param("purchaseOrderId") Long purchaseOrderId,
                                            @Param("materialId") Long materialId,
                                            @Param("incrementQty") java.math.BigDecimal incrementQty);

    /**
     * 统计仍未完成收货的明细行数（received_qty < plan_qty）。
     */
    @Select("""
            SELECT COUNT(1)
            FROM purchase_order_item
            WHERE tenant_id = #{tenantId}
              AND purchase_order_id = #{purchaseOrderId}
              AND received_qty < plan_qty
            """)
    Integer countUnfinishedItemsByOrderId(@Param("tenantId") Long tenantId, @Param("purchaseOrderId") Long purchaseOrderId);

    /**
     * 统计已开始收货的明细行数（received_qty > 0）。
     */
    @Select("""
            SELECT COUNT(1)
            FROM purchase_order_item
            WHERE tenant_id = #{tenantId}
              AND purchase_order_id = #{purchaseOrderId}
              AND received_qty > 0
            """)
    Integer countStartedItemsByOrderId(@Param("tenantId") Long tenantId, @Param("purchaseOrderId") Long purchaseOrderId);
}
