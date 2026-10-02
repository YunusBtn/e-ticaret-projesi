package com.yunus.service.impl;


import com.yunus.dto.user.UserCreateRequest;
import com.yunus.dto.user.UserResponse;
import com.yunus.entity.User;
import com.yunus.enums.Role;
import com.yunus.exception.BusinessException;
import com.yunus.exception.ErrorType;
import com.yunus.mapper.UserMapper;
import com.yunus.repository.UserRepository;
import com.yunus.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorType.DUPLICATE_ENTRY, "username" + request.getUsername());
        }
        User user = userMapper.toEntity(request);
        user.setRole(request.getRole() != null ? request.getRole() : Role.USER);
        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorType.NOT_FOUND, "Kullanıcı, id: " + id));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream().
                map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse updateUser(Long id, UserCreateRequest request) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorType.NOT_FOUND, "Kullanıcı bulunamadı"));
        existingUser.setUsername(request.getUsername());
        existingUser.setEmail(request.getEmail());

        User updatedUser = userRepository.save(existingUser);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorType.NOT_FOUND, "Silinmek istenen Kullanıcı Bulunamadı"));
        userRepository.delete(user);

    }
}
