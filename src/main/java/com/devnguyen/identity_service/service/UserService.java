package com.devnguyen.identity_service.service;

import com.devnguyen.identity_service.dto.request.UserCreationRequest;
import com.devnguyen.identity_service.dto.request.UserUpdateRequest;
import com.devnguyen.identity_service.dto.response.UserResponse;
import com.devnguyen.identity_service.entity.User;
import com.devnguyen.identity_service.enums.PredefinedRole;
import com.devnguyen.identity_service.exception.AppException;
import com.devnguyen.identity_service.exception.ErrorCode;
import com.devnguyen.identity_service.mapper.UserMapper;
import com.devnguyen.identity_service.repository.RoleRepository;
import com.devnguyen.identity_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(UserCreationRequest request) {

        if (userRepository.existsByUsername(request.getUsername()))
            throw new AppException(ErrorCode.USER_EXISTED);

        User user = userMapper.toUser(request);

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        var roles = new HashSet<>(roleRepository.findAllById(
                request.getRoles() != null
                        ? request.getRoles()
                        : List.of(PredefinedRole.USER_ROLE)
        ));
        user.setRoles(roles);

        user = userRepository.save(user);

        log.info("User created: {}", user.getUsername());

        return userMapper.toUserResponse(user);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<UserResponse> getUsers() {
        log.info("Getting all users");
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
    }


    @PostAuthorize("returnObject.username == authentication.name or hasAuthority('ROLE_ADMIN')")
    public UserResponse getUser(String id) {
        log.info("Getting user: {}", id);
        return userMapper.toUserResponse(
                userRepository.findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED))
        );
    }


    public UserResponse getMyInfo() {

        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(String userId, UserUpdateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        userMapper.updateUser(user, request);


        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Cập nhật roles nếu request có gửi
        if (request.getRoles() != null) {
            var roles = new HashSet<>(roleRepository.findAllById(request.getRoles()));
            user.setRoles(roles);
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(String userId) {

        if (!userRepository.existsById(userId))
            throw new AppException(ErrorCode.USER_NOT_EXISTED);

        userRepository.deleteById(userId);
        log.info("User deleted: {}", userId);
    }
}