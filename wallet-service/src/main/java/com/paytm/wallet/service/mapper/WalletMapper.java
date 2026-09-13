package com.paytm.wallet.service.mapper;

import com.paytm.wallet.common.dto.WalletResponse;
import com.paytm.wallet.common.util.Constants;
import com.paytm.wallet.core.entity.WalletEntity;

public final class WalletMapper {

    private WalletMapper() {
    }

    public static WalletResponse toResponse(WalletEntity wallet) {
        if (wallet == null) {
            return null;
        }
        return new WalletResponse(
                wallet.getWalletId(),
                wallet.getUserId(),
                wallet.getBalancePaise(),
                Constants.WALLET_STATUS_ACTIVE
        );
    }
}
