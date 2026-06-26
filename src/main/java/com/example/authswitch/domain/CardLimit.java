package com.example.authswitch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Spending limits for a card plus a rolling daily-spend counter (velocity check). */
@Entity
@Table(name = "card_limit")
public class CardLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false, unique = true)
    private Long cardId;

    /** Max amount for a single transaction (minor units). */
    @Column(name = "per_txn_limit", nullable = false)
    private long perTxnLimit;

    /** Max total spend per day (minor units). */
    @Column(name = "daily_limit", nullable = false)
    private long dailyLimit;

    /** Amount already spent within the current window day (minor units). */
    @Column(name = "daily_spent", nullable = false)
    private long dailySpent;

    /** The day the daily_spent counter belongs to; reset when a new day arrives. */
    @Column(name = "window_date", nullable = false)
    private LocalDate windowDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public long getPerTxnLimit() { return perTxnLimit; }
    public void setPerTxnLimit(long perTxnLimit) { this.perTxnLimit = perTxnLimit; }

    public long getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(long dailyLimit) { this.dailyLimit = dailyLimit; }

    public long getDailySpent() { return dailySpent; }
    public void setDailySpent(long dailySpent) { this.dailySpent = dailySpent; }

    public LocalDate getWindowDate() { return windowDate; }
    public void setWindowDate(LocalDate windowDate) { this.windowDate = windowDate; }
}
