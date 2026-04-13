package com.example.scm.mdm.controller;

import com.example.scm.common.core.Result;
import com.example.scm.mdm.dto.CreateMaterialRequest;
import com.example.scm.mdm.dto.UpdateMaterialRequest;
import com.example.scm.mdm.service.MaterialService;
import com.example.scm.mdm.vo.MaterialVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/v1/materials")
@Tag(name = "MDM-Material")
@Slf4j
public class MaterialController {

    private final MaterialService materialService;

    @Autowired
    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping({"", "/"})
    @Operation(summary = "Create material", description = "Create material master data under current tenant.")
    public Result<MaterialVO> create(@Valid @RequestBody CreateMaterialRequest request) {
        log.info("Receive create material request, materialCode={}", request.getMaterialCode());
        return Result.success(materialService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update material", description = "Update material basic attributes by id.")
    public Result<MaterialVO> update(@PathVariable("id") Long id, @Valid @RequestBody UpdateMaterialRequest request) {
        log.info("Receive update material request, id={}", id);
        return Result.success(materialService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get material detail", description = "Query one material under current tenant by id.")
    public Result<MaterialVO> getById(@PathVariable("id") Long id) {
        log.info("Receive get material detail request, id={}", id);
        return Result.success(materialService.getById(id));
    }

    @GetMapping({"", "/"})
    @Operation(summary = "List materials", description = "Query all non-deleted materials under current tenant.")
    public Result<List<MaterialVO>> list() {
        log.info("Receive list materials request");
        return Result.success(materialService.list());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete material", description = "Logical delete only, no physical deletion.")
    public Result<Void> delete(@PathVariable("id") Long id) {
        log.info("Receive delete material request, id={}", id);
        materialService.delete(id);
        return Result.success(null);
    }
}
