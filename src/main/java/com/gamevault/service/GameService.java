package com.gamevault.service;

import com.gamevault.dto.AddToLibraryRequest;
import com.gamevault.dto.GameRequest;
import com.gamevault.dto.GameResponse;
import com.gamevault.dto.PlaythroughRequest;
import com.gamevault.dto.SessionRequest;
import com.gamevault.model.Game;
import com.gamevault.model.GameCatalog;
import com.gamevault.model.GameSession;
import com.gamevault.model.Playthrough;
import com.gamevault.model.Trophy;
import com.gamevault.model.UnlockedTrophy;
import com.gamevault.model.User;
import com.gamevault.repository.GameCatalogRepository;
import com.gamevault.repository.GameRepository;
import com.gamevault.repository.TrophyRepository;
import com.gamevault.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class GameService {

    private final GameRepository gameRepo;
    private final UserRepository userRepo;
    private final GameCatalogRepository catalogRepo;
    private final TrophyRepository trophyRepo;
    private final ActivityService activityService;
    private final SteamService steamService;
    private final PsnService psnService;

    public GameService(GameRepository gameRepo, UserRepository userRepo, GameCatalogRepository catalogRepo, TrophyRepository trophyRepo, ActivityService activityService, SteamService steamService, PsnService psnService) {
        this.gameRepo = gameRepo;
        this.userRepo = userRepo;
        this.catalogRepo = catalogRepo;
        this.trophyRepo = trophyRepo;
        this.activityService = activityService;
        this.steamService = steamService;
        this.psnService = psnService;
    }

    public List<GameResponse> getGames(Long userId, String status, String platform, String genre, String search) {
        List<Game> games = gameRepo.findFiltered(userId,
                blankToNull(status), blankToNull(platform), blankToNull(genre), blankToNull(search));
        // batch-compute community ratings for all catalog games in the result
        List<Long> catalogIds = games.stream().map(g -> g.getCatalogGame().getId()).distinct().toList();
        java.util.Map<Long, Double> communityRatings = new java.util.HashMap<>();
        if (!catalogIds.isEmpty()) {
            gameRepo.avgRatingByCatalogIds(catalogIds).forEach(row ->
                    communityRatings.put((Long) row[0], (Double) row[1]));
        }
        return games.stream()
                .map(g -> GameResponse.from(g, communityRatings.get(g.getCatalogGame().getId())))
                .toList();
    }

    public List<GameResponse> getGamesByUsername(String username) {
        User owner = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
        return gameRepo.findFiltered(owner.getId(), null, null, null, null)
                .stream().map(GameResponse::from).toList();
    }

    public GameResponse getGame(Long userId, Long gameId) {
        return GameResponse.from(findOwned(userId, gameId));
    }

    @Transactional
    public GameResponse addToLibrary(Long userId, AddToLibraryRequest req) {
        User owner = findUser(userId);
        GameCatalog catalogGame = catalogRepo.findById(req.catalogGameId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Este jogo não existe no catálogo"));
        if (gameRepo.existsByOwnerIdAndCatalogGameId(userId, catalogGame.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já tens este jogo na tua biblioteca");
        }
        Game game = new Game();
        game.setCatalogGame(catalogGame);
        game.setOwner(owner);
        game.setAddedAt(System.currentTimeMillis());
        game.setStatusUpdatedAt(game.getAddedAt());
        applyPersonalFields(game, req.status(), req.hours(), req.rating(), req.notes(),
                req.review(), req.progress(), req.tags());
        GameResponse saved = GameResponse.from(gameRepo.save(game));
        activityService.recordAdded(owner, catalogGame, game.getStatus());
        if (game.getReview() != null && !game.getReview().isBlank()) {
            activityService.recordReview(owner, catalogGame, game.getReview(), game.getRating(), game.getReviewPlatform());
        }
        return saved;
    }

    @Transactional
    public GameResponse updateGame(Long userId, Long gameId, GameRequest req) {
        Game game = findOwned(userId, gameId);
        String oldStatus = game.getStatus();
        String oldReview = game.getReview();
        applyPersonalFields(game, req.status(), req.hours(), req.rating(), req.notes(),
                req.review(), req.progress(), req.tags());
        if (oldStatus != null && !oldStatus.equals(req.status())) {
            game.setStatusUpdatedAt(System.currentTimeMillis());
        }
        GameResponse saved = GameResponse.from(gameRepo.save(game));
        if (oldStatus != null && !oldStatus.equals(req.status())
                && ("completed".equals(req.status()) || "playing".equals(req.status())
                    || "paused".equals(req.status()) || "backlog".equals(req.status())
                    || "wishlist".equals(req.status()) || "dropped".equals(req.status()))) {
            activityService.recordStatusChange(game.getOwner(), game.getCatalogGame(), req.status(), req.hours(), req.rating());
        }
        String newReview = game.getReview();
        if (newReview != null && !newReview.isBlank() && !newReview.equals(oldReview)) {
            activityService.recordReview(game.getOwner(), game.getCatalogGame(), newReview, game.getRating(), game.getReviewPlatform());
        }
        return saved;
    }

    /** Updates only the review text, rating and platform for a game, recording/updating the review Activity. */
    @Transactional
    public GameResponse updateReview(Long userId, Long gameId, Integer rating, String review, String reviewPlatform) {
        Game game = findOwned(userId, gameId);
        if (rating != null) game.setRating(rating);
        game.setReview(review == null || review.isBlank() ? null : review.trim());
        game.setReviewPlatform(blankToNull(reviewPlatform));
        GameResponse saved = GameResponse.from(gameRepo.save(game));
        boolean hasContent = game.getReview() != null || (game.getRating() != null && game.getRating() > 0);
        if (hasContent) {
            activityService.recordReview(game.getOwner(), game.getCatalogGame(), game.getReview(), game.getRating(), game.getReviewPlatform());
        } else {
            activityService.deleteReview(game.getOwner(), game.getCatalogGame());
        }
        return saved;
    }

    @Transactional
    public GameResponse toggleFavorite(Long userId, Long gameId) {
        Game game = findOwned(userId, gameId);
        game.setFavorite(!game.isFavorite());
        return GameResponse.from(gameRepo.save(game));
    }

    @Transactional
    public void deleteGame(Long userId, Long gameId) {
        gameRepo.delete(findOwned(userId, gameId));
    }

    @Transactional
    public GameResponse addSession(Long userId, Long gameId, Long playthroughId, SessionRequest req) {
        Game game = findOwned(userId, gameId);
        Playthrough playthrough = findPlaythrough(game, playthroughId);
        GameSession session = new GameSession();
        session.setDate(req.date());
        session.setHours(req.hours());
        session.setNotes(req.notes());
        session.setPlaythrough(playthrough);
        playthrough.getSessions().add(session);
        return GameResponse.from(gameRepo.save(game));
    }

    @Transactional
    public GameResponse deleteSession(Long userId, Long gameId, Long playthroughId, Long sessionId) {
        Game game = findOwned(userId, gameId);
        Playthrough playthrough = findPlaythrough(game, playthroughId);
        playthrough.getSessions().removeIf(s -> s.getId().equals(sessionId));
        return GameResponse.from(gameRepo.save(game));
    }

    private Playthrough findPlaythrough(Game game, Long playthroughId) {
        return game.getPlaythroughs().stream()
                .filter(p -> p.getId().equals(playthroughId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playthrough não encontrada"));
    }

    @Transactional
    public GameResponse addPlaythrough(Long userId, Long gameId, PlaythroughRequest req) {
        Game game = findOwned(userId, gameId);
        Playthrough playthrough = new Playthrough();
        playthrough.setType(req.type());
        playthrough.setSpeedrun(req.speedrun());
        playthrough.setModded(req.modded());
        playthrough.setCompleted(req.completed());
        playthrough.setHours(req.hours());
        playthrough.setPlatform(blankToNull(req.platform()));
        playthrough.setDifficulty(blankToNull(req.difficulty()));
        playthrough.setLanguage(blankToNull(req.language()));
        playthrough.setNotes(blankToNull(req.notes()));
        playthrough.setGame(game);
        game.getPlaythroughs().add(playthrough);
        return GameResponse.from(gameRepo.save(game));
    }

    @Transactional
    public GameResponse updatePlaythrough(Long userId, Long gameId, Long playthroughId, PlaythroughRequest req) {
        Game game = findOwned(userId, gameId);
        Playthrough playthrough = game.getPlaythroughs().stream()
                .filter(p -> p.getId().equals(playthroughId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playthrough não encontrada"));
        playthrough.setType(req.type());
        playthrough.setSpeedrun(req.speedrun());
        playthrough.setModded(req.modded());
        playthrough.setCompleted(req.completed());
        playthrough.setHours(req.hours());
        playthrough.setPlatform(blankToNull(req.platform()));
        playthrough.setDifficulty(blankToNull(req.difficulty()));
        playthrough.setLanguage(blankToNull(req.language()));
        playthrough.setNotes(blankToNull(req.notes()));
        return GameResponse.from(gameRepo.save(game));
    }

    @Transactional
    public GameResponse deletePlaythrough(Long userId, Long gameId, Long playthroughId) {
        Game game = findOwned(userId, gameId);
        game.getPlaythroughs().removeIf(p -> p.getId().equals(playthroughId));
        return GameResponse.from(gameRepo.save(game));
    }

    @Transactional
    public GameResponse unlockTrophy(Long userId, Long gameId, Long trophyId) {
        Game game = findOwned(userId, gameId);
        Trophy trophy = trophyRepo.findById(trophyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conquista não encontrada"));
        if (!trophy.getCatalogGame().getId().equals(game.getCatalogGame().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta conquista não pertence a este jogo");
        }
        boolean alreadyUnlocked = game.getUnlockedTrophies().stream()
                .anyMatch(ut -> ut.getTrophy().getId().equals(trophyId));
        if (!alreadyUnlocked) {
            UnlockedTrophy unlocked = new UnlockedTrophy();
            unlocked.setTrophy(trophy);
            unlocked.setGame(game);
            long now = System.currentTimeMillis();
            unlocked.setUnlockedAt(now);
            unlocked.setMarkedAt(now);
            unlocked.setSource("manual");
            game.getUnlockedTrophies().add(unlocked);
            GameResponse saved = GameResponse.from(gameRepo.save(game));
            activityService.recordTrophyUnlock(game.getOwner(), game.getCatalogGame(), trophyId);
            return saved;
        }
        return GameResponse.from(gameRepo.save(game));
    }

    @Transactional
    public GameResponse lockTrophy(Long userId, Long gameId, Long trophyId) {
        Game game = findOwned(userId, gameId);
        game.getUnlockedTrophies().removeIf(ut -> ut.getTrophy().getId().equals(trophyId));
        return GameResponse.from(gameRepo.save(game));
    }

    /**
     * Imports achievement definitions and unlock status from Steam for this game, matching them
     * (by display name) against the catalog's existing Conquistas, creating any that are missing,
     * and unlocking the ones the player has earned on Steam.
     */
    @Transactional
    public GameResponse syncSteamAchievements(Long userId, Long gameId) {
        Game game = findOwned(userId, gameId);
        User owner = game.getOwner();
        GameCatalog catalogGame = game.getCatalogGame();

        if (catalogGame.getSteamAppId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este jogo não tem um Steam App ID associado no catálogo");
        }
        if (owner.getSteamId() == null || owner.getSteamId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Liga a tua conta Steam no perfil para sincronizar conquistas");
        }

        Long appId = catalogGame.getSteamAppId();
        List<SteamService.SteamAchievementDef> schema;
        Map<String, Long> unlocked;
        try {
            schema = steamService.fetchAchievementSchema(appId);
            unlocked = steamService.fetchUnlockedAchievements(owner.getSteamId(), appId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível obter dados da Steam: " + e.getMessage());
        }
        if (schema.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Não foram encontradas conquistas para este jogo na Steam");
        }

        // Global unlock percentages, used to derive a PSN-style rarity tier for Steam achievements (best-effort)
        Map<String, Double> globalPercentages = steamService.fetchGlobalAchievementPercentages(appId);

        // Pass 1 — make sure every Steam achievement has a matching Conquista in the catalog (matched by name)
        boolean catalogChanged = false;
        for (var def : schema) {
            if (def.name() == null) continue;
            String displayName = (def.displayName() != null && !def.displayName().isBlank()) ? def.displayName() : def.name();
            Double percent = globalPercentages.get(def.name());
            Trophy existing = catalogGame.getTrophies().stream()
                    .filter(t -> t.getName().equalsIgnoreCase(displayName))
                    .findFirst().orElse(null);
            if (existing == null) {
                Trophy trophy = new Trophy();
                trophy.setName(displayName);
                trophy.setDescription(blankToNull(def.description()));
                trophy.setIconUrl(blankToNull(def.icon()));
                if (percent != null) {
                    trophy.setEarnedRate(percent);
                    trophy.setRarity(SteamService.rarityTierFromPercent(percent));
                }
                trophy.setCatalogGame(catalogGame);
                catalogGame.getTrophies().add(trophy);
                catalogChanged = true;
            } else if (percent != null && existing.getEarnedRate() == null) {
                existing.setEarnedRate(percent);
                existing.setRarity(SteamService.rarityTierFromPercent(percent));
                catalogChanged = true;
            }
        }
        if (catalogChanged) {
            catalogGame = catalogRepo.save(catalogGame);
            game.setCatalogGame(catalogGame);
        }

        // Pass 2 — unlock the Conquistas that Steam reports as achieved for this player
        int newlyUnlocked = 0;
        long syncTime = System.currentTimeMillis();
        for (var def : schema) {
            if (def.name() == null) continue;
            Long unlockTime = unlocked.get(def.name());
            if (unlockTime == null) continue;
            String displayName = (def.displayName() != null && !def.displayName().isBlank()) ? def.displayName() : def.name();
            Trophy trophy = catalogGame.getTrophies().stream()
                    .filter(t -> t.getName().equalsIgnoreCase(displayName))
                    .findFirst().orElse(null);
            if (trophy == null) continue;
            Long trophyId = trophy.getId();
            boolean already = game.getUnlockedTrophies().stream().anyMatch(ut -> ut.getTrophy().getId().equals(trophyId));
            if (!already) {
                UnlockedTrophy ut = new UnlockedTrophy();
                ut.setTrophy(trophy);
                ut.setGame(game);
                ut.setUnlockedAt(unlockTime > 0 ? unlockTime * 1000 : syncTime);
                ut.setMarkedAt(syncTime);
                ut.setSource("steam");
                game.getUnlockedTrophies().add(ut);
                newlyUnlocked++;
            }
        }

        GameResponse saved = GameResponse.from(gameRepo.save(game));
        if (newlyUnlocked > 0) {
            activityService.recordTrophyUnlockBatch(owner, catalogGame, null, newlyUnlocked);
        }
        return saved;
    }

    /**
     * Imports PSN trophy definitions and unlock status for this game.
     * Requires the user to have their NPSSO token configured in their profile.
     * Auto-registers missing trophy definitions in the catalog, then unlocks the earned ones.
     */
    @Transactional
    public GameResponse syncPsnTrophies(Long userId, Long gameId) {
        Game game = findOwned(userId, gameId);
        User owner = game.getOwner();
        GameCatalog catalogGame = game.getCatalogGame();

        if (catalogGame.getPsnNpCommunicationId() == null || catalogGame.getPsnNpCommunicationId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este jogo não tem um NP Communication ID PSN associado no catálogo");
        }
        if (owner.getNpsso() == null || owner.getNpsso().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configura o teu NPSSO nas definições para sincronizar troféus PSN");
        }

        String npCommId = catalogGame.getPsnNpCommunicationId();

        // Authenticate
        String accessToken;
        String accountId;
        try {
            accessToken = psnService.exchangeNpssoForAccessToken(owner.getNpsso());
            accountId   = owner.getPsnAccountId() != null ? owner.getPsnAccountId() : psnService.fetchAccountId(accessToken);
            if (owner.getPsnAccountId() == null) {
                owner.setPsnAccountId(accountId);
                userRepo.save(owner);
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }

        // Fetch definitions and earned trophies from PSN
        List<PsnService.PsnTrophy> definitions;
        List<PsnService.PsnTrophy> earned;
        try {
            definitions = psnService.fetchTrophyDefinitions(accessToken, npCommId);
            earned      = psnService.fetchEarnedTrophies(accessToken, accountId, npCommId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Erro ao obter troféus PSN: " + e.getMessage());
        }
        if (definitions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Não foram encontrados troféus PSN para este jogo");
        }

        // Build maps from earned response: trophyId → earnedTs, trophyId → rarity data
        // (rarity fields come from the earned endpoint, not the definitions endpoint)
        java.util.Map<Integer, Long>    earnedMap   = new java.util.HashMap<>();
        java.util.Map<Integer, Integer> rarityMap   = new java.util.HashMap<>();
        java.util.Map<Integer, Double>  rateMap     = new java.util.HashMap<>();
        for (var e : earned) {
            if (e.trophyId() == null) continue;
            if (Boolean.TRUE.equals(e.earned())) {
                earnedMap.put(e.trophyId(), parseIsoToMillis(e.earnedDateTime()));
            }
            if (e.trophyRare() != null)        rarityMap.put(e.trophyId(), e.trophyRare());
            if (e.trophyEarnedRate() != null) {
                try { rateMap.put(e.trophyId(), Double.parseDouble(e.trophyEarnedRate())); } catch (NumberFormatException ignored) {}
            }
        }
        // Pass 1 — register missing trophy definitions in the catalog
        boolean catalogChanged = false;
        for (var def : definitions) {
            if (def.trophyId() == null || def.trophyName() == null) continue;
            String psnId = "psn:" + def.trophyId();
            Trophy existing = catalogGame.getTrophies().stream()
                    .filter(t -> psnId.equals(t.getDescription()) || def.trophyName().equalsIgnoreCase(t.getName()))
                    .findFirst().orElse(null);
            if (existing == null) {
                Trophy trophy = new Trophy();
                trophy.setName(def.trophyName());
                trophy.setDescription(def.trophyDetail());
                trophy.setIconUrl(def.trophyIconUrl());
                trophy.setTrophyType(def.trophyType());
                // Rarity from earned response (keyed by trophyId)
                if (rarityMap.containsKey(def.trophyId())) trophy.setRarity(rarityMap.get(def.trophyId()));
                if (rateMap.containsKey(def.trophyId()))   trophy.setEarnedRate(rateMap.get(def.trophyId()));
                trophy.setCatalogGame(catalogGame);
                catalogGame.getTrophies().add(trophy);
                catalogChanged = true;
            } else {
                // Update rarity on existing trophy from earned response
                if (rarityMap.containsKey(def.trophyId())) { existing.setRarity(rarityMap.get(def.trophyId())); catalogChanged = true; }
                if (rateMap.containsKey(def.trophyId()))   { existing.setEarnedRate(rateMap.get(def.trophyId())); catalogChanged = true; }
            }
        }
        if (catalogChanged) {
            catalogGame = catalogRepo.save(catalogGame);
            game.setCatalogGame(catalogGame);
        }

        // Pass 2 — unlock the earned ones
        int newlyUnlocked = 0;
        long syncTime = System.currentTimeMillis();
        for (var def : definitions) {
            if (def.trophyId() == null) continue;
            Long earnedAt = earnedMap.get(def.trophyId());
            if (earnedAt == null) continue;

            Trophy trophy = catalogGame.getTrophies().stream()
                    .filter(t -> def.trophyName() != null && def.trophyName().equalsIgnoreCase(t.getName()))
                    .findFirst().orElse(null);
            if (trophy == null) continue;

            boolean alreadyUnlocked = game.getUnlockedTrophies().stream()
                    .anyMatch(ut -> ut.getTrophy().getId().equals(trophy.getId()));
            if (alreadyUnlocked) continue;

            com.gamevault.model.UnlockedTrophy ut = new com.gamevault.model.UnlockedTrophy();
            ut.setTrophy(trophy);
            ut.setGame(game);
            ut.setUnlockedAt(earnedAt);
            ut.setMarkedAt(syncTime);
            ut.setSource("psn");
            game.getUnlockedTrophies().add(ut);
            newlyUnlocked++;
        }

        GameResponse result = GameResponse.from(gameRepo.save(game));
        if (newlyUnlocked > 0) {
            activityService.recordTrophyUnlockBatch(owner, catalogGame, null, newlyUnlocked);
        }
        return result;
    }

    private long parseIsoToMillis(String iso) {
        if (iso == null || iso.isBlank()) return System.currentTimeMillis();
        try {
            return java.time.Instant.parse(iso).toEpochMilli();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Game findOwned(Long userId, Long gameId) {
        return gameRepo.findByOwnerIdAndId(userId, gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jogo não encontrado"));
    }

    private User findUser(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
    }

    private void applyPersonalFields(Game game, String status, Double hours, Integer rating, String notes,
                                      String review, Integer progress, List<String> tags) {
        game.setStatus(status);
        game.setHours(hours != null ? hours : 0.0);
        game.setRating(rating != null ? rating : 0);
        game.setNotes(notes);
        game.setReview(blankToNull(review));
        game.setProgress(progress != null ? progress : 0);
        if (tags != null) {
            game.getTags().clear();
            game.getTags().addAll(tags);
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
