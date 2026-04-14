package com.example.scm.mdm.controller;

import com.example.scm.common.core.Result;
import com.example.scm.mdm.dto.CreateWarehouseRequest;
import com.example.scm.mdm.dto.UpdateWarehouseRequest;
import com.example.scm.mdm.service.WarehouseService;
import com.example.scm.mdm.vo.WarehouseVO;
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
@RequestMapping("/api/v1/warehouses")
@Tag(name = "仓库主数据", description = "仓库主数据管理接口")
@Slf4j
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @PostMapping({"", "/"})
    @Operation(summary = "创建仓库", description = "在当前租户下创建仓库主数据。")
    public Result<WarehouseVO> create(@Valid @RequestBody CreateWarehouseRequest request) {
        log.info("Receive create warehouse request, warehouseCode={}", request.getWarehouseCode());
        return Result.success(warehouseService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改仓库", description = "根据仓库ID修改仓库基础属性。")
    public Result<WarehouseVO> update(@PathVariable("id") Long id, @Valid @RequestBody UpdateWarehouseRequest request) {
        log.info("Receive update warehouse request, id={}", id);
        return Result.success(warehouseService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询仓库详情", description = "根据仓库ID查询当前租户下的仓库详情。")
    public Result<WarehouseVO> getById(@PathVariable("id") Long id) {
        log.info("Receive get warehouse detail request, id={}", id);
        return Result.success(warehouseService.getById(id));
    }

    @GetMapping({"", "/"})
    @Operation(summary = "查询仓库列表", description = "查询当前租户下全部未删除的仓库。")
    public Result<List<WarehouseVO>> list() {
        log.info("Receive list warehouses request");
        return Result.success(warehouseService.list());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除仓库", description = "逻辑删除仓库，不做物理删除。")
    public Result<Void> delete(@PathVariable("id") Long id) {
        log.info("Receive delete warehouse request, id={}", id);
        warehouseService.delete(id);
        return Result.success(null);
    }
}
