package com.gamevault.controller;

import com.gamevault.dto.UserResponse;
import com.gamevault.security.UserPrincipal;
import com.gamevault.service.BlockService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blocks")
public class BlockController {

    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @GetMapping
    public List<UserResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return blockService.listBlocked(principal.getId());
    }

    @PostMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String username) {
        blockService.block(principal.getId(), username);
    }

    @DeleteMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String username) {
        blockService.unblock(principal.getId(), username);
    }
}
