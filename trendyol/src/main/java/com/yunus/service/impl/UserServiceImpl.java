package com.yunus.service.impl;


import com.yunus.dto.common.PageResponse;
import com.yunus.dto.user.UserCreateRequest;
import com.yunus.dto.user.UserResponse;
import com.yunus.entity.User;
import com.yunus.enums.OrderStatus;
import com.yunus.enums.Role;
import com.yunus.exception.BusinessException;
import com.yunus.exception.ErrorType;
import com.yunus.mapper.UserMapper;
import com.yunus.model.UserPrincipal;
import com.yunus.repository.NotificationRepository;
import com.yunus.repository.OrderRepository;
import com.yunus.repository.UserRepository;
import com.yunus.service.UserService;
import com.yunus.util.PageableValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Set<String> USER_SORT_FIELDS = Set.of("id", "username", "email", "role");

    private static final Set<OrderStatus> ACTIVE_ORDER_STATUSES =
            Set.of(OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.SHIPPED);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final NotificationRepository notificationRepository;
    private final OrderRepository orderRepository;

    @Override
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorType.DUPLICATE_ENTRY, "username" + request.getUsername());
        }
        User user = userMapper.toEntity(request);
        user.setRole(request.getRole() != null ? request.getRole() : Role.USER);

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id, UserPrincipal principal) {

        if (!principal.isAdmin() && !principal.getId().equals(id)) {
            throw userNotFound(id);
        }

        return userMapper.toResponse(findActiveUserOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {

        PageableValidator.validateSort(pageable, USER_SORT_FIELDS);

        Page<UserResponse> page = userRepository.findAllByDeletedAtIsNull(pageable)
                .map(userMapper::toResponse);

        return PageResponse.from(page);

    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserCreateRequest request) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> userNotFound(id));

        boolean demoting = request.getRole() != null && request.getRole() != Role.ADMIN;

        if (demoting && isLastAdmin(existingUser)) {
            throw new BusinessException(ErrorType.LAST_ADMIN_PROTECTED);
        }

        existingUser.setUsername(request.getUsername());
        existingUser.setEmail(request.getEmail());

        if (request.getRole() != null) {
            existingUser.setRole(request.getRole());
        }

        User updatedUser = userRepository.save(existingUser);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id, Long successorId) {

        User target = findActiveUserOrThrow(id);

        if (orderRepository.existsByUserIdAndStatusIn(id, ACTIVE_ORDER_STATUSES)) {
            throw new BusinessException(ErrorType.USER_HAS_ACTIVE_ORDERS);
        }

        if (isLastAdmin(target)) {
            if (successorId == null) {
                throw new BusinessException(ErrorType.LAST_ADMIN_PROTECTED,
                        "- devralacak kullanıcıyı successorId ile belirtin");
            }

            if (successorId.equals(id)) {
                throw new BusinessException(ErrorType.VALIDATION_ERROR, "- DEVRALAN KİŞİ SİLİNEN KİŞİ OLAMAZ");
            }

            User successor = findActiveUserOrThrow(successorId);
            successor.setRole(Role.ADMIN);

        }

        String token = UUID.randomUUID().toString();

        target.setUsername("deleted-" + token);
        target.setEmail("deleted-" + token + "@deleted.invalid");
        target.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        target.setRole(Role.USER);
        target.setDeletedAt(LocalDateTime.now());


        notificationRepository.deleteByUserId(id);
    }

    private boolean isLastAdmin(User user) {
        if (user.getRole() != Role.ADMIN) {
            return false;
        }
        return userRepository.findAllByRoleForUpdate(Role.ADMIN).size() <= 1;
    }

    private BusinessException userNotFound(Long id) {
        return new BusinessException(ErrorType.NOT_FOUND, "Kullanıcı bulunamadı, id: " + id);
    }

    private User findActiveUserOrThrow(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> userNotFound(id));
    }

}
