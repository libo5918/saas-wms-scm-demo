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
    @Operation(summary = "Create purchase receipt", description = "Create receipt header and receipt items.")
    public Result<PurchaseReceiptVO> create(@Valid @RequestBody CreatePurchaseReceiptRequest request) {
        log.info("Receive create purchase receipt request, receiptNo={}", request.getReceiptNo());
        return Result.success(purchaseReceiptService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase receipt detail", description = "Query header and items by receipt id.")
    public Result<PurchaseReceiptVO> getById(@PathVariable("id") Long id) {
        log.info("Receive get purchase receipt detail request, id={}", id);
        return Result.success(purchaseReceiptService.getById(id));
    }

    @GetMapping({"", "/"})
    @Operation(summary = "List purchase receipts", description = "Query purchase receipt list under current tenant.")
    public Result<List<PurchaseReceiptVO>> list() {
        log.info("Receive list purchase receipts request");
        return Result.success(purchaseReceiptService.list());
    }
}
