package com.paytm.wallet.service.idempotency;

import com.paytm.wallet.core.entity.TransferEntity;

public interface IdempotencyService {

    TransferEntity getOrThrow(String idempotencyKey);
}
