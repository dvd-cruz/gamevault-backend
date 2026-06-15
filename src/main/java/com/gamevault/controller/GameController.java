package com.gamevault.controller;

import com.gamevault.dto.AddToLibraryRequest;
import com.gamevault.dto.GameRequest;
import com.gamevault.dto.GameResponse;
import com.gamevault.dto.PlaythroughRequest;
import com.gamevault.dto.SessionRequest;
import com.gamevault.security.UserPrincipal;
import com.gamevault.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public List<GameResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String search) {
        return gameService.getGames(principal.getId(), status, platform, genre, search);
    }

    @GetMapping("/{id}")
    public GameResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return gameService.getGame(principal.getId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddToLibraryRequest req) {
        return gameService.addToLibrary(principal.getId(), req);
    }

    /** Bulk-imports a Steam account's owned games into the user's library. */
    @PostMapping("/import/steam")
    public java.util.Map<String, Object> importSteam(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody java.util.Map<String, String> body) {
        return gameService.importSteamLibrary(principal.getId(), body.get("steamId"));
    }

    @PutMapping("/{id}")
    public GameResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody GameRequest req) {
        return gameService.updateGame(principal.getId(), id, req);
    }

    @PatchMapping("/{id}/review")
    public GameResponse updateReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> body) {
        Integer rating  = body.get("rating") != null ? ((Number) body.get("rating")).intValue() : null;
        String review   = body.get("review") != null ? body.get("review").toString() : null;
        String platform = body.get("reviewPlatform") != null ? body.get("reviewPlatform").toString() : null;
        return gameService.updateReview(principal.getId(), id, rating, review, platform);
    }

    @PostMapping("/{id}/favorite")
    public GameResponse toggleFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return gameService.toggleFavorite(principal.getId(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        gameService.deleteGame(principal.getId(), id);
    }

    @PostMapping("/{id}/playthroughs/{playthroughId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public GameResponse addSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long playthroughId,
            @Valid @RequestBody SessionRequest req) {
        return gameService.addSession(principal.getId(), id, playthroughId, req);
    }

    @DeleteMapping("/{id}/playthroughs/{playthroughId}/sessions/{sessionId}")
    public GameResponse deleteSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long playthroughId,
            @PathVariable Long sessionId) {
        return gameService.deleteSession(principal.getId(), id, playthroughId, sessionId);
    }

    @PostMapping("/{id}/playthroughs")
    @ResponseStatus(HttpStatus.CREATED)
    public GameResponse addPlaythrough(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody PlaythroughRequest req) {
        return gameService.addPlaythrough(principal.getId(), id, req);
    }

    @PutMapping("/{id}/playthroughs/{playthroughId}")
    public GameResponse updatePlaythrough(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long playthroughId,
            @Valid @RequestBody PlaythroughRequest req) {
        return gameService.updatePlaythrough(principal.getId(), id, playthroughId, req);
    }

    @DeleteMapping("/{id}/playthroughs/{playthroughId}")
    public GameResponse deletePlaythrough(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long playthroughId) {
        return gameService.deletePlaythrough(principal.getId(), id, playthroughId);
    }

    @PostMapping("/{id}/trophies/{trophyId}/unlock")
    public GameResponse unlockTrophy(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long trophyId) {
        return gameService.unlockTrophy(principal.getId(), id, trophyId);
    }

    @DeleteMapping("/{id}/trophies/{trophyId}/unlock")
    public GameResponse lockTrophy(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long trophyId) {
        return gameService.lockTrophy(principal.getId(), id, trophyId);
    }

    @PostMapping("/{id}/steam-sync")
    public GameResponse syncSteamAchievements(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return gameService.syncSteamAchievements(principal.getId(), id);
    }

    @PostMapping("/{id}/psn-sync")
    public GameResponse syncPsnTrophies(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return gameService.syncPsnTrophies(principal.getId(), id);
    }
}
