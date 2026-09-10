package com.paytm.wallet.service.idempotency.impl;

import com.paytm.wallet.core.entity.TransferEntity;
import com.paytm.wallet.service.idempotency.IdempotencyService;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    @Override
    public TransferEntity getOrThrow(String idempotencyKey) {
        return null;
    }
}
