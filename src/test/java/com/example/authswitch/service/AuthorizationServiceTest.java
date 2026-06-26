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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthorizationServiceTest {

    @Mock CardRepository cardRepository;
    @Mock CardLimitRepository cardLimitRepository;
    @Mock TransactionRepository transactionRepository;

    AuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new AuthorizationService(cardRepository, cardLimitRepository,
                transactionRepository, new MockIssuer());
        when(transactionRepository.findByStanAndRrn(anyString(), anyString())).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        when(cardLimitRepository.save(any(CardLimit.class))).thenAnswer(i -> i.getArgument(0));
    }

    private AuthorizationRequest request(String pan, long amount) {
        AuthorizationRequest r = new AuthorizationRequest();
        r.setPan(pan);
        r.setExpiry("3012");
        r.setAmount(amount);
        r.setCurrency("840");
        r.setStan("000001");
        r.setRrn("000000000001");
        return r;
    }

    private Card card(CardStatus status, String expiry, long balance) {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(balance);
        account.setCurrency("840");
        account.setStatus("ACTIVE");

        Card card = new Card();
        card.setId(1L);
        card.setPanHash("hash");
        card.setPanLast4("1111");
        card.setExpiry(expiry);
        card.setStatus(status);
        card.setAccount(account);
        return card;
    }

    private CardLimit limit(long perTxn, long daily, long spent) {
        CardLimit l = new CardLimit();
        l.setId(1L);
        l.setCardId(1L);
        l.setPerTxnLimit(perTxn);
        l.setDailyLimit(daily);
        l.setDailySpent(spent);
        l.setWindowDate(LocalDate.now(ZoneOffset.UTC));
        return l;
    }

    @Test
    void approvesWhenEverythingValid() {
        when(cardRepository.findByPanHash(anyString())).thenReturn(Optional.of(card(CardStatus.ACTIVE, "3012", 50000)));
        when(cardLimitRepository.findByCardId(1L)).thenReturn(Optional.of(limit(20000, 30000, 0)));

        AuthorizationResponse resp = service.authorize(request("4111111111111111", 1000));

        assertThat(resp.isApproved()).isTrue();
        assertThat(resp.getResponseCode()).isEqualTo("00");
        assertThat(resp.getAuthCode()).isNotBlank();
    }

    @Test
    void declinesUnknownCardWith14() {
        when(cardRepository.findByPanHash(anyString())).thenReturn(Optional.empty());

        AuthorizationResponse resp = service.authorize(request("4111111111111111", 1000));

        assertThat(resp.isApproved()).isFalse();
        assertThat(resp.getResponseCode()).isEqualTo("14");
    }

    @Test
    void declinesBlockedCardWith05() {
        when(cardRepository.findByPanHash(anyString())).thenReturn(Optional.of(card(CardStatus.BLOCKED, "3012", 50000)));

        AuthorizationResponse resp = service.authorize(request("4000000000000002", 1000));

        assertThat(resp.getResponseCode()).isEqualTo("05");
    }

    @Test
    void declinesExpiredCardWith54() {
        when(cardRepository.findByPanHash(anyString())).thenReturn(Optional.of(card(CardStatus.ACTIVE, "2001", 50000)));

        AuthorizationResponse resp = service.authorize(request("4000000000000010", 1000));

        assertThat(resp.getResponseCode()).isEqualTo("54");
    }

    @Test
    void declinesInsufficientFundsWith51() {
        when(cardRepository.findByPanHash(anyString())).thenReturn(Optional.of(card(CardStatus.ACTIVE, "3012", 500)));
        when(cardLimitRepository.findByCardId(1L)).thenReturn(Optional.of(limit(20000, 30000, 0)));

        AuthorizationResponse resp = service.authorize(request("5555555555554444", 1000));

        assertThat(resp.getResponseCode()).isEqualTo("51");
    }

    @Test
    void declinesOverPerTxnLimitWith61() {
        when(cardRepository.findByPanHash(anyString())).thenReturn(Optional.of(card(CardStatus.ACTIVE, "3012", 5_000_000)));
        when(cardLimitRepository.findByCardId(1L)).thenReturn(Optional.of(limit(20000, 30000, 0)));

        AuthorizationResponse resp = service.authorize(request("4111111111111111", 25000));

        assertThat(resp.getResponseCode()).isEqualTo("61");
    }

    @Test
    void isIdempotentOnReplay() {
        Transaction prior = new Transaction();
        prior.setStan("000001");
        prior.setRrn("000000000001");
        prior.setApproved(true);
        prior.setResponseCode("00");
        prior.setAuthCode("123456");
        when(transactionRepository.findByStanAndRrn("000001", "000000000001")).thenReturn(Optional.of(prior));

        AuthorizationResponse resp = service.authorize(request("4111111111111111", 1000));

        assertThat(resp.isApproved()).isTrue();
        assertThat(resp.getAuthCode()).isEqualTo("123456");
    }
}
