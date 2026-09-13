package com.paytm.wallet.service.mapper;

import com.paytm.wallet.common.dto.TransferResponse;
import com.paytm.wallet.core.entity.TransferEntity;

public final class TransferMapper {

    private TransferMapper() {
    }

    public static TransferResponse toResponse(TransferEntity transfer) {
        if (transfer == null) {
            return null;
        }
        return new TransferResponse(
                transfer.getTransferId(),
                transfer.getFromWalletId(),
                transfer.getToWalletId(),
                transfer.getAmountPaise(),
                transfer.getStatus(),
                transfer.getIdempotencyKey(),
                transfer.getDeclinedReason()
        );
    }
}
