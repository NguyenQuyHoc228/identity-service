package com.devnguyen.identity_service.mapper;

import com.devnguyen.identity_service.dto.request.PermissionRequest;
import com.devnguyen.identity_service.dto.response.PermissionResponse;
import com.devnguyen.identity_service.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    /*
     * Permission.name = PermissionRequest.name → tự map
     * Permission.description = PermissionRequest.description → tự map
     * Không cần @Mapping nào cả
     */
    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);
}