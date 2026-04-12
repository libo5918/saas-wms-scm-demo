package com.example.scm.mdm.controller;

import com.example.scm.common.core.Result;
import com.example.scm.mdm.dto.CreateMaterialRequest;
import com.example.scm.mdm.dto.UpdateMaterialRequest;
import com.example.scm.mdm.service.MaterialService;
import com.example.scm.mdm.vo.MaterialVO;
import jakarta.validation.Valid;
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
public class MaterialController {

    private final MaterialService materialService;

    @Autowired
    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping
    public Result<MaterialVO> create(@Valid @RequestBody CreateMaterialRequest request) {
        return Result.success(materialService.create(request));
    }

    @PutMapping("/{id}")
    public Result<MaterialVO> update(@PathVariable("id") Long id, @Valid @RequestBody UpdateMaterialRequest request) {
        return Result.success(materialService.update(id, request));
    }

    @GetMapping("/{id}")
    public Result<MaterialVO> getById(@PathVariable("id") Long id) {
        return Result.success(materialService.getById(id));
    }

    @GetMapping
    public Result<List<MaterialVO>> list() {
        return Result.success(materialService.list());
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        materialService.delete(id);
        return Result.success(null);
    }
}
