package com.example.authswitch.repo;

import com.example.authswitch.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findByPanHash(String panHash);
}
