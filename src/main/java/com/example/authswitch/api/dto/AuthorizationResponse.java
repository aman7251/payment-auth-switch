package com.example.authswitch.api.dto;

/** The authorization decision returned to the caller (and mapped into ISO 0110). */
public class AuthorizationResponse {

    private boolean approved;
    private String responseCode;     // ISO field 39
    private String responseMessage;
    private String authCode;         // ISO field 38 (present when approved)
    private String stan;
    private String rrn;
    private long latencyMs;

    public AuthorizationResponse() {
    }

    public AuthorizationResponse(boolean approved, String responseCode, String responseMessage,
                                 String authCode, String stan, String rrn, long latencyMs) {
        this.approved = approved;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.authCode = authCode;
        this.stan = stan;
        this.rrn = rrn;
        this.latencyMs = latencyMs;
    }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }

    public String getResponseMessage() { return responseMessage; }
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }

    public String getAuthCode() { return authCode; }
    public void setAuthCode(String authCode) { this.authCode = authCode; }

    public String getStan() { return stan; }
    public void setStan(String stan) { this.stan = stan; }

    public String getRrn() { return rrn; }
    public void setRrn(String rrn) { this.rrn = rrn; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
}
