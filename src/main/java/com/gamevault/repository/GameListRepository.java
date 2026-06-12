package com.gamevault.repository;

import com.gamevault.model.GameList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameListRepository extends JpaRepository<GameList, Long> {
    Optional<GameList> findBySlug(String slug);
    List<GameList> findAllByOrderByCreatedAtDesc();
}
