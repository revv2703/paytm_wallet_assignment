package com.paytm.wallet.service.wallet.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.paytm.wallet.common.dto.CreateWalletRequest;
import com.paytm.wallet.common.dto.WalletResponse;
import com.paytm.wallet.core.entity.WalletEntity;
import com.paytm.wallet.core.repository.WalletRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private Clock clock;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    private WalletServiceImpl walletService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        walletService = new WalletServiceImpl(walletRepository, clock, Optional.empty(), transactionManager);
    }

    @Test
    void getWalletShouldReturnWalletWhenExists() {
        String walletId = "wal_1";
        WalletEntity entity = new WalletEntity(walletId, "user_1", 500L, Instant.now());
        when(walletRepository.findByWalletId(walletId)).thenReturn(Optional.of(entity));

        WalletResponse response = walletService.getWallet(walletId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(walletId);
        assertThat(response.userId()).isEqualTo("user_1");
        assertThat(response.balancePaise()).isEqualTo(500L);
    }

    @Test
    void createOrGetWalletShouldReturnExistingWhenExists() {
        CreateWalletRequest request = new CreateWalletRequest("user_1");
        WalletEntity entity = new WalletEntity("wal_1", "user_1", 1000L, Instant.now());
        when(walletRepository.findByUserId("user_1")).thenReturn(Optional.of(entity));

        WalletResponse response = walletService.createOrGetWallet(request);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo("user_1");
        assertThat(response.balancePaise()).isEqualTo(1000L);
    }
}
