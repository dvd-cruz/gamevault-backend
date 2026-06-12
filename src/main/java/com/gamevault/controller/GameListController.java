package com.gamevault.controller;

import com.gamevault.dto.GameListResponse;
import com.gamevault.service.GameListService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lists")
public class GameListController {

    private final GameListService listService;

    public GameListController(GameListService listService) {
        this.listService = listService;
    }

    @GetMapping
    public List<GameListResponse> all() {
        return listService.getAll();
    }

    @GetMapping("/{slug}")
    public GameListResponse bySlug(@PathVariable String slug) {
        return listService.getBySlug(slug);
    }
}
