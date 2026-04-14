package com.example.scm.mdm.controller;

import com.example.scm.common.core.Result;
import com.example.scm.mdm.dto.CreateLocationRequest;
import com.example.scm.mdm.dto.UpdateLocationRequest;
import com.example.scm.mdm.service.LocationService;
import com.example.scm.mdm.vo.LocationVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@Tag(name = "库位主数据", description = "库位主数据管理接口")
@Slf4j
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping({"", "/"})
    @Operation(summary = "创建库位", description = "在当前租户下创建库位主数据。")
    public Result<LocationVO> create(@Valid @RequestBody CreateLocationRequest request) {
        log.info("Receive create location request, warehouseId={}, locationCode={}",
                request.getWarehouseId(), request.getLocationCode());
        return Result.success(locationService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改库位", description = "根据库位ID修改库位基础属性。")
    public Result<LocationVO> update(@PathVariable("id") Long id, @Valid @RequestBody UpdateLocationRequest request) {
        log.info("Receive update location request, id={}", id);
        return Result.success(locationService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询库位详情", description = "根据库位ID查询当前租户下的库位详情。")
    public Result<LocationVO> getById(@PathVariable("id") Long id) {
        log.info("Receive get location detail request, id={}", id);
        return Result.success(locationService.getById(id));
    }

    @GetMapping({"", "/"})
    @Operation(summary = "查询库位列表", description = "查询当前租户下全部未删除的库位，可按仓库过滤。")
    public Result<List<LocationVO>> list(@RequestParam(value = "warehouseId", required = false) Long warehouseId) {
        log.info("Receive list locations request, warehouseId={}", warehouseId);
        return Result.success(locationService.list(warehouseId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除库位", description = "逻辑删除库位，不做物理删除。")
    public Result<Void> delete(@PathVariable("id") Long id) {
        log.info("Receive delete location request, id={}", id);
        locationService.delete(id);
        return Result.success(null);
    }
}
