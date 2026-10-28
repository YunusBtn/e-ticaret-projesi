package com.yunus.service;

import com.yunus.dto.common.PageResponse;
import com.yunus.dto.notification.NotificationResponse;
import com.yunus.enums.NotificationType;
import com.yunus.model.UserPrincipal;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    NotificationResponse createNotification(Long userId, String message, NotificationType type);

    PageResponse<NotificationResponse> getUserNotifications(UserPrincipal principal, Pageable pageable);

    void markAsRead(Long notificationId, UserPrincipal principal);

    long countUnread(UserPrincipal principal);


}
