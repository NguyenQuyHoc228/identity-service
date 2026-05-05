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
        // Map request → entity (permissions bị ignore trong mapper)
        Role role = roleMapper.toRole(request);

        /*
         * Tại sao cần findAllById thay vì chỉ lưu tên permission?
         *
         * request.getPermissions() = Set<String> = {"CREATE_DATA", "READ_DATA"}
         * Role.permissions = Set<Permission> = cần Permission entity thực sự
         *
         * findAllById(ids): một câu query duy nhất lấy tất cả Permission có name trong list
         * → SELECT * FROM permission WHERE name IN ('CREATE_DATA', 'READ_DATA')
         * → Hiệu quả hơn gọi findById nhiều lần (N queries)
         *
         * HashSet: wrap lại vì findAllById trả về List, ta cần Set
         * (Role.permissions là Set<Permission>)
         */
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