package com.example.authswitch.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Incoming authorization request (the JSON body of POST /authorize).
 * Mirrors the meaningful ISO 8583 fields so the REST and ISO paths share one engine.
 */
public class AuthorizationRequest {

    /** Card number / PAN (ISO field 2). Test PANs only. */
    @NotBlank
    @Pattern(regexp = "\\d{12,19}", message = "pan must be 12-19 digits")
    private String pan;

    /** Expiry as YYMM (ISO field 14), e.g. 3012 for Dec 2030. */
    @NotBlank
    @Pattern(regexp = "\\d{4}", message = "expiry must be YYMM")
    private String expiry;

    /** Amount in minor units / cents (ISO field 4). */
    @Positive
    private long amount;

    /** ISO 4217 numeric currency (ISO field 49), e.g. 840 = USD. */
    @Pattern(regexp = "\\d{3}", message = "currency must be a 3-digit ISO code")
    private String currency = "840";

    /** System Trace Audit Number (ISO field 11). */
    @NotBlank
    private String stan;

    /** Retrieval Reference Number (ISO field 37). */
    @NotBlank
    private String rrn;

    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }

    public String getExpiry() { return expiry; }
    public void setExpiry(String expiry) { this.expiry = expiry; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStan() { return stan; }
    public void setStan(String stan) { this.stan = stan; }

    public String getRrn() { return rrn; }
    public void setRrn(String rrn) { this.rrn = rrn; }
}
