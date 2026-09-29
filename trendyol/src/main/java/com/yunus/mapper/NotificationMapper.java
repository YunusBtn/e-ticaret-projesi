package com.yunus.mapper;

import com.yunus.dto.notification.NotificationResponse;
import com.yunus.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponse toResponse(Notification notification);
}
