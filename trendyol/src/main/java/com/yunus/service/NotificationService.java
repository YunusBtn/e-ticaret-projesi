package com.yunus.service;

import com.yunus.dto.notification.NotificationResponse;
import com.yunus.enums.NotificationType;
import com.yunus.model.UserPrincipal;

import java.util.List;

public interface NotificationService {

    NotificationResponse createNotification(Long userId, String message, NotificationType type);

    List<NotificationResponse> getUserNotifications(UserPrincipal principal);

    void markAsRead(Long notificationId, UserPrincipal principal);

    long countUnread(UserPrincipal principal);
}
