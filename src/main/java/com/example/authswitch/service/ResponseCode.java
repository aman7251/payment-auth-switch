package com.example.authswitch.service;

/**
 * ISO 8583 field 39 response codes. These two-digit codes are an industry standard;
 * the acquirer/terminal interprets them (e.g. "51" -> show "Insufficient funds").
 */
public enum ResponseCode {
    APPROVED("00", "Approved"),
    DO_NOT_HONOR("05", "Do not honor"),
    INVALID_CARD("14", "Invalid card number"),
    INSUFFICIENT_FUNDS("51", "Insufficient funds"),
    EXPIRED_CARD("54", "Expired card"),
    EXCEEDS_LIMIT("61", "Exceeds withdrawal amount limit"),
    ISSUER_UNAVAILABLE("91", "Issuer or switch inoperative"),
    SYSTEM_ERROR("96", "System malfunction");

    private final String code;
    private final String message;

    ResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() { return code; }
    public String message() { return message; }

    public boolean isApproved() { return this == APPROVED; }
}
