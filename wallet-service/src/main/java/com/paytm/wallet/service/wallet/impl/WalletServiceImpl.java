package com.paytm.wallet.service.wallet.impl;

import com.paytm.wallet.common.dto.CreateWalletRequest;
import com.paytm.wallet.common.dto.WalletResponse;
import com.paytm.wallet.service.wallet.WalletService;
import org.springframework.stereotype.Service;

@Service
public class WalletServiceImpl implements WalletService {

    @Override
    public WalletResponse createOrGetWallet(CreateWalletRequest request) {
        return null;
    }

    @Override
    public WalletResponse getWallet(String id) {
        return null;
    }
}
