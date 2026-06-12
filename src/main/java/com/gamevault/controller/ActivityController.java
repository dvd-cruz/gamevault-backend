package com.gamevault.controller;

import com.gamevault.dto.ActivityResponse;
import com.gamevault.dto.GameResponse;
import com.gamevault.model.User;
import com.gamevault.repository.UserRepository;
import com.gamevault.security.UserPrincipal;
import com.gamevault.service.ActivityService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityService activityService;
    private final UserRepository userRepo;

    public ActivityController(ActivityService activityService, UserRepository userRepo) {
        this.activityService = activityService;
        this.userRepo = userRepo;
    }

    @GetMapping("/feed")
    public List<ActivityResponse> feed(@AuthenticationPrincipal UserPrincipal principal) {
        return activityService.getFriendFeed(requireUser(principal));
    }

    @GetMapping("/{id}")
    public ActivityResponse getPost(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return activityService.getPost(requireUser(principal), id);
    }

    @GetMapping("/{id}/trophies")
    public List<GameResponse.GameTrophyResponse> getActivityTrophies(@PathVariable Long id) {
        return activityService.getTrophiesForActivity(id);
    }

    @PatchMapping("/{id}")
    public ActivityResponse editPost(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id,
                                     @RequestBody Map<String, String> body) {
        return activityService.editPost(requireUser(principal), id, body.get("text"));
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> deletePost(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        activityService.deletePost(requireUser(principal), id);
        return Map.of("deleted", true);
    }

    @PostMapping("/{id}/like")
    public ActivityResponse toggleLike(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return activityService.toggleLike(requireUser(principal), id);
    }

    @PostMapping("/{id}/comments")
    public ActivityResponse addComment(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id,
                                       @RequestBody Map<String, String> body) {
        return activityService.addComment(requireUser(principal), id, body.get("text"));
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ActivityResponse deleteComment(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id,
                                          @PathVariable Long commentId) {
        return activityService.deleteComment(requireUser(principal), id, commentId);
    }

    private User requireUser(UserPrincipal principal) {
        return userRepo.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
    }
}
