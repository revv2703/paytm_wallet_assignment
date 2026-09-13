package com.paytm.wallet.service.transfer.impl;

import com.paytm.wallet.common.dto.CreateTransferRequest;
import com.paytm.wallet.common.dto.TransferResponse;
import com.paytm.wallet.common.exception.ApiException;
import com.paytm.wallet.common.util.Constants;
import com.paytm.wallet.core.entity.TransferEntity;
import com.paytm.wallet.core.entity.WalletEntity;
import com.paytm.wallet.core.repository.TransferRepository;
import com.paytm.wallet.core.repository.WalletRepository;
import com.paytm.wallet.service.transfer.TransferService;
import com.paytm.wallet.service.mapper.TransferMapper;
import com.paytm.wallet.service.idempotency.IdempotencyService;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TransferServiceImpl implements TransferService {

    private static final Logger logger = LoggerFactory.getLogger(TransferServiceImpl.class);

    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;
    private final Optional<CacheManager> cacheManager;

    public TransferServiceImpl(WalletRepository walletRepository, TransferRepository transferRepository, IdempotencyService idempotencyService, Clock clock, Optional<CacheManager> cacheManager) {
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
        this.cacheManager = cacheManager;
    }

    @Override
    @Transactional
    public TransferResponse createTransfer(CreateTransferRequest request) {
        if (request == null) {
            logger.warn("createTransfer called with null request");
            throw new ApiException(Constants.TRANSFER_REQUEST_REQUIRED, HttpStatus.BAD_REQUEST);
        }
        if (Objects.equals(request.from(), request.to())) {
            logger.warn("createTransfer validation failed: sender and receiver are same: {}", request.from());
            throw new ApiException(Constants.TRANSFER_SENDER_RECEIVER_SAME, HttpStatus.BAD_REQUEST);
        }

        String idempotencyKey = request.idempotencyKey().trim();
        logger.info("createTransfer request idempotencyKey={} from={} to={} amount={}", idempotencyKey, request.from(), request.to(), request.amountPaise());

        Optional<TransferEntity> existingTransfer = idempotencyService.findByIdempotencyKey(idempotencyKey);
        if (existingTransfer.isPresent()) {
            logger.info("Existing transfer found for idempotencyKey={}", idempotencyKey);
            TransferEntity transfer = existingTransfer.get();
            if (!Objects.equals(transfer.getFromWalletId(), request.from())
                    || !Objects.equals(transfer.getToWalletId(), request.to())
                    || !Objects.equals(transfer.getAmountPaise(), request.amountPaise())) {
                logger.warn("Idempotency key conflict for key={}. existing(from={},to={},amount={}) vs request(from={},to={},amount={})",
                        idempotencyKey,
                        transfer.getFromWalletId(), transfer.getToWalletId(), transfer.getAmountPaise(),
                        request.from(), request.to(), request.amountPaise());
                throw new ApiException(Constants.TRANSFER_IDEMPOTENCY_KEY_CONFLICT, HttpStatus.CONFLICT);
            }
            return TransferMapper.toResponse(transfer);
        }

        List<String> orderedWalletIds = Stream.of(request.from(), request.to())
                .sorted(Comparator.naturalOrder())
                .distinct()
                .toList();

        List<WalletEntity> walletEntities = walletRepository.findAllByWalletIdInForUpdate(orderedWalletIds);
        Map<String, WalletEntity> walletMap = walletEntities.stream()
                .collect(Collectors.toMap(WalletEntity::getWalletId, wallet -> wallet));

        WalletEntity fromWallet = walletMap.get(request.from());
        WalletEntity toWallet = walletMap.get(request.to());
        if (fromWallet == null || toWallet == null) {
            throw new ApiException(Constants.WALLET_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        if (fromWallet.getBalancePaise() < request.amountPaise()) {
            String reason = Constants.TRANSFER_INSUFFICIENT_BALANCE;
            TransferEntity declined = new TransferEntity(
                    Constants.TRANSFER_ID_PREFIX + UUID.randomUUID(),
                    request.from(),
                    request.to(),
                    request.amountPaise(),
                    idempotencyKey,
                    Constants.TRANSFER_DECLINED,
                    Instant.now(clock)
            );
            declined.setDeclinedReason(reason);
            try {
                logger.info("Persisting declined transfer for idempotencyKey={} due to insufficient balance", idempotencyKey);
                return TransferMapper.toResponse(transferRepository.saveAndFlush(declined));
            } catch (DataIntegrityViolationException ex) {
                logger.error("Failed to save declined transfer for idempotencyKey={}", idempotencyKey, ex);
                Optional<TransferEntity> existing = idempotencyService.findByIdempotencyKey(idempotencyKey);
                if (existing.isPresent()) {
                    return TransferMapper.toResponse(existing.get());
                }
                throw new ApiException(Constants.TRANSFER_CREATION_FAILED + ": failed to save declined transfer", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        fromWallet.setBalancePaise(fromWallet.getBalancePaise() - request.amountPaise());
        toWallet.setBalancePaise(toWallet.getBalancePaise() + request.amountPaise());
        walletRepository.saveAll(List.of(fromWallet, toWallet));

        // update cache if available
        cacheManager.ifPresent(cm -> {
            Cache cache = cm.getCache("walletBalances");
            if (cache != null) {
                cache.put(fromWallet.getWalletId(), fromWallet.getBalancePaise());
                cache.put(toWallet.getWalletId(), toWallet.getBalancePaise());
                logger.debug("Updated cache balances for wallets {} and {}", fromWallet.getWalletId(), toWallet.getWalletId());
            }
        });

        TransferEntity transferEntity = new TransferEntity(
                Constants.TRANSFER_ID_PREFIX + UUID.randomUUID(),
                request.from(),
                request.to(),
                request.amountPaise(),
                idempotencyKey,
                Constants.TRANSFER_COMPLETED,
                Instant.now(clock)
        );

        try {
            logger.info("Persisting completed transfer for idempotencyKey={}", idempotencyKey);
            return TransferMapper.toResponse(transferRepository.saveAndFlush(transferEntity));
        } catch (DataIntegrityViolationException ex) {
            logger.error("Failed to save completed transfer for idempotencyKey={}", idempotencyKey, ex);
            Optional<TransferEntity> existing = idempotencyService.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return TransferMapper.toResponse(existing.get());
            }
            throw new ApiException(Constants.TRANSFER_CREATION_FAILED + ": database error while creating transfer", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse getTransfer(String id) {
        if (id == null || id.isBlank()) {
            logger.warn("getTransfer called with null/blank id");
            throw new ApiException(Constants.TRANSFER_ID_REQUIRED, HttpStatus.BAD_REQUEST);
        }

        logger.info("getTransfer called for id={}", id);
        TransferEntity transfer = transferRepository.findByTransferId(id)
                .orElseThrow(() -> {
                    logger.info("Transfer not found for id={}", id);
                    return new ApiException(Constants.TRANSFER_NOT_FOUND, HttpStatus.NOT_FOUND);
                });
        return TransferMapper.toResponse(transfer);
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new ApiException("Authentication required", HttpStatus.UNAUTHORIZED);
        }
        return authentication.getName();
    }
}
