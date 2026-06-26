package com.example.authswitch.repo;

import com.example.authswitch.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByStanAndRrn(String stan, String rrn);
}
