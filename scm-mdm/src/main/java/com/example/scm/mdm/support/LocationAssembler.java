package com.example.scm.mdm.support;

import com.example.scm.mdm.dto.CreateLocationRequest;
import com.example.scm.mdm.entity.Location;
import com.example.scm.mdm.vo.LocationVO;
import org.springframework.stereotype.Component;

@Component
public class LocationAssembler {

    public Location toNewEntity(Long tenantId, Long operatorId, CreateLocationRequest request) {
        Location location = new Location();
        location.setTenantId(tenantId);
        location.setWarehouseId(request.getWarehouseId());
        location.setLocationCode(request.getLocationCode());
        location.setLocationName(request.getLocationName());
        location.setLocationType(request.getLocationType());
        location.setStatus(request.getStatus());
        location.setCreatedBy(operatorId);
        location.setUpdatedBy(operatorId);
        location.setDeleted(0);
        return location;
    }

    public LocationVO toVO(Location location) {
        LocationVO vo = new LocationVO();
        vo.setId(location.getId());
        vo.setWarehouseId(location.getWarehouseId());
        vo.setLocationCode(location.getLocationCode());
        vo.setLocationName(location.getLocationName());
        vo.setLocationType(location.getLocationType());
        vo.setStatus(location.getStatus());
        return vo;
    }
}
