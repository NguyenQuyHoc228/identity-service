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

/*
 * @Service: đánh dấu đây là Spring Bean thuộc Service layer.
 * Về mặt kỹ thuật @Service = @Component, nhưng về mặt semantic:
 * → Cho developer biết đây là business logic layer
 * → Spring có thể xử lý khác nhau trong tương lai (AOP, transaction...)
 *
 * @RequiredArgsConstructor: Lombok generate constructor với tất cả field final.
 * Tại sao dùng constructor injection thay vì @Autowired field injection?
 *
 * Field injection (@Autowired trực tiếp trên field):
 *   ❌ Không thể tạo object mà không có Spring container → khó unit test
 *   ❌ Field là private nhưng Spring dùng reflection để inject → "magic", khó hiểu
 *   ❌ Dependency không rõ ràng (không thấy trong constructor)
 *   ❌ Có thể tạo circular dependency mà không bị phát hiện sớm
 *
 * Constructor injection:
 *   ✅ Dependency rõ ràng, bắt buộc phải provide khi tạo object
 *   ✅ Dễ unit test: new PermissionService(mockRepo, mockMapper)
 *   ✅ Immutable (field final)
 *   ✅ Spring team recommend từ Spring 4.3+
 *
 * @RequiredArgsConstructor + final fields = constructor injection tự động
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    // final → immutable → thread-safe, không ai reassign được
    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    public PermissionResponse create(PermissionRequest request) {
        /*
         * Flow tạo Permission:
         * 1. Map request DTO → Entity (MapStruct)
         * 2. Save entity → DB (JPA)
         * 3. Map entity → Response DTO (MapStruct)
         * 4. Return response DTO
         *
         * Tại sao không check duplicate?
         * Permission.name là PK → DB tự throw exception nếu trùng
         * → Spring translate thành DataIntegrityViolationException
         * → GlobalExceptionHandler catch và trả về 500
         * (Có thể improve thêm bằng cách check trước và throw AppException 400)
         */
        Permission permission = permissionMapper.toPermission(request);
        permission = permissionRepository.save(permission);
        return permissionMapper.toPermissionResponse(permission);
    }

    public List<PermissionResponse> getAll() {
        /*
         * findAll() trả List<Permission> → stream() → map từng cái → collect
         * Tại sao không dùng for loop?
         * Stream + method reference ngắn gọn và functional hơn.
         * permissionMapper::toPermissionResponse = lambda p -> permissionMapper.toPermissionResponse(p)
         */
        return permissionRepository.findAll()
                .stream()
                .map(permissionMapper::toPermissionResponse)
                .toList(); // Java 16+: .toList() thay vì .collect(Collectors.toList())
    }

    public void delete(String permissionName) {
        /*
         * deleteById: nếu không tồn tại → Spring Data throw EmptyResultDataAccessException
         * (trong Spring Boot 3 thì không throw, chỉ ignore)
         * Nếu muốn strict → check existsById trước rồi mới delete
         */
        permissionRepository.deleteById(permissionName);
        log.info("Permission deleted: {}", permissionName);
    }
}