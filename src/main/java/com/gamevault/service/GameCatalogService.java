package com.gamevault.service;

import com.gamevault.dto.CatalogDlcRequest;
import com.gamevault.dto.CatalogGameRequest;
import com.gamevault.dto.CatalogGameResponse;
import com.gamevault.dto.TrophyRequest;
import com.gamevault.model.CatalogDlc;
import com.gamevault.model.Franchise;
import com.gamevault.model.GameCatalog;
import com.gamevault.model.Trophy;
import com.gamevault.model.User;
import com.gamevault.repository.FranchiseRepository;
import com.gamevault.repository.GameCatalogRepository;
import com.gamevault.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GameCatalogService {

    private final GameCatalogRepository catalogRepo;
    private final UserRepository userRepo;
    private final SteamService steamService;
    private final PsnPriceService psnPriceService;
    private final com.gamevault.repository.GameRepository gameRepo;
    private final FranchiseRepository franchiseRepo;

    public GameCatalogService(GameCatalogRepository catalogRepo, UserRepository userRepo, SteamService steamService,
                              PsnPriceService psnPriceService, com.gamevault.repository.GameRepository gameRepo,
                              FranchiseRepository franchiseRepo) {
        this.catalogRepo = catalogRepo;
        this.userRepo = userRepo;
        this.steamService = steamService;
        this.psnPriceService = psnPriceService;
        this.gameRepo = gameRepo;
        this.franchiseRepo = franchiseRepo;
    }

    public List<CatalogGameResponse> browse(String genre, String platform, String search) {
        // group every library entry by catalog game so community stats (rating, players) come along in the list
        var entriesByCatalog = gameRepo.findAll().stream()
                .filter(g -> g.getCatalogGame() != null)
                .collect(java.util.stream.Collectors.groupingBy(g -> g.getCatalogGame().getId()));
        return catalogRepo.findFiltered(blankToNull(platform), blankToNull(genre), blankToNull(search))
                .stream().map(c -> CatalogGameResponse.from(c, entriesByCatalog.get(c.getId()))).toList();
    }

    public CatalogGameResponse getById(Long id) {
        GameCatalog catalogGame = findCatalogGame(id);
        return CatalogGameResponse.from(catalogGame, gameRepo.findByCatalogGameId(id));
    }

    /** Looks up the current Steam Store price for this catalog game's linked Steam App ID, if any. */
    public com.gamevault.dto.SteamPriceResponse getSteamPrice(Long catalogGameId) {
        GameCatalog catalogGame = findCatalogGame(catalogGameId);
        Long appId = catalogGame.getSteamAppId();
        if (appId == null) return com.gamevault.dto.SteamPriceResponse.unavailable();
        try {
            return com.gamevault.dto.SteamPriceResponse.from(steamService.fetchPriceOverview(appId), appId);
        } catch (Exception e) {
            return com.gamevault.dto.SteamPriceResponse.unavailable();
        }
    }

    /** Looks up the current PS Store price for this catalog game's linked PSN Product ID, if any. */
    public com.gamevault.dto.PsnPriceResponse getPsnPrice(Long catalogGameId) {
        GameCatalog catalogGame = findCatalogGame(catalogGameId);
        String productId = catalogGame.getPsnProductId();
        if (productId == null || productId.isBlank()) return com.gamevault.dto.PsnPriceResponse.unavailable();
        try {
            String conceptId = psnPriceService.resolveConceptId(productId);
            if (conceptId == null) return com.gamevault.dto.PsnPriceResponse.unavailable();
            return com.gamevault.dto.PsnPriceResponse.from(psnPriceService.fetchPriceByConceptId(conceptId), conceptId);
        } catch (Exception e) {
            return com.gamevault.dto.PsnPriceResponse.unavailable();
        }
    }

    /** Looks up the DLCs published on the Steam Store for this catalog game's linked Steam App ID, if any. */
    public List<com.gamevault.dto.SteamDlcResponse> getSteamDlcs(Long catalogGameId) {
        GameCatalog catalogGame = findCatalogGame(catalogGameId);
        Long appId = catalogGame.getSteamAppId();
        if (appId == null) return List.of();
        try {
            return steamService.fetchDlcs(appId).stream().map(com.gamevault.dto.SteamDlcResponse::from).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    @Transactional
    public CatalogGameResponse register(Long userId, CatalogGameRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
        if (!user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores podem registar jogos no catálogo");
        }
        if (catalogRepo.existsByTitleIgnoreCase(req.title())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um jogo com este título no catálogo");
        }
        GameCatalog catalogGame = new GameCatalog();
        applyFields(catalogGame, req);
        if (catalogGame.getSteamAppId() != null) {
            importTrophiesFromSteamSchema(catalogGame);
        }
        return CatalogGameResponse.from(catalogRepo.save(catalogGame));
    }

    @Transactional
    public CatalogGameResponse update(Long userId, Long catalogGameId, CatalogGameRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
        if (!user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores podem editar jogos no catálogo");
        }
        GameCatalog catalogGame = findCatalogGame(catalogGameId);
        if (!catalogGame.getTitle().equalsIgnoreCase(req.title()) && catalogRepo.existsByTitleIgnoreCase(req.title())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um jogo com este título no catálogo");
        }
        Long previousSteamAppId = catalogGame.getSteamAppId();
        applyFields(catalogGame, req);
        if (catalogGame.getSteamAppId() != null && !catalogGame.getSteamAppId().equals(previousSteamAppId)) {
            importTrophiesFromSteamSchema(catalogGame);
        }
        return CatalogGameResponse.from(catalogRepo.save(catalogGame));
    }

    /**
     * Removes a game from the catalog (admin only). Trophies and DLCs cascade; franchise links are cleared.
     * Refuses to delete if the game is still referenced by any library entry, activity, event or tier list,
     * so we never orphan users' data — the admin must remove those first.
     */
    @Transactional
    public void delete(Long userId, Long catalogGameId) {
        requireAdmin(userId, "Apenas administradores podem apagar jogos do catálogo");
        GameCatalog catalogGame = findCatalogGame(catalogGameId);

        if (!gameRepo.findByCatalogGameId(catalogGameId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível apagar: este jogo está na biblioteca de um ou mais utilizadores.");
        }

        catalogGame.getFranchises().clear(); // remove os vínculos M2M (mantém as franchises)
        try {
            catalogRepo.delete(catalogGame); // cascateia troféus + DLCs
            catalogRepo.flush();             // força já as verificações de FK para as podermos converter
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível apagar: este jogo está a ser usado (atividade, eventos ou listas).");
        }
    }

    /**
     * Bulk-imports a Steam user's owned games into the CATALOG ONLY (admin) — does not touch anyone's
     * library. Find-or-create by Steam App ID (falls back to title), backfilling the App ID on existing
     * entries and pulling cover/hero art from the Steam CDN. Returns {added, skipped, total}.
     */
    @Transactional
    public java.util.Map<String, Object> importSteamToCatalog(Long userId, String steamIdInput) {
        requireAdmin(userId, "Apenas administradores podem importar para o catálogo");
        String steamId = steamService.resolveSteamId(steamIdInput);
        var owned = steamService.fetchOwnedGames(steamId);
        if (owned.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nenhum jogo encontrado. O perfil Steam tem de ser público (Detalhes do jogo).");
        }
        int added = 0, skipped = 0;
        for (var g : owned) {
            GameCatalog existing = catalogRepo.findBySteamAppId(g.appId())
                    .orElseGet(() -> catalogRepo.findByTitleIgnoreCase(g.name()).orElse(null));
            if (existing != null) {
                if (existing.getSteamAppId() == null) {
                    existing.setSteamAppId(g.appId());
                    catalogRepo.save(existing);
                }
                skipped++;
                continue;
            }
            GameCatalog catalog = new GameCatalog();
            catalog.setTitle(g.name());
            catalog.setSteamAppId(g.appId());
            catalog.setPlatform("PC");
            catalog.setCoverUrl("https://cdn.cloudflare.steamstatic.com/steam/apps/" + g.appId() + "/library_600x900_2x.jpg");
            catalog.setHeroImageUrl("https://cdn.cloudflare.steamstatic.com/steam/apps/" + g.appId() + "/library_hero.jpg");
            catalogRepo.save(catalog);
            added++;
        }
        return java.util.Map.of("added", added, "skipped", skipped, "total", owned.size());
    }

    /**
     * Auto-registers the catalog's Conquistas list from Steam's achievement schema for the
     * configured Steam App ID, so the trophy list exists globally as soon as an admin links
     * the game to Steam — individual users can later sync their personal unlock status, or
     * mark Conquistas as completed manually. Best-effort: failures don't block saving the catalog entry.
     */
    private void importTrophiesFromSteamSchema(GameCatalog catalogGame) {
        if (!steamService.isConfigured()) return;
        try {
            List<SteamService.SteamAchievementDef> schema = steamService.fetchAchievementSchema(catalogGame.getSteamAppId());
            java.util.Map<String, Double> globalPercentages = steamService.fetchGlobalAchievementPercentages(catalogGame.getSteamAppId());
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
                } else if (percent != null && existing.getEarnedRate() == null) {
                    existing.setEarnedRate(percent);
                    existing.setRarity(SteamService.rarityTierFromPercent(percent));
                }
            }
        } catch (Exception ignored) {
            // Steam unreachable / not configured / app has no stats — admin can still add Conquistas manually
        }
    }

    private void applyFields(GameCatalog catalogGame, CatalogGameRequest req) {
        catalogGame.setTitle(req.title());
        catalogGame.setEmoji(req.emoji() != null ? req.emoji() : "🎮");
        catalogGame.setCoverUrl(req.coverUrl());
        catalogGame.setHeroImageUrl(req.heroImageUrl());
        catalogGame.setHeroImagePositionY(req.heroImagePositionY());
        catalogGame.setIconUrl(req.iconUrl());
        catalogGame.setLogoUrl(req.logoUrl());
        catalogGame.setPlatform(com.gamevault.util.PlatformUtil.normalize(req.platform()));
        catalogGame.setGenre(req.genre());
        catalogGame.setReleaseDate(req.releaseDate());
        catalogGame.setDeveloper(req.developer());
        catalogGame.setPublisher(req.publisher());
        catalogGame.setDescription(req.description());
        catalogGame.setDifficulties(req.difficulties());
        catalogGame.setHasDlc(req.hasDlc());
        catalogGame.setSteamAppId(req.steamAppId());
        catalogGame.setPsnNpCommunicationId(req.psnNpCommunicationId());
        catalogGame.setPsnProductId(req.psnProductId());
        catalogGame.setFranchises(resolveFranchises(req.franchiseNames()));
    }

    /** Finds existing franchises by name (case-insensitive) or creates new ones for names that don't exist yet. */
    private List<Franchise> resolveFranchises(List<String> names) {
        if (names == null) return new java.util.ArrayList<>();
        return names.stream()
                .map(String::trim)
                .filter(n -> !n.isBlank())
                .distinct()
                .map(name -> franchiseRepo.findByNameIgnoreCase(name).orElseGet(() -> {
                    Franchise f = new Franchise();
                    f.setName(name);
                    return franchiseRepo.save(f);
                }))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
    }

    @Transactional
    public CatalogGameResponse importPsnTrophies(Long catalogGameId, String npsso, PsnService psnService) {
        GameCatalog catalogGame = findCatalogGame(catalogGameId);
        if (catalogGame.getPsnNpCommunicationId() == null || catalogGame.getPsnNpCommunicationId().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este jogo não tem um PSN NP Communication ID definido no catálogo");

        String accessToken = psnService.exchangeNpssoForAccessToken(npsso);
        java.util.List<PsnService.PsnTrophy> definitions = psnService.fetchTrophyDefinitions(accessToken, catalogGame.getPsnNpCommunicationId());

        if (definitions.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "A API PSN não devolveu troféus para o ID: " + catalogGame.getPsnNpCommunicationId());

        int added = 0;
        for (PsnService.PsnTrophy def : definitions) {
            if (def.trophyName() == null) continue;
            boolean exists = catalogGame.getTrophies().stream()
                    .anyMatch(t -> def.trophyName().equalsIgnoreCase(t.getName()));
            if (!exists) {
                Trophy trophy = new Trophy();
                trophy.setName(def.trophyName());
                trophy.setDescription(def.trophyDetail());
                trophy.setIconUrl(def.trophyIconUrl());
                trophy.setTrophyType(def.trophyType());
                if (def.trophyEarnedRate() != null) {
                    try { trophy.setEarnedRate(Double.parseDouble(def.trophyEarnedRate())); } catch (NumberFormatException ignored) {}
                }
                trophy.setRarity(def.trophyRare());
                trophy.setCatalogGame(catalogGame);
                catalogGame.getTrophies().add(trophy);
                added++;
            } else {
                // Update rarity even if trophy already exists
                catalogGame.getTrophies().stream()
                        .filter(t -> def.trophyName().equalsIgnoreCase(t.getName()))
                        .findFirst().ifPresent(t -> {
                            if (def.trophyEarnedRate() != null && t.getEarnedRate() == null) {
                                try { t.setEarnedRate(Double.parseDouble(def.trophyEarnedRate())); } catch (NumberFormatException ignored) {}
                            }
                            if (def.trophyRare() != null && t.getRarity() == null) {
                                t.setRarity(def.trophyRare());
                            }
                            if (def.trophyType() != null && t.getTrophyType() == null) {
                                t.setTrophyType(def.trophyType());
                            }
                            if (def.trophyIconUrl() != null && t.getIconUrl() == null) {
                                t.setIconUrl(def.trophyIconUrl());
                            }
                        });
            }
        }
        return CatalogGameResponse.from(catalogRepo.save(catalogGame));
    }

    @Transactional
    public CatalogGameResponse addTrophy(Long userId, Long catalogGameId, TrophyRequest req) {
        User user = requireAdmin(userId, "Apenas administradores podem gerir conquistas");
        GameCatalog catalogGame = findCatalogGame(catalogGameId);
        Trophy trophy = new Trophy();
        trophy.setName(req.name());
        trophy.setDescription(blankToNull(req.description()));
        trophy.setIconUrl(blankToNull(req.iconUrl()));
        trophy.setCatalogGame(catalogGame);
        catalogGame.getTrophies().add(trophy);
        return CatalogGameResponse.from(catalogRepo.save(catalogGame));
    }

    @Transactional
    public CatalogGameResponse deleteTrophy(Long userId, Long catalogGameId, Long trophyId) {
        requireAdmin(userId, "Apenas administradores podem gerir conquistas");
        GameCatalog catalogGame = findCatalogGame(catalogGameId);
        boolean removed = catalogGame.getTrophies().removeIf(t -> t.getId().equals(trophyId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conquista não encontrada");
        }
        return CatalogGameResponse.from(catalogRepo.save(catalogGame));
    }

    /** Combines Steam-derived DLCs (read-only) with manually-registered DLCs into one unified list for display. */
    public List<com.gamevault.dto.CatalogDlcResponse> getAllDlcs(Long catalogGameId) {
        List<com.gamevault.dto.CatalogDlcResponse> result = new java.util.ArrayList<>();
        for (com.gamevault.dto.SteamDlcResponse s : getSteamDlcs(catalogGameId)) {
            Double price = s.free() ? 0.0 : (s.finalPrice() != null ? s.finalPrice() : s.initial());
            result.add(new com.gamevault.dto.CatalogDlcResponse(s.appId(), s.name(), price, s.storeUrl(), s.headerImage(), false));
        }
        result.addAll(getManualDlcs(catalogGameId));
        return result;
    }

    @Transactional
    public List<com.gamevault.dto.CatalogDlcResponse> addManualDlc(Long userId, Long catalogGameId, CatalogDlcRequest req) {
        requireAdmin(userId, "Apenas administradores podem gerir DLCs");
        GameCatalog catalogGame = findCatalogGame(catalogGameId);
        CatalogDlc dlc = new CatalogDlc();
        dlc.setName(req.name());
        dlc.setPrice(req.price());
        dlc.setStoreUrl(blankToNull(req.storeUrl()));
        dlc.setCoverUrl(blankToNull(req.coverUrl()));
        dlc.setCatalogGame(catalogGame);
        catalogGame.getDlcs().add(dlc);
        catalogRepo.save(catalogGame);
        return catalogGame.getDlcs().stream().map(com.gamevault.dto.CatalogDlcResponse::from).toList();
    }

    @Transactional
    public List<com.gamevault.dto.CatalogDlcResponse> updateManualDlc(Long userId, Long catalogGameId, Long dlcId, CatalogDlcRequest req) {
        requireAdmin(userId, "Apenas administradores podem gerir DLCs");
        GameCatalog catalogGame = findCatalogGame(catalogGameId);
        CatalogDlc dlc = catalogGame.getDlcs().stream()
                .filter(d -> d.getId().equals(dlcId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DLC não encontrada"));
        dlc.setName(req.name());
        dlc.setPrice(req.price());
        dlc.setStoreUrl(blankToNull(req.storeUrl()));
        dlc.setCoverUrl(blankToNull(req.coverUrl()));
        catalogRepo.save(catalogGame);
        return catalogGame.getDlcs().stream().map(com.gamevault.dto.CatalogDlcResponse::from).toList();
    }

    @Transactional
    public List<com.gamevault.dto.CatalogDlcResponse> deleteManualDlc(Long userId, Long catalogGameId, Long dlcId) {
        requireAdmin(userId, "Apenas administradores podem gerir DLCs");
        GameCatalog catalogGame = findCatalogGame(catalogGameId);
        boolean removed = catalogGame.getDlcs().removeIf(d -> d.getId().equals(dlcId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DLC não encontrada");
        }
        catalogRepo.save(catalogGame);
        return catalogGame.getDlcs().stream().map(com.gamevault.dto.CatalogDlcResponse::from).toList();
    }

    public List<com.gamevault.dto.CatalogDlcResponse> getManualDlcs(Long catalogGameId) {
        GameCatalog catalogGame = findCatalogGame(catalogGameId);
        return catalogGame.getDlcs().stream().map(com.gamevault.dto.CatalogDlcResponse::from).toList();
    }

    private User requireAdmin(Long userId, String message) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
        if (!user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
        return user;
    }

    private GameCatalog findCatalogGame(Long id) {
        return catalogRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jogo não encontrado no catálogo"));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
