package com.gamevault.repository;

import com.gamevault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY u.username")
    List<User> search(@Param("q") String q);
}
