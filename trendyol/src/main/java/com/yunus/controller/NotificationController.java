package com.yunus.controller;

import com.yunus.dto.common.PageResponse;
import com.yunus.dto.notification.NotificationResponse;
import com.yunus.model.UserPrincipal;
import com.yunus.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/my")
    public ResponseEntity<PageResponse<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject @PageableDefault(size = 10, sort= "createdAt", direction = Sort.Direction.DESC)Pageable pageable) {
        return ResponseEntity.ok(notificationService.getUserNotifications(principal, pageable));
    }


    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAsRead(id, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my/unread-count")
    public ResponseEntity<Long> countMyUnread(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.countUnread(principal));
    }
}
