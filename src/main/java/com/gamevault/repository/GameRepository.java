package com.gamevault.repository;

import com.gamevault.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByOwnerIdOrderByAddedAtDesc(Long ownerId);

    @Query(value = """
            SELECT g.* FROM games g
            JOIN game_catalog c ON c.id = g.catalog_game_id
            WHERE g.user_id = :ownerId
              AND (CAST(:status   AS varchar) IS NULL OR g.status  = :status)
              AND (CAST(:platform AS varchar) IS NULL
                   OR LOWER(', ' || COALESCE(c.platform, '') || ', ') LIKE LOWER('%, ' || :platform || ', %'))
              AND (CAST(:genre    AS varchar) IS NULL OR c.genre    = :genre)
              AND (
                    CAST(:search AS varchar) IS NULL
                    OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(c.genre, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
            ORDER BY g.added_at DESC
            """, nativeQuery = true)
    List<Game> findFiltered(
            @Param("ownerId")  Long ownerId,
            @Param("status")   String status,
            @Param("platform") String platform,
            @Param("genre")    String genre,
            @Param("search")   String search
    );

    Optional<Game> findByOwnerIdAndId(Long ownerId, Long gameId);

    Optional<Game> findByOwnerIdAndCatalogGameId(Long ownerId, Long catalogGameId);

    boolean existsByOwnerIdAndId(Long ownerId, Long gameId);

    boolean existsByOwnerIdAndCatalogGameId(Long ownerId, Long catalogGameId);

    /** All library entries (across every user) for a given catalog game — used to compute community-wide stats. */
    List<Game> findByCatalogGameId(Long catalogGameId);

    /** Batch average rating per catalog game — returns [catalogGameId, avgRating] rows. */
    @Query("SELECT g.catalogGame.id, AVG(g.rating) FROM Game g WHERE g.catalogGame.id IN :ids AND g.rating IS NOT NULL AND g.rating > 0 GROUP BY g.catalogGame.id")
    List<Object[]> avgRatingByCatalogIds(@Param("ids") List<Long> ids);
}
