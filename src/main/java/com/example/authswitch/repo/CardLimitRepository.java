package com.example.authswitch.repo;

import com.example.authswitch.domain.CardLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardLimitRepository extends JpaRepository<CardLimit, Long> {
    Optional<CardLimit> findByCardId(Long cardId);
}
