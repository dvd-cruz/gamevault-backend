package com.gamevault.controller;

import com.gamevault.dto.FranchiseRequest;
import com.gamevault.dto.FranchiseResponse;
import com.gamevault.model.Franchise;
import com.gamevault.model.User;
import com.gamevault.repository.FranchiseRepository;
import com.gamevault.repository.UserRepository;
import com.gamevault.security.UserPrincipal;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/franchises")
public class FranchiseController {

    private final FranchiseRepository franchiseRepo;
    private final UserRepository userRepo;
    private final com.gamevault.service.FileStorageService fileStorage;

    public FranchiseController(FranchiseRepository franchiseRepo, UserRepository userRepo,
                               com.gamevault.service.FileStorageService fileStorage) {
        this.franchiseRepo = franchiseRepo;
        this.userRepo = userRepo;
        this.fileStorage = fileStorage;
    }

    @GetMapping
    public List<FranchiseResponse> all(@AuthenticationPrincipal UserPrincipal principal) {
        User user = currentUser(principal);
        return franchiseRepo.findAll().stream()
                .map(f -> FranchiseResponse.summary(f, isFavorite(user, f)))
                .toList();
    }

    @GetMapping("/{id}")
    public FranchiseResponse get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        User user = currentUser(principal);
        Franchise franchise = franchiseRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Franquia não encontrada"));
        return FranchiseResponse.from(franchise, isFavorite(user, franchise));
    }

    @PostMapping("/{id}/favorite")
    @Transactional
    public FranchiseResponse toggleFavorite(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        User user = userRepo.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Franchise franchise = franchiseRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Franquia não encontrada"));

        if (isFavorite(user, franchise)) {
            user.getFavoriteFranchises().removeIf(f -> f.getId().equals(franchise.getId()));
        } else {
            user.getFavoriteFranchises().add(franchise);
        }
        userRepo.save(user);
        return FranchiseResponse.from(franchise, isFavorite(user, franchise));
    }

    @PutMapping("/{id}")
    @Transactional
    public FranchiseResponse update(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id,
                                     @RequestBody FranchiseRequest req) {
        User user = userRepo.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores podem editar franquias");
        }
        Franchise franchise = franchiseRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Franquia não encontrada"));

        if (req.name() != null && !req.name().isBlank()) {
            franchise.setName(req.name().trim());
        }
        if (req.iconUrl() != null && !req.iconUrl().equals(franchise.getIconUrl())) {
            fileStorage.deleteIfUpload(franchise.getIconUrl());
        }
        franchise.setIconUrl(req.iconUrl());
        franchiseRepo.save(franchise);
        return FranchiseResponse.from(franchise, isFavorite(user, franchise));
    }

    private User currentUser(UserPrincipal principal) {
        return principal != null ? userRepo.findById(principal.getId()).orElse(null) : null;
    }

    private boolean isFavorite(User user, Franchise franchise) {
        return user != null && user.getFavoriteFranchises().stream().anyMatch(f -> f.getId().equals(franchise.getId()));
    }
}
