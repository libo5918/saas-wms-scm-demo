package com.example.scm.purchase.controller;

import com.example.scm.common.core.Result;
import com.example.scm.purchase.dto.CreatePurchaseOrderRequest;
import com.example.scm.purchase.service.PurchaseOrderService;
import com.example.scm.purchase.vo.PurchaseOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 采购订单控制器。
 */
@RestController
@RequestMapping("/api/v1/purchase-orders")
@Tag(name = "采购订单", description = "采购订单管理接口")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping({"", "/"})
    @Operation(summary = "创建采购订单", description = "创建采购订单主单和明细。")
    public Result<PurchaseOrderVO> create(@Valid @RequestBody CreatePurchaseOrderRequest request) {
        return Result.success(purchaseOrderService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询采购订单详情", description = "按主键查询采购订单头和明细。")
    public Result<PurchaseOrderVO> getById(@PathVariable("id") Long id) {
        return Result.success(purchaseOrderService.getById(id));
    }

    @GetMapping("/by-order-no")
    @Operation(summary = "按单号查询采购订单", description = "按业务单号查询采购订单头和明细。")
    public Result<PurchaseOrderVO> getByOrderNo(@RequestParam("orderNo") String orderNo) {
        return Result.success(purchaseOrderService.getByOrderNo(orderNo));
    }

    @GetMapping({"", "/"})
    @Operation(summary = "查询采购订单列表", description = "查询当前租户下的采购订单列表。")
    public Result<List<PurchaseOrderVO>> list() {
        return Result.success(purchaseOrderService.list());
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消采购订单", description = "仅允许取消未收货的采购订单。")
    public Result<PurchaseOrderVO> cancel(@PathVariable("id") Long id) {
        return Result.success(purchaseOrderService.cancel(id));
    }
}
