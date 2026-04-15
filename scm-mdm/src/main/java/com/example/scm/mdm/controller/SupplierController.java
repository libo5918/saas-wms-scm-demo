package com.example.scm.mdm.controller;

import com.example.scm.common.core.Result;
import com.example.scm.mdm.dto.CreateSupplierRequest;
import com.example.scm.mdm.dto.UpdateSupplierRequest;
import com.example.scm.mdm.service.SupplierService;
import com.example.scm.mdm.vo.SupplierVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@Tag(name = "供应商主数据", description = "供应商主数据管理接口")
@Slf4j
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping({"", "/"})
    @Operation(summary = "创建供应商", description = "在当前租户下创建供应商主数据。")
    public Result<SupplierVO> create(@Valid @RequestBody CreateSupplierRequest request) {
        return Result.success(supplierService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改供应商", description = "根据供应商ID修改供应商基础属性。")
    public Result<SupplierVO> update(@PathVariable("id") Long id, @Valid @RequestBody UpdateSupplierRequest request) {
        return Result.success(supplierService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询供应商详情", description = "根据供应商ID查询当前租户下的供应商详情。")
    public Result<SupplierVO> getById(@PathVariable("id") Long id) {
        return Result.success(supplierService.getById(id));
    }

    @GetMapping({"", "/"})
    @Operation(summary = "查询供应商列表", description = "查询当前租户下全部未删除的供应商。")
    public Result<List<SupplierVO>> list() {
        return Result.success(supplierService.list());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除供应商", description = "逻辑删除供应商，不做物理删除。")
    public Result<Void> delete(@PathVariable("id") Long id) {
        supplierService.delete(id);
        return Result.success(null);
    }
}
