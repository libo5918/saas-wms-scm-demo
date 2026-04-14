package com.example.scm.sales.controller;

import com.example.scm.common.core.Result;
import com.example.scm.sales.dto.CreateSalesOrderRequest;
import com.example.scm.sales.service.SalesOrderService;
import com.example.scm.sales.vo.SalesOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sales-orders")
@Tag(name = "Sales-Order")
@Slf4j
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    public SalesOrderController(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }

    @PostMapping({"", "/"})
    @Operation(summary = "创建销售订单", description = "创建销售订单并自动触发库存锁定。")
    public Result<SalesOrderVO> create(@Valid @RequestBody CreateSalesOrderRequest request) {
        return Result.success(salesOrderService.create(request));
    }

    @PostMapping("/{id}/retry-lock")
    @Operation(summary = "重试销售订单锁库", description = "针对锁库失败的销售订单重新触发库存锁定。")
    public Result<SalesOrderVO> retryLock(@PathVariable("id") Long id) {
        return Result.success(salesOrderService.retryLock(id));
    }

    @PostMapping("/{id}/ship")
    @Operation(summary = "销售订单发货", description = "对已锁库的销售订单执行出库发货。")
    public Result<SalesOrderVO> ship(@PathVariable("id") Long id) {
        return Result.success(salesOrderService.ship(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消销售订单", description = "取消销售订单，已锁库时自动解锁库存。")
    public Result<SalesOrderVO> cancel(@PathVariable("id") Long id) {
        return Result.success(salesOrderService.cancel(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询销售订单详情", description = "按主键查询销售订单头和明细。")
    public Result<SalesOrderVO> getById(@PathVariable("id") Long id) {
        return Result.success(salesOrderService.getById(id));
    }

    @GetMapping("/by-order-no")
    @Operation(summary = "按单号查询销售订单", description = "按销售单号查询销售订单头和明细。")
    public Result<SalesOrderVO> getByOrderNo(@RequestParam("orderNo") String orderNo) {
        return Result.success(salesOrderService.getByOrderNo(orderNo));
    }

    @GetMapping({"", "/"})
    @Operation(summary = "查询销售订单列表", description = "查询当前租户下的销售订单列表。")
    public Result<List<SalesOrderVO>> list() {
        return Result.success(salesOrderService.list());
    }
}
