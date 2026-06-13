package com.gamevault.controller;

import com.gamevault.dto.TierListResponse;
import com.gamevault.model.GameCatalog;
import com.gamevault.model.TierList;
import com.gamevault.model.TierListEntry;
import com.gamevault.repository.GameCatalogRepository;
import com.gamevault.repository.TierListRepository;
import com.gamevault.repository.UserRepository;
import com.gamevault.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/tierlists")
public class TierListController {

    private static final Set<String> VALID_TIERS = Set.of("S", "A", "B", "C", "D");

    private final TierListRepository tierListRepo;
    private final GameCatalogRepository catalogRepo;
    private final UserRepository userRepo;

    public TierListController(TierListRepository tierListRepo, GameCatalogRepository catalogRepo, UserRepository userRepo) {
        this.tierListRepo = tierListRepo;
        this.catalogRepo = catalogRepo;
        this.userRepo = userRepo;
    }

    /** The logged-in user's tier lists. */
    @GetMapping
    public List<TierListResponse> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return tierListRepo.findByOwnerIdOrderByCreatedAtDesc(principal.getId())
                .stream().map(TierListResponse::from).toList();
    }

    /** Tier lists of any user — shown on their profile. */
    @GetMapping("/user/{username}")
    public List<TierListResponse> byUser(@PathVariable String username) {
        return tierListRepo.findByOwnerUsernameOrderByCreatedAtDesc(username)
                .stream().map(TierListResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TierListResponse one(@PathVariable Long id) {
        return TierListResponse.from(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TierListResponse create(@AuthenticationPrincipal UserPrincipal principal,
                                   @RequestBody Map<String, Object> body) {
        String title = body.get("title") != null ? body.get("title").toString().trim() : "";
        if (title.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O título é obrigatório");
        if (tierListRepo.findByOwnerIdOrderByCreatedAtDesc(principal.getId()).size() >= 3)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Só podes ter no máximo 3 tier lists");
        TierList t = new TierList();
        t.setTitle(title);
        t.setOwner(userRepo.findById(principal.getId()).orElseThrow());
        t.setCreatedAt(System.currentTimeMillis());
        return TierListResponse.from(tierListRepo.save(t));
    }

    /** Full update: title + the complete set of entries (replaces existing ones). */
    @PutMapping("/{id}")
    @Transactional
    public TierListResponse update(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable Long id,
                                   @RequestBody UpdateRequest body) {
        TierList t = findOwned(principal, id);
        if (body.title() != null && !body.title().isBlank()) t.setTitle(body.title().trim());
        t.getEntries().clear();
        if (body.entries() != null) {
            for (EntryRequest e : body.entries()) {
                if (!VALID_TIERS.contains(e.tier()))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tier inválido: " + e.tier());
                GameCatalog game = catalogRepo.findById(e.gameId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jogo não encontrado: " + e.gameId()));
                TierListEntry entry = new TierListEntry();
                entry.setTier(e.tier());
                entry.setPosition(e.position() != null ? e.position() : 0);
                entry.setCatalogGame(game);
                entry.setTierList(t);
                t.getEntries().add(entry);
            }
        }
        t.setUpdatedAt(System.currentTimeMillis());
        return TierListResponse.from(tierListRepo.save(t));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        tierListRepo.delete(findOwned(principal, id));
    }

    private TierList find(Long id) {
        return tierListRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tier list não encontrada"));
    }

    private TierList findOwned(UserPrincipal principal, Long id) {
        TierList t = find(id);
        if (!t.getOwner().getId().equals(principal.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta tier list não é tua");
        return t;
    }

    public record EntryRequest(Long gameId, String tier, Integer position) {}
    public record UpdateRequest(String title, List<EntryRequest> entries) {}
}
