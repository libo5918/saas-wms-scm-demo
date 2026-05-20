package com.example.scm.mdm;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.TenantContext;
import com.example.scm.mdm.entity.Material;
import com.example.scm.mdm.mapper.MaterialMapper;
import com.example.scm.mdm.service.impl.MaterialServiceImpl;
import com.example.scm.mdm.support.MaterialAssembler;
import com.example.scm.mdm.support.MaterialValidator;
import com.example.scm.mdm.vo.MaterialVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceImplTest {

    @Mock
    private MaterialMapper materialMapper;

    @Mock
    private MaterialValidator materialValidator;

    @Mock
    private MaterialAssembler materialAssembler;

    @InjectMocks
    private MaterialServiceImpl materialService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldQueryMaterialByCode() {
        TenantContext.setTenantId(1L);
        Material material = new Material();
        material.setId(10L);
        material.setMaterialCode("MAT-001");
        MaterialVO materialVO = new MaterialVO();
        materialVO.setId(10L);
        materialVO.setMaterialCode("MAT-001");

        when(materialMapper.selectByCode(1L, "MAT-001")).thenReturn(Optional.of(material));
        when(materialAssembler.toVO(material)).thenReturn(materialVO);

        MaterialVO result = materialService.getByCode("MAT-001");

        assertEquals(10L, result.getId());
        assertEquals("MAT-001", result.getMaterialCode());
        verify(materialMapper).selectByCode(1L, "MAT-001");
    }

    @Test
    void shouldThrowWhenMaterialCodeNotFound() {
        TenantContext.setTenantId(1L);
        when(materialMapper.selectByCode(1L, "MAT-404")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> materialService.getByCode("MAT-404"));

        assertEquals("Material not found", exception.getMessage());
    }
}
