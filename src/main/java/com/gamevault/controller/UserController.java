package com.gamevault.controller;

import com.gamevault.dto.ActivityResponse;
import com.gamevault.dto.ChangePasswordRequest;
import com.gamevault.dto.FranchiseResponse;
import com.gamevault.dto.GameResponse;
import com.gamevault.dto.UpdateProfileRequest;
import com.gamevault.dto.MeResponse;
import com.gamevault.dto.UserResponse;
import com.gamevault.model.User;
import com.gamevault.repository.UserRepository;
import com.gamevault.security.UserPrincipal;
import com.gamevault.service.ActivityService;
import com.gamevault.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final GameService gameService;
    private final ActivityService activityService;
    private final com.gamevault.service.PsnService psnService;
    private final com.gamevault.service.FileStorageService fileStorage;

    public UserController(UserRepository userRepo, PasswordEncoder passwordEncoder, GameService gameService, ActivityService activityService, com.gamevault.service.PsnService psnService, com.gamevault.service.FileStorageService fileStorage) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.gameService = gameService;
        this.activityService = activityService;
        this.psnService = psnService;
        this.fileStorage = fileStorage;
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return userRepo.findById(principal.getId())
                .map(MeResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/me")
    public MeResponse updateMe(@AuthenticationPrincipal UserPrincipal principal,
                                 @Valid @RequestBody UpdateProfileRequest req) {
        User u = userRepo.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        userRepo.findByEmail(req.email())
                .filter(other -> !other.getId().equals(u.getId()))
                .ifPresent(other -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já está em uso"); });

        u.setName(req.name());
        u.setEmail(req.email());
        if (req.avatar() != null && !req.avatar().equals(u.getAvatar())) {
            fileStorage.deleteIfUpload(u.getAvatar());
        }
        u.setAvatar(req.avatar());
        u.setBio(req.bio());
        u.setLocation(req.location());
        u.setSteamId(req.steamId());
        u.setPrivateProfile(req.privateProfile());

        // If a new NPSSO was provided, validate it and resolve the account ID
        String newNpsso = req.npsso();
        if (newNpsso != null && !newNpsso.isBlank() && !newNpsso.equals(u.getNpsso())) {
            try {
                String accessToken = psnService.exchangeNpssoForAccessToken(newNpsso.trim());
                String accountId   = psnService.fetchAccountId(accessToken);
                u.setNpsso(newNpsso.trim());
                u.setPsnAccountId(accountId);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NPSSO inválido: " + e.getMessage());
            }
        } else if (newNpsso != null && newNpsso.isBlank()) {
            u.setNpsso(null);
            u.setPsnAccountId(null);
        }

        return MeResponse.from(userRepo.save(u));
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal UserPrincipal principal,
                               @Valid @RequestBody ChangePasswordRequest req) {
        User u = userRepo.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(req.currentPassword(), u.getPassword())) {
            throw new BadCredentialsException("Password atual incorreta");
        }

        u.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepo.save(u);
    }

    @GetMapping("/{username}")
    public UserResponse byUsername(@PathVariable String username) {
        return userRepo.findByUsername(username)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
    }

    @GetMapping("/{username}/games")
    public List<GameResponse> gamesByUsername(@PathVariable String username) {
        return gameService.getGamesByUsername(username);
    }

    @GetMapping("/{username}/posts")
    public List<ActivityResponse> postsByUsername(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String username) {
        User viewer = principal != null ? userRepo.findById(principal.getId()).orElse(null) : null;
        return activityService.getPostsByUsername(username, viewer);
    }

    @GetMapping("/{username}/favorite-franchises")
    public List<FranchiseResponse> favoriteFranchises(@PathVariable String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
        return user.getFavoriteFranchises().stream()
                .map(f -> FranchiseResponse.summary(f, true))
                .toList();
    }

    @GetMapping("/search")
    public List<UserResponse> search(@RequestParam String q) {
        if (q == null || q.isBlank() || q.length() < 2) return List.of();
        return userRepo.search(q.trim()).stream().map(UserResponse::from).toList();
    }

    @GetMapping
    public List<UserResponse> all() {
        return userRepo.findAll().stream().map(UserResponse::from).toList();
    }
}
