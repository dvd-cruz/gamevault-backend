package com.gamevault.controller;

import com.gamevault.dto.NotificationResponse;
import com.gamevault.model.User;
import com.gamevault.repository.UserRepository;
import com.gamevault.security.UserPrincipal;
import com.gamevault.service.NotificationService;
import com.gamevault.service.SseEmitterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterRegistry sseRegistry;
    private final UserRepository userRepo;

    public NotificationController(NotificationService notificationService, SseEmitterRegistry sseRegistry, UserRepository userRepo) {
        this.notificationService = notificationService;
        this.sseRegistry = sseRegistry;
        this.userRepo = userRepo;
    }

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepo.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
        return notificationService.getNotifications(user);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return sseRegistry.register(principal.getId());
    }

    @PostMapping("/read")
    public void markRead(@AuthenticationPrincipal UserPrincipal principal, @RequestBody Map<String, List<String>> body) {
        // read state is managed client-side; this endpoint exists for future server-side tracking
    }
}
