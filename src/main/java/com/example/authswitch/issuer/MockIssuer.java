package com.example.authswitch.issuer;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Stand-in for a real card issuer. In a production switch, after local checks the
 * switch forwards the request to the issuing bank. Here we simulate that hop:
 * - issuers are "available" unless the PAN starts with 9999 (lets us demo code 91)
 * - an approval returns a 6-digit authorization code
 */
@Component
public class MockIssuer {

    private final SecureRandom random = new SecureRandom();

    public boolean isAvailable(String pan) {
        return !pan.startsWith("9999");
    }

    public String generateAuthCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
