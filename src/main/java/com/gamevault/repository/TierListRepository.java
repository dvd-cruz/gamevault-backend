package com.gamevault.repository;

import com.gamevault.model.TierList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TierListRepository extends JpaRepository<TierList, Long> {
    List<TierList> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<TierList> findByOwnerUsernameOrderByCreatedAtDesc(String username);
}
