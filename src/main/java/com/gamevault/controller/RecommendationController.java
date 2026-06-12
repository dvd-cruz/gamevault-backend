package com.gamevault.controller;

import com.gamevault.dto.RecommendationRequest;
import com.gamevault.dto.RecommendationResponse;
import com.gamevault.security.UserPrincipal;
import com.gamevault.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recService;

    public RecommendationController(RecommendationService recService) {
        this.recService = recService;
    }

    @GetMapping
    public List<RecommendationResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return recService.getReceived(principal.getId());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return Map.of("count", recService.countUnread(principal.getId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecommendationResponse send(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RecommendationRequest req) {
        return recService.send(principal.getId(), req);
    }

    @PatchMapping("/{id}/read")
    public RecommendationResponse markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return recService.markRead(principal.getId(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismiss(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        recService.dismiss(principal.getId(), id);
    }
}
