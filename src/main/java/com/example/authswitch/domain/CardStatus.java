package com.example.authswitch.domain;

/** Lifecycle state of a card. Only ACTIVE cards can authorize. */
public enum CardStatus {
    ACTIVE,
    BLOCKED,
    EXPIRED
}
