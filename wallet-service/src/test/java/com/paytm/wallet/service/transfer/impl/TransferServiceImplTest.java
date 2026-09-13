package com.paytm.wallet.service.transfer.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.paytm.wallet.common.dto.CreateTransferRequest;
import com.paytm.wallet.common.dto.TransferResponse;
import com.paytm.wallet.common.exception.ApiException;
import com.paytm.wallet.core.entity.WalletEntity;
import com.paytm.wallet.core.repository.TransferRepository;
import com.paytm.wallet.core.repository.WalletRepository;
import com.paytm.wallet.service.idempotency.IdempotencyService;
import com.paytm.wallet.service.metrics.WalletMetricsService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private Clock clock;

    @Mock
    private WalletMetricsService walletMetricsService;

    private TransferServiceImpl transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferServiceImpl(
                walletRepository,
                transferRepository,
                idempotencyService,
                clock,
                Optional.empty(),
                walletMetricsService
        );

        // Mock Security Context
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user_1");
        
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createTransferShouldThrowExceptionWhenUserDoesNotOwnFromWallet() {
        CreateTransferRequest request = new CreateTransferRequest(
                "wal_from",
                "wal_to",
                100L,
                "key_123"
        );

        WalletEntity fromWallet = new WalletEntity("wal_from", "user_different", 500L, Instant.now());
        WalletEntity toWallet = new WalletEntity("wal_to", "user_2", 200L, Instant.now());

        when(walletRepository.findAllByWalletIdInForUpdate(any())).thenReturn(List.of(fromWallet, toWallet));

        assertThatThrownBy(() -> transferService.createTransfer(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("You do not own the sender wallet");
    }
}
