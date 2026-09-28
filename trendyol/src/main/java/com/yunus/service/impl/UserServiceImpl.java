package com.yunus.service.impl;

import com.yunus.entity.User;
import com.yunus.repository.UserRepository;
import com.yunus.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;


    @Override
    public User createUser(User user) {

        if (userRepository.existsByUsername(user.getUsername())){
            throw new RuntimeException("Username already exists");
        }

        return userRepository.save(user);
    }

    @Override
    public User getUserById(Long id) {
        return null;
    }

    @Override
    public List<User> getAllUsers() {
        return List.of();
    }

    @Override
    public User updateUser(Long id, User updatedUser) {
        return null;
    }

    @Override
    public void deleteUser(Long id) {

    }
}
