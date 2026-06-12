package com.gamevault.repository;

import com.gamevault.model.Franchise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FranchiseRepository extends JpaRepository<Franchise, Long> {

    Optional<Franchise> findByNameIgnoreCase(String name);
}
