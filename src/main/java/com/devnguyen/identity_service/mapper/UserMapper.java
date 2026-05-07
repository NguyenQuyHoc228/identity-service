package com.devnguyen.identity_service.mapper;

import com.devnguyen.identity_service.dto.request.UserCreationRequest;
import com.devnguyen.identity_service.dto.request.UserUpdateRequest;
import com.devnguyen.identity_service.dto.response.UserResponse;
import com.devnguyen.identity_service.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", ignore = true)
    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "roles", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}