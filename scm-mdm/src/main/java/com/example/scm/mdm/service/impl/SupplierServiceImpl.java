package com.example.scm.mdm.service.impl;

import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.mdm.dto.CreateSupplierRequest;
import com.example.scm.mdm.dto.UpdateSupplierRequest;
import com.example.scm.mdm.entity.Supplier;
import com.example.scm.mdm.mapper.SupplierMapper;
import com.example.scm.mdm.service.SupplierService;
import com.example.scm.mdm.support.SupplierAssembler;
import com.example.scm.mdm.support.SupplierValidator;
import com.example.scm.mdm.vo.SupplierVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SupplierServiceImpl implements SupplierService {

    private static final long SYSTEM_OPERATOR_ID = 1L;

    private final SupplierMapper supplierMapper;
    private final SupplierValidator supplierValidator;
    private final SupplierAssembler supplierAssembler;

    public SupplierServiceImpl(SupplierMapper supplierMapper,
                               SupplierValidator supplierValidator,
                               SupplierAssembler supplierAssembler) {
        this.supplierMapper = supplierMapper;
        this.supplierValidator = supplierValidator;
        this.supplierAssembler = supplierAssembler;
    }

    @Override
    @Transactional
    public SupplierVO create(CreateSupplierRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        supplierValidator.validateForCreate(request);
        supplierMapper.selectByCode(tenantId, request.getSupplierCode()).ifPresent(supplier -> {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "Supplier code already exists");
        });
        Supplier supplier = supplierAssembler.toNewEntity(tenantId, SYSTEM_OPERATOR_ID, request);
        supplierMapper.insert(supplier);
        return supplierAssembler.toVO(supplier);
    }

    @Override
    @Transactional
    public SupplierVO update(Long id, UpdateSupplierRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        supplierValidator.validateForUpdate(request);
        Supplier supplier = supplierMapper.selectById(tenantId, id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Supplier not found"));
        supplier.setSupplierName(request.getSupplierName());
        supplier.setContactName(request.getContactName());
        supplier.setContactPhone(request.getContactPhone());
        supplier.setStatus(request.getStatus());
        supplier.setUpdatedBy(SYSTEM_OPERATOR_ID);
        int affected = supplierMapper.update(supplier);
        if (affected == 0) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Update supplier failed");
        }
        return supplierAssembler.toVO(supplier);
    }

    @Override
    public SupplierVO getById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        return supplierMapper.selectById(tenantId, id)
                .map(supplierAssembler::toVO)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Supplier not found"));
    }

    @Override
    public List<SupplierVO> list() {
        Long tenantId = TenantContext.getRequiredTenantId();
        return supplierMapper.selectByTenantId(tenantId).stream().map(supplierAssembler::toVO).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        int affected = supplierMapper.softDelete(tenantId, id, SYSTEM_OPERATOR_ID);
        if (affected == 0) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND.code(), "Supplier not found");
        }
    }
}
