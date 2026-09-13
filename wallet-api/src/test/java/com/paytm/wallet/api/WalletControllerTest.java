package com.paytm.wallet.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.paytm.wallet.api.controller.WalletController;
import com.paytm.wallet.common.dto.CreateWalletRequest;
import com.paytm.wallet.common.dto.WalletResponse;
import com.paytm.wallet.service.wallet.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController controller;

    @Test
    void createWalletShouldReturnCreated() {
        CreateWalletRequest request = new CreateWalletRequest("user-1");
        WalletResponse expected = new WalletResponse("wallet-1", "user-1", 0L, "ACTIVE");
        when(walletService.createOrGetWallet(request)).thenReturn(expected);

        ResponseEntity<WalletResponse> response = controller.createOrGetWallet(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expected);
    }
}
