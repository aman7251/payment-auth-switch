package com.example.authswitch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * An audit record of every authorization attempt. The (stan, rrn) pair is unique,
 * which gives us idempotency: a retried message returns the original decision.
 */
@Entity
@Table(name = "transaction",
        uniqueConstraints = @UniqueConstraint(name = "uq_txn_stan_rrn", columnNames = {"stan", "rrn"}))
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** System Trace Audit Number (ISO 8583 field 11). */
    @Column(nullable = false, length = 12)
    private String stan;

    /** Retrieval Reference Number (ISO 8583 field 37). */
    @Column(nullable = false, length = 12)
    private String rrn;

    @Column(name = "pan_last4", nullable = false, length = 4)
    private String panLast4;

    @Column(nullable = false, length = 4)
    private String mti;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, length = 3)
    private String currency;

    /** ISO 8583 field 39 response code, e.g. "00" approved. */
    @Column(name = "response_code", nullable = false, length = 2)
    private String responseCode;

    @Column(nullable = false)
    private boolean approved;

    @Column(name = "auth_code", length = 6)
    private String authCode;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStan() { return stan; }
    public void setStan(String stan) { this.stan = stan; }

    public String getRrn() { return rrn; }
    public void setRrn(String rrn) { this.rrn = rrn; }

    public String getPanLast4() { return panLast4; }
    public void setPanLast4(String panLast4) { this.panLast4 = panLast4; }

    public String getMti() { return mti; }
    public void setMti(String mti) { this.mti = mti; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getAuthCode() { return authCode; }
    public void setAuthCode(String authCode) { this.authCode = authCode; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
