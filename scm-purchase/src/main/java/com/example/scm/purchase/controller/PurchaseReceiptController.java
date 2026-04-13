package com.example.scm.purchase.controller;

import com.example.scm.common.core.Result;
import com.example.scm.purchase.dto.CreatePurchaseReceiptRequest;
import com.example.scm.purchase.service.PurchaseReceiptService;
import com.example.scm.purchase.vo.PurchaseReceiptVO;
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
@RequestMapping("/api/v1/purchase-receipts")
@Tag(name = "Purchase-Receipt")
@Slf4j
public class PurchaseReceiptController {

    private final PurchaseReceiptService purchaseReceiptService;

    public PurchaseReceiptController(PurchaseReceiptService purchaseReceiptService) {
        this.purchaseReceiptService = purchaseReceiptService;
    }

    @PostMapping({"", "/"})
    @Operation(summary = "创建采购收货单", description = "创建采购收货单头和收货明细。")
    public Result<PurchaseReceiptVO> create(@Valid @RequestBody CreatePurchaseReceiptRequest request) {
        log.info("Receive create purchase receipt request, receiptNo={}", request.getReceiptNo());
        return Result.success(purchaseReceiptService.create(request));
    }

    @PostMapping("/{id}/retry-stock-in")
    @Operation(summary = "重试收货单入库", description = "针对同一张入库失败的采购收货单重新触发库存入库。")
    public Result<PurchaseReceiptVO> retryStockIn(@PathVariable("id") Long id) {
        log.info("Receive retry purchase receipt stock-in request, id={}", id);
        return Result.success(purchaseReceiptService.retryStockIn(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消收货单", description = "取消未完成或入库失败的采购收货单。")
    public Result<PurchaseReceiptVO> cancel(@PathVariable("id") Long id) {
        log.info("Receive cancel purchase receipt request, id={}", id);
        return Result.success(purchaseReceiptService.cancel(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询收货单详情", description = "根据收货单ID查询单头和明细信息。")
    public Result<PurchaseReceiptVO> getById(@PathVariable("id") Long id) {
        log.info("Receive get purchase receipt detail request, id={}", id);
        return Result.success(purchaseReceiptService.getById(id));
    }

    @GetMapping("/by-receipt-no")
    @Operation(summary = "按单号查询收货单详情", description = "根据业务收货单号查询单头和明细信息。")
    public Result<PurchaseReceiptVO> getByReceiptNo(@RequestParam("receiptNo") String receiptNo) {
        log.info("Receive get purchase receipt detail by receiptNo request, receiptNo={}", receiptNo);
        return Result.success(purchaseReceiptService.getByReceiptNo(receiptNo));
    }

    @GetMapping({"", "/"})
    @Operation(summary = "查询收货单列表", description = "查询当前租户下的采购收货单列表。")
    public Result<List<PurchaseReceiptVO>> list() {
        log.info("Receive list purchase receipts request");
        return Result.success(purchaseReceiptService.list());
    }
}
