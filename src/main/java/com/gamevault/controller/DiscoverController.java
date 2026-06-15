package com.gamevault.controller;

import com.gamevault.dto.FreeGameResponse;
import com.gamevault.service.EpicFreeGamesService;
import com.gamevault.service.SteamFreeGamesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/discover")
public class DiscoverController {

    private final EpicFreeGamesService epicFreeGames;
    private final SteamFreeGamesService steamFreeGames;

    public DiscoverController(EpicFreeGamesService epicFreeGames, SteamFreeGamesService steamFreeGames) {
        this.epicFreeGames = epicFreeGames;
        this.steamFreeGames = steamFreeGames;
    }

    /** Games temporarily free-to-keep right now, across Epic and Steam. */
    @GetMapping("/free-games")
    public List<FreeGameResponse> freeGames() {
        List<FreeGameResponse> all = new ArrayList<>(epicFreeGames.getCurrentlyFree());
        all.addAll(steamFreeGames.getCurrentlyFree());
        return all;
    }
}
