package com.gamevault.service;

import com.gamevault.model.GameCatalog;
import com.gamevault.model.Trophy;
import com.gamevault.repository.FranchiseRepository;
import com.gamevault.repository.GameCatalogRepository;
import com.gamevault.repository.GameRepository;
import com.gamevault.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Reproduces the reported bug: a game registered with a Steam App ID gets its trophies
 * (with rarity but no trophyType) imported from Steam's achievement schema. Linking a
 * PSN NP Communication ID and syncing PSN trophies should then backfill trophyType
 * (platinum/gold/silver/bronze) onto those existing trophies.
 */
class GameCatalogServiceTrophyTypeTest {

    @Test
    void psnSyncBackfillsTrophyTypeOnSteamImportedTrophy() {
        GameCatalogRepository catalogRepo = mock(GameCatalogRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        SteamService steamService = mock(SteamService.class);
        PsnPriceService psnPriceService = mock(PsnPriceService.class);
        GameRepository gameRepo = mock(GameRepository.class);
        PsnService psnService = mock(PsnService.class);
        FranchiseRepository franchiseRepo = mock(FranchiseRepository.class);

        GameCatalogService service = new GameCatalogService(catalogRepo, userRepo, steamService, psnPriceService, gameRepo, franchiseRepo);

        // 1. "God of War (2018)" already in catalog with a Steam-imported trophy
        //    (trophyType is null, rarity was set from Steam's global unlock %).
        GameCatalog gow = new GameCatalog();
        gow.setTitle("God of War (2018)");
        gow.setPsnNpCommunicationId("NPWR14282_00");

        Trophy steamTrophy = new Trophy();
        steamTrophy.setName("Vingança Cumprida");
        steamTrophy.setRarity(0); // Ultra Rare, derived from Steam global %
        steamTrophy.setTrophyType(null); // not set by Steam import
        steamTrophy.setCatalogGame(gow);
        gow.getTrophies().add(steamTrophy);

        when(catalogRepo.findById(1L)).thenReturn(java.util.Optional.of(gow));
        when(catalogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 2. PSN sync returns a definition for the same trophy (matched by name),
        //    now including trophyType "platinum" and an icon URL.
        PsnService.PsnTrophy def = new PsnService.PsnTrophy(
                1, "Vingança Cumprida", "Termina o jogo", "https://psn.example/icon.png",
                "platinum", false, "1.2", 0, null, null);

        when(psnService.exchangeNpssoForAccessToken("fake-npsso")).thenReturn("access-token");
        when(psnService.fetchTrophyDefinitions("access-token", "NPWR14282_00")).thenReturn(List.of(def));

        // Act
        service.importPsnTrophies(1L, "fake-npsso", psnService);

        // Assert: the existing (Steam-imported) trophy now has trophyType set,
        // so the frontend can render the platinum/gold/silver/bronze emoji.
        assertThat(gow.getTrophies()).hasSize(1);
        Trophy updated = gow.getTrophies().get(0);
        assertThat(updated.getTrophyType()).isEqualTo("platinum");
        assertThat(updated.getRarity()).isEqualTo(0); // untouched, already set
        assertThat(updated.getIconUrl()).isEqualTo("https://psn.example/icon.png");
    }
}
