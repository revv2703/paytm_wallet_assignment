package com.paytm.wallet.service.idempotency;

import com.paytm.wallet.core.entity.TransferEntity;
import java.util.Optional;

public interface IdempotencyService {

    TransferEntity getOrThrow(String idempotencyKey);

    Optional<TransferEntity> findByIdempotencyKey(String idempotencyKey);
}
