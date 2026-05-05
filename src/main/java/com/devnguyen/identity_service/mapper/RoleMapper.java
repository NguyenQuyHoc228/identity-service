package com.devnguyen.identity_service.mapper;

import com.devnguyen.identity_service.dto.request.RoleRequest;
import com.devnguyen.identity_service.dto.response.RoleResponse;
import com.devnguyen.identity_service.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    /*
     * @Mapping(target = "permissions", ignore = true):
     * Lý do tương tự UserMapper: RoleRequest.permissions là Set<String>,
     * Role.permissions là Set<Permission> entity → Service xử lý
     */
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);

    /*
     * toRoleResponse: Role → RoleResponse
     * Role.permissions = Set<Permission> → RoleResponse.permissions = Set<PermissionResponse>
     * MapStruct sẽ tự dùng PermissionMapper.toPermissionResponse() để convert
     */
    RoleResponse toRoleResponse(Role role);
}