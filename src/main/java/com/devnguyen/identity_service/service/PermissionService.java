package com.devnguyen.identity_service.service;

import com.devnguyen.identity_service.dto.request.PermissionRequest;
import com.devnguyen.identity_service.dto.response.PermissionResponse;
import com.devnguyen.identity_service.entity.Permission;
import com.devnguyen.identity_service.mapper.PermissionMapper;
import com.devnguyen.identity_service.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    public PermissionResponse create(PermissionRequest request) {

        Permission permission = permissionMapper.toPermission(request);
        permission = permissionRepository.save(permission);
        return permissionMapper.toPermissionResponse(permission);
    }

    public List<PermissionResponse> getAll() {

        return permissionRepository.findAll()
                .stream()
                .map(permissionMapper::toPermissionResponse)
                .toList(); // Java 16+: .toList() thay vì .collect(Collectors.toList())
    }

    public void delete(String permissionName) {

        permissionRepository.deleteById(permissionName);
        log.info("Permission deleted: {}", permissionName);
    }
}