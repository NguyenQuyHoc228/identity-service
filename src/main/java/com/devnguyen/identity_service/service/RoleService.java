package com.devnguyen.identity_service.service;

import com.devnguyen.identity_service.dto.request.RoleRequest;
import com.devnguyen.identity_service.dto.response.RoleResponse;
import com.devnguyen.identity_service.entity.Role;
import com.devnguyen.identity_service.mapper.RoleMapper;
import com.devnguyen.identity_service.repository.PermissionRepository;
import com.devnguyen.identity_service.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    public RoleResponse create(RoleRequest request) {
        // Map request → entity
        Role role = roleMapper.toRole(request);

        var permissions = permissionRepository.findAllById(request.getPermissions());
        role.setPermissions(new HashSet<>(permissions));

        role = roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    public List<RoleResponse> getAll() {
        return roleRepository.findAll()
                .stream()
                .map(roleMapper::toRoleResponse)
                .toList();
    }

    public void delete(String roleName) {
        roleRepository.deleteById(roleName);
        log.info("Role deleted: {}", roleName);
    }
}