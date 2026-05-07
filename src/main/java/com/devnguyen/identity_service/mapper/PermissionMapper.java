package com.devnguyen.identity_service.mapper;

import com.devnguyen.identity_service.dto.request.PermissionRequest;
import com.devnguyen.identity_service.dto.response.PermissionResponse;
import com.devnguyen.identity_service.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);
}