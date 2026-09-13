package com.paytm.wallet.service.idempotency.impl;

import com.paytm.wallet.common.exception.ApiException;
import com.paytm.wallet.core.entity.TransferEntity;
import com.paytm.wallet.core.repository.TransferRepository;
import com.paytm.wallet.service.idempotency.IdempotencyService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.paytm.wallet.common.util.Constants;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyServiceImpl.class);

    private final TransferRepository transferRepository;

    public IdempotencyServiceImpl(TransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    @Override
    public TransferEntity getOrThrow(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            logger.warn("getOrThrow called with null/blank idempotencyKey");
            throw new ApiException(Constants.IDEMPOTENCY_KEY_REQUIRED, HttpStatus.BAD_REQUEST);
        }

        return transferRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> {
                    logger.info("No transfer found for idempotencyKey={}", idempotencyKey);
                    return new ApiException(Constants.TRANSFER_NOT_FOUND_FOR_IDEMPOTENCY_KEY, HttpStatus.NOT_FOUND);
                });
    }

    @Override
    public Optional<TransferEntity> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            logger.debug("findByIdempotencyKey called with null/blank key");
            return Optional.empty();
        }
        return transferRepository.findByIdempotencyKey(idempotencyKey);
    }
}
