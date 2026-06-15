package com.gamevault.repository;

import com.gamevault.model.GameCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameCatalogRepository extends JpaRepository<GameCatalog, Long> {

    boolean existsByTitleIgnoreCase(String title);

    Optional<GameCatalog> findByTitleIgnoreCase(String title);

    Optional<GameCatalog> findBySteamAppId(Long steamAppId);

    /** Catalog games linked to a store, so their current price can be checked. */
    @Query("select c from GameCatalog c where c.steamAppId is not null or c.psnProductId is not null")
    List<GameCatalog> findWithStoreId();

    @Query(value = """
            SELECT * FROM game_catalog
            WHERE (CAST(:platform AS varchar) IS NULL
                   OR LOWER(', ' || COALESCE(platform, '') || ', ') LIKE LOWER('%, ' || :platform || ', %'))
              AND (CAST(:genre    AS varchar) IS NULL OR genre    = :genre)
              AND (
                    CAST(:search AS varchar) IS NULL
                    OR LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(genre, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
            ORDER BY title ASC
            """, nativeQuery = true)
    List<GameCatalog> findFiltered(
            @Param("platform") String platform,
            @Param("genre")    String genre,
            @Param("search")   String search
    );
}
