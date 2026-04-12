package com.example.scm.mdm.service;

import com.example.scm.mdm.dto.CreateMaterialRequest;
import com.example.scm.mdm.dto.UpdateMaterialRequest;
import com.example.scm.mdm.vo.MaterialVO;

import java.util.List;

public interface MaterialService {

    MaterialVO create(CreateMaterialRequest request);

    MaterialVO update(Long id, UpdateMaterialRequest request);

    MaterialVO getById(Long id);

    List<MaterialVO> list();

    void delete(Long id);
}
