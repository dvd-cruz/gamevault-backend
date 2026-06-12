package com.gamevault.controller;

import com.gamevault.dto.ActivityResponse;
import com.gamevault.dto.CatalogGameRequest;
import com.gamevault.dto.CatalogGameResponse;
import com.gamevault.dto.PostRequest;
import com.gamevault.dto.TrophyRequest;
import com.gamevault.model.User;
import com.gamevault.repository.UserRepository;
import com.gamevault.security.UserPrincipal;
import com.gamevault.service.ActivityService;
import com.gamevault.service.GameCatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class GameCatalogController {

    private final GameCatalogService catalogService;
    private final ActivityService activityService;
    private final UserRepository userRepo;
    private final com.gamevault.service.PsnService psnService;

    public GameCatalogController(GameCatalogService catalogService, ActivityService activityService, UserRepository userRepo, com.gamevault.service.PsnService psnService) {
        this.catalogService = catalogService;
        this.activityService = activityService;
        this.userRepo = userRepo;
        this.psnService = psnService;
    }

    @GetMapping
    public List<CatalogGameResponse> browse(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String search) {
        return catalogService.browse(genre, platform, search);
    }

    @GetMapping("/{id}")
    public CatalogGameResponse get(@PathVariable Long id) {
        return catalogService.getById(id);
    }

    @GetMapping("/{id}/steam-price")
    public com.gamevault.dto.SteamPriceResponse steamPrice(@PathVariable Long id) {
        return catalogService.getSteamPrice(id);
    }

    @GetMapping("/{id}/psn-price")
    public com.gamevault.dto.PsnPriceResponse psnPrice(@PathVariable Long id) {
        return catalogService.getPsnPrice(id);
    }

    @GetMapping("/{id}/dlcs")
    public java.util.List<com.gamevault.dto.CatalogDlcResponse> dlcs(@PathVariable Long id) {
        return catalogService.getAllDlcs(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogGameResponse register(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CatalogGameRequest req) {
        return catalogService.register(principal.getId(), req);
    }

    @PutMapping("/{id}")
    public CatalogGameResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CatalogGameRequest req) {
        return catalogService.update(principal.getId(), id, req);
    }

    @PostMapping("/{id}/trophies")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogGameResponse addTrophy(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody TrophyRequest req) {
        return catalogService.addTrophy(principal.getId(), id, req);
    }

    /** Debug endpoint — lists the PSN trophy titles for the authenticated user so you can find NP Communication IDs */
    @GetMapping("/psn-titles")
    public java.util.List<com.gamevault.service.PsnService.TrophyTitleEntry> psnTitles(
            @AuthenticationPrincipal UserPrincipal principal) {
        com.gamevault.model.User user = userRepo.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (user.getNpsso() == null || user.getNpsso().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NPSSO não configurado");
        try {
            String accessToken = psnService.exchangeNpssoForAccessToken(user.getNpsso());
            String accountId   = psnService.fetchAccountId(accessToken);
            return psnService.fetchTrophyTitles(accessToken, accountId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    }

    @PostMapping("/{id}/psn-import-trophies")
    public CatalogGameResponse importPsnTrophies(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        com.gamevault.model.User user = userRepo.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!user.isAdmin())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas admins podem importar troféus PSN");
        if (user.getNpsso() == null || user.getNpsso().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configura o teu NPSSO nas Definições para importar troféus PSN");
        try {
            return catalogService.importPsnTrophies(id, user.getNpsso(), psnService);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    }

    @DeleteMapping("/{id}/trophies/{trophyId}")
    public CatalogGameResponse deleteTrophy(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long trophyId) {
        return catalogService.deleteTrophy(principal.getId(), id, trophyId);
    }

    @PostMapping("/{id}/dlcs")
    @ResponseStatus(HttpStatus.CREATED)
    public java.util.List<com.gamevault.dto.CatalogDlcResponse> addManualDlc(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody com.gamevault.dto.CatalogDlcRequest req) {
        return catalogService.addManualDlc(principal.getId(), id, req);
    }

    @PutMapping("/{id}/dlcs/{dlcId}")
    public java.util.List<com.gamevault.dto.CatalogDlcResponse> updateManualDlc(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long dlcId,
            @Valid @RequestBody com.gamevault.dto.CatalogDlcRequest req) {
        return catalogService.updateManualDlc(principal.getId(), id, dlcId, req);
    }

    @DeleteMapping("/{id}/dlcs/{dlcId}")
    public java.util.List<com.gamevault.dto.CatalogDlcResponse> deleteManualDlc(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long dlcId) {
        return catalogService.deleteManualDlc(principal.getId(), id, dlcId);
    }

    @GetMapping("/{id}/posts")
    public List<ActivityResponse> getPosts(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        User viewer = principal != null ? userRepo.findById(principal.getId()).orElse(null) : null;
        return activityService.getGamePosts(id, viewer);
    }

    @PostMapping("/{id}/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityResponse addPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody PostRequest req) {
        User actor = userRepo.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
        return activityService.createPost(actor, id, req.text(), req.image());
    }
}
