package com.gamevault.service;

import com.gamevault.dto.GameListResponse;
import com.gamevault.repository.GameListRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GameListService {

    private final GameListRepository listRepo;

    public GameListService(GameListRepository listRepo) {
        this.listRepo = listRepo;
    }

    public List<GameListResponse> getAll() {
        return listRepo.findAllByOrderByCreatedAtDesc()
                .stream().map(GameListResponse::from).toList();
    }

    public GameListResponse getBySlug(String slug) {
        return listRepo.findBySlug(slug)
                .map(GameListResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lista não encontrada"));
    }
}
