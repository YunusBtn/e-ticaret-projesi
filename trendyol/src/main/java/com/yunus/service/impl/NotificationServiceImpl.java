package com.yunus.service.impl;

import com.yunus.dto.notification.NotificationResponse;
import com.yunus.entity.Notification;
import com.yunus.entity.User;
import com.yunus.enums.NotificationType;
import com.yunus.exception.BusinessException;
import com.yunus.exception.ErrorType;
import com.yunus.mapper.NotificationMapper;
import com.yunus.repository.NotificationRepository;
import com.yunus.repository.UserRepository;
import com.yunus.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UserRepository userRepository;


    @Override
    public NotificationResponse createNotification(Long userId, String message, NotificationType type) {
        User user = userRepository.findById(userId).
                orElseThrow(() -> new BusinessException(ErrorType.NOT_FOUND, "Kullanıcı Bulunamadı, id : " + userId + " "));

        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .type(type)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        return notificationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(notificationMapper::toResponse)
                .toList();

    }


    @Override
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorType.NOT_FOUND, "Bildirim Bulunamadı : " + notificationId + " "));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {

        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
}
