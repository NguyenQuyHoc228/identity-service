package com.devnguyen.identity_service.mapper;

import com.devnguyen.identity_service.dto.request.UserCreationRequest;
import com.devnguyen.identity_service.dto.request.UserUpdateRequest;
import com.devnguyen.identity_service.dto.response.UserResponse;
import com.devnguyen.identity_service.entity.User;
import org.mapstruct.*;

/*
 * @Mapper(componentModel = "spring"):
 *   componentModel = "spring" → MapStruct generate implementation như một Spring @Component
 *   → Có thể @Autowired / @Inject vào Service như Bean bình thường
 *
 *   Nếu không có componentModel:
 *   → MapStruct tạo class thông thường, phải dùng: UserMapper.INSTANCE
 *   → Không tích hợp được với Spring DI
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /*
     * toUser: map UserCreationRequest → User entity.
     *
     * @Mapping(target = "roles", ignore = true):
     * Tại sao ignore roles?
     * → UserCreationRequest.roles là Set<String> (tên role)
     * → User.roles là Set<Role> (entity objects)
     * → MapStruct không tự biết cách convert String → Role entity
     *   (cần query DB để lookup Role)
     * → Việc này thuộc trách nhiệm của Service, không phải Mapper
     * → Ignore ở đây, Service sẽ tự set roles sau khi lookup từ DB
     */
    @Mapping(target = "roles", ignore = true)
    User toUser(UserCreationRequest request);

    /*
     * toUserResponse: map User entity → UserResponse DTO.
     *
     * Field có tên giống nhau (id, username, firstName, lastName, dob, roles)
     * → MapStruct tự map, không cần @Mapping
     *
     * Nhưng roles: User.roles = Set<Role>, UserResponse.roles = Set<RoleResponse>
     * → MapStruct sẽ tự tìm RoleMapper.toRoleResponse() để convert
     *    (nếu RoleMapper cũng được đăng ký là Spring Bean)
     * → Đây là tính năng "nested mapping" của MapStruct
     */
    UserResponse toUserResponse(User user);

    /*
     * updateUser: update User entity từ UserUpdateRequest.
     *
     * @MappingTarget User user: thay vì tạo User mới, MapStruct UPDATE object đã có.
     * Tại sao cần điều này?
     * → Nếu tạo User mới: mất hết data không có trong request (id, username...)
     * → @MappingTarget: chỉ update field có trong request, giữ nguyên field khác
     *
     * @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE):
     * Nếu field trong request là null → KHÔNG update field đó trong entity.
     * Ví dụ: request chỉ có firstName, password = null
     * → Không set user.password = null (không xóa password!)
     * → Chỉ update firstName
     * Đây là "partial update" / PATCH behavior
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "roles", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}