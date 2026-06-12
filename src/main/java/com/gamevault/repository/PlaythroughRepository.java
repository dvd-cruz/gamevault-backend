package com.gamevault.repository;

import com.gamevault.model.Playthrough;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaythroughRepository extends JpaRepository<Playthrough, Long> {
}
