package com.example.authswitch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** A cardholder account holding the available balance (in minor units, e.g. cents). */
@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Available balance in minor units (cents). */
    @Column(nullable = false)
    private long balance;

    /** ISO 4217 numeric currency code, e.g. "840" for USD. */
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 16)
    private String status;

    /** Optimistic lock: prevents two concurrent authorizations from corrupting the balance. */
    @Version
    private long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public long getBalance() { return balance; }
    public void setBalance(long balance) { this.balance = balance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
