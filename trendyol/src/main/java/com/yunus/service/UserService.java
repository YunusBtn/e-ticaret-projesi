package com.yunus.service;

import com.yunus.dto.common.PageResponse;
import com.yunus.dto.user.UserCreateRequest;
import com.yunus.dto.user.UserResponse;
import com.yunus.entity.User;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    UserResponse createUser(UserCreateRequest request);
    UserResponse getUserById(Long id);
    UserResponse updateUser(Long id,UserCreateRequest request);
    PageResponse<UserResponse> getAllUsers(Pageable pageable);
    void deleteUser(Long id);





}
