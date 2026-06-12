package com.gamevault.controller;

import com.gamevault.dto.FriendRequestResponse;
import com.gamevault.dto.FriendStatusResponse;
import com.gamevault.dto.UserResponse;
import com.gamevault.security.UserPrincipal;
import com.gamevault.service.FriendshipService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @GetMapping
    public List<UserResponse> friends(@AuthenticationPrincipal UserPrincipal principal) {
        return friendshipService.listFriends(principal.getId());
    }

    @GetMapping("/requests")
    public List<FriendRequestResponse> requests(@AuthenticationPrincipal UserPrincipal principal) {
        return friendshipService.pendingRequests(principal.getId());
    }

    @GetMapping("/status/{username}")
    public FriendStatusResponse status(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String username) {
        return friendshipService.statusWith(principal.getId(), username);
    }

    @PostMapping("/requests/{username}")
    @ResponseStatus(HttpStatus.CREATED)
    public FriendRequestResponse sendRequest(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String username) {
        return friendshipService.sendRequest(principal.getId(), username);
    }

    @PostMapping("/requests/{id}/accept")
    public void accept(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        friendshipService.respondToRequest(principal.getId(), id, true);
    }

    @PostMapping("/requests/{id}/decline")
    public void decline(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        friendshipService.respondToRequest(principal.getId(), id, false);
    }

    @DeleteMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String username) {
        friendshipService.removeFriendOrCancelRequest(principal.getId(), username);
    }
}
