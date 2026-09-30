package com.yunus.service;

import com.yunus.dto.notification.NotificationResponse;
import com.yunus.enums.NotificationType;

import java.util.List;

public interface NotificationService {
    NotificationResponse createNotification(Long userId, String message, NotificationType type);

    List<NotificationResponse> getUserNotifications(Long userId);

    void markAsRead(Long notificationId);

    long countUnread(Long userId);
}
