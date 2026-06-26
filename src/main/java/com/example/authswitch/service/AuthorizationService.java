package com.example.authswitch.service;

import com.example.authswitch.api.dto.AuthorizationRequest;
import com.example.authswitch.api.dto.AuthorizationResponse;
import com.example.authswitch.domain.Account;
import com.example.authswitch.domain.Card;
import com.example.authswitch.domain.CardLimit;
import com.example.authswitch.domain.CardStatus;
import com.example.authswitch.domain.Transaction;
import com.example.authswitch.issuer.MockIssuer;
import com.example.authswitch.repo.CardLimitRepository;
import com.example.authswitch.repo.CardRepository;
import com.example.authswitch.repo.TransactionRepository;
import com.example.authswitch.util.PanHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * The heart of the switch: takes an authorization request, runs the checks a real
 * card switch runs (card valid? not expired? within limits? funds available?),
 * decides an ISO 8583 response code, updates balances, and records the transaction.
 */
@Service
public class AuthorizationService {

    private final CardRepository cardRepository;
    private final CardLimitRepository cardLimitRepository;
    private final TransactionRepository transactionRepository;
    private final MockIssuer issuer;

    public AuthorizationService(CardRepository cardRepository,
                                CardLimitRepository cardLimitRepository,
                                TransactionRepository transactionRepository,
                                MockIssuer issuer) {
        this.cardRepository = cardRepository;
        this.cardLimitRepository = cardLimitRepository;
        this.transactionRepository = transactionRepository;
        this.issuer = issuer;
    }

    @Transactional
    public AuthorizationResponse authorize(AuthorizationRequest req) {
        long start = System.nanoTime();

        // Idempotency: if we've already seen this STAN+RRN, return the original decision.
        Optional<Transaction> existing = transactionRepository.findByStanAndRrn(req.getStan(), req.getRrn());
        if (existing.isPresent()) {
            return toResponse(existing.get(), elapsedMs(start));
        }

        String panHash = PanHasher.sha256Hex(req.getPan());
        String last4 = req.getPan().substring(req.getPan().length() - 4);

        ResponseCode decision = decide(req, panHash);

        // Apply side effects only on approval.
        String authCode = null;
        if (decision.isApproved()) {
            Card card = cardRepository.findByPanHash(panHash).orElseThrow();
            CardLimit limit = cardLimitRepository.findByCardId(card.getId()).orElseThrow();
            Account account = card.getAccount();

            account.setBalance(account.getBalance() - req.getAmount());
            limit.setDailySpent(limit.getDailySpent() + req.getAmount());
            cardLimitRepository.save(limit);
            // account is managed; the balance change flushes on commit
            authCode = issuer.generateAuthCode();
        }

        Transaction txn = new Transaction();
        txn.setStan(req.getStan());
        txn.setRrn(req.getRrn());
        txn.setPanLast4(last4);
        txn.setMti("0100");
        txn.setAmount(req.getAmount());
        txn.setCurrency(req.getCurrency());
        txn.setResponseCode(decision.code());
        txn.setApproved(decision.isApproved());
        txn.setAuthCode(authCode);
        txn.setCreatedAt(Instant.now());
        txn.setLatencyMs(elapsedMs(start));
        transactionRepository.save(txn);

        return new AuthorizationResponse(
                decision.isApproved(), decision.code(), decision.message(),
                authCode, req.getStan(), req.getRrn(), txn.getLatencyMs());
    }

    /** Runs the rule chain and returns the first failing reason, or APPROVED. */
    private ResponseCode decide(AuthorizationRequest req, String panHash) {
        Optional<Card> cardOpt = cardRepository.findByPanHash(panHash);
        if (cardOpt.isEmpty()) {
            return ResponseCode.INVALID_CARD;
        }
        Card card = cardOpt.get();

        if (card.getStatus() == CardStatus.BLOCKED) {
            return ResponseCode.DO_NOT_HONOR;
        }
        if (card.getStatus() == CardStatus.EXPIRED || isExpired(card.getExpiry())) {
            return ResponseCode.EXPIRED_CARD;
        }
        if (!issuer.isAvailable(req.getPan())) {
            return ResponseCode.ISSUER_UNAVAILABLE;
        }

        CardLimit limit = cardLimitRepository.findByCardId(card.getId()).orElse(null);
        if (limit == null) {
            return ResponseCode.SYSTEM_ERROR;
        }
        rollDailyWindowIfNeeded(limit);

        if (req.getAmount() > limit.getPerTxnLimit()) {
            return ResponseCode.EXCEEDS_LIMIT;
        }
        if (limit.getDailySpent() + req.getAmount() > limit.getDailyLimit()) {
            return ResponseCode.EXCEEDS_LIMIT;
        }
        if (card.getAccount().getBalance() < req.getAmount()) {
            return ResponseCode.INSUFFICIENT_FUNDS;
        }
        return ResponseCode.APPROVED;
    }

    /** Reset the rolling daily-spend counter when a new day starts. */
    private void rollDailyWindowIfNeeded(CardLimit limit) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (!today.equals(limit.getWindowDate())) {
            limit.setWindowDate(today);
            limit.setDailySpent(0);
        }
    }

    /** expiry is YYMM (e.g. 3012). Expired once we pass the end of that month. */
    private boolean isExpired(String expiry) {
        int yy = Integer.parseInt(expiry.substring(0, 2));
        int mm = Integer.parseInt(expiry.substring(2, 4));
        YearMonth exp = YearMonth.of(2000 + yy, mm);
        return YearMonth.now(ZoneOffset.UTC).isAfter(exp);
    }

    private AuthorizationResponse toResponse(Transaction txn, long latencyMs) {
        ResponseCode rc = fromCode(txn.getResponseCode());
        return new AuthorizationResponse(
                txn.isApproved(), txn.getResponseCode(), rc.message(),
                txn.getAuthCode(), txn.getStan(), txn.getRrn(), latencyMs);
    }

    private ResponseCode fromCode(String code) {
        for (ResponseCode rc : ResponseCode.values()) {
            if (rc.code().equals(code)) {
                return rc;
            }
        }
        return ResponseCode.SYSTEM_ERROR;
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
