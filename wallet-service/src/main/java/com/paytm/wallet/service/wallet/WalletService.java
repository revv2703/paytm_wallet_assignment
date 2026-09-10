package com.paytm.wallet.service.wallet;

import com.paytm.wallet.common.dto.CreateWalletRequest;
import com.paytm.wallet.common.dto.WalletResponse;

public interface WalletService {

    WalletResponse createOrGetWallet(CreateWalletRequest request);

    WalletResponse getWallet(String id);
}
