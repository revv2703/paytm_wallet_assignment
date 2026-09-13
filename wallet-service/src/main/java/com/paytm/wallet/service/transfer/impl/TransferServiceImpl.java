package com.paytm.wallet.service.transfer.impl;

import com.paytm.wallet.common.dto.CreateTransferRequest;
import com.paytm.wallet.common.dto.TransferResponse;
import com.paytm.wallet.common.exception.ApiException;
import com.paytm.wallet.common.util.Constants;
import com.paytm.wallet.core.entity.TransferEntity;
import com.paytm.wallet.core.entity.WalletEntity;
import com.paytm.wallet.core.repository.TransferRepository;
import com.paytm.wallet.core.repository.WalletRepository;
import com.paytm.wallet.service.idempotency.IdempotencyService;
import com.paytm.wallet.service.mapper.TransferMapper;
import com.paytm.wallet.service.metrics.WalletMetricsService;
import com.paytm.wallet.service.transfer.TransferService;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class TransferServiceImpl implements TransferService {

    private static final Logger logger = LoggerFactory.getLogger(TransferServiceImpl.class);

    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;
    private final Optional<CacheManager> cacheManager;
    private final WalletMetricsService walletMetricsService;

    public TransferServiceImpl(WalletRepository walletRepository, TransferRepository transferRepository, IdempotencyService idempotencyService, Clock clock, Optional<CacheManager> cacheManager, WalletMetricsService walletMetricsService) {
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
        this.cacheManager = cacheManager;
        this.walletMetricsService = walletMetricsService;
    }

    @Override
    @Transactional
    public TransferResponse createTransfer(CreateTransferRequest request) {
        walletMetricsService.incrementTransferRequest();
        Timer.Sample sample = walletMetricsService.startTransferTimer();

        try {
            if (request == null) {
                logger.warn("createTransfer called with null request");
                walletMetricsService.incrementTransferError();
                throw new ApiException(Constants.TRANSFER_REQUEST_REQUIRED, HttpStatus.BAD_REQUEST);
            }
            if (Objects.equals(request.from(), request.to())) {
                logger.warn("createTransfer validation failed: sender and receiver are same: {}", request.from());
                walletMetricsService.incrementTransferError();
                throw new ApiException(Constants.TRANSFER_SENDER_RECEIVER_SAME, HttpStatus.BAD_REQUEST);
            }

            String idempotencyKey = request.idempotencyKey().trim();
            logger.info("createTransfer request idempotencyKey={} from={} to={} amount={}", idempotencyKey, request.from(), request.to(), request.amountPaise());

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
                walletMetricsService.incrementTransferError();
                throw new ApiException(Constants.WALLET_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            Optional<TransferEntity> existingTransfer = idempotencyService.findByIdempotencyKey(idempotencyKey);
            if (existingTransfer.isPresent()) {
                logger.info("Existing transfer found for idempotencyKey={} after locking wallets", idempotencyKey);
                TransferEntity transfer = existingTransfer.get();
                if (!Objects.equals(transfer.getFromWalletId(), request.from())
                        || !Objects.equals(transfer.getToWalletId(), request.to())
                        || !Objects.equals(transfer.getAmountPaise(), request.amountPaise())) {
                    logger.warn("Idempotency key conflict for key={} (post-lock). existing(from={},to={},amount={}) vs request(from={},to={},amount={})",
                            idempotencyKey,
                            transfer.getFromWalletId(), transfer.getToWalletId(), transfer.getAmountPaise(),
                            request.from(), request.to(), request.amountPaise());
                    walletMetricsService.incrementTransferError();
                    throw new ApiException(Constants.TRANSFER_IDEMPOTENCY_KEY_CONFLICT, HttpStatus.CONFLICT);
                }
                walletMetricsService.incrementTransferIdempotentReplay();
                return TransferMapper.toResponse(transfer);
            }

            if (fromWallet.getBalancePaise() < request.amountPaise()) {
                walletMetricsService.incrementTransferDeclinedInsufficientFunds();
                return handleInsufficientBalance(request, idempotencyKey, fromWallet, toWallet);
            }

            applyTransferBalance(fromWallet, toWallet, request.amountPaise());
            cacheUpdatedWallets(fromWallet, toWallet);
            return saveCompletedTransfer(request, idempotencyKey);
        } catch (ApiException ex) {
            logger.error("Transfer request failed: {}", ex.getMessage(), ex);
            walletMetricsService.incrementTransferError();
            throw ex;
        } finally {
            walletMetricsService.finishTransferTimer(sample);
        }
    }

    private TransferResponse handleInsufficientBalance(CreateTransferRequest request, String idempotencyKey, WalletEntity fromWallet, WalletEntity toWallet) {
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
            logger.info("Declined transfer insert conflict for idempotencyKey={}, fetching existing", idempotencyKey);
            Optional<TransferEntity> existing = idempotencyService.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return TransferMapper.toResponse(existing.get());
            }
            logger.error("Failed to save declined transfer and no existing transfer found for idempotencyKey={}", idempotencyKey, ex);
            throw new ApiException(Constants.TRANSFER_CREATION_FAILED + ": failed to save declined transfer", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void applyTransferBalance(WalletEntity fromWallet, WalletEntity toWallet, long amountPaise) {
        fromWallet.setBalancePaise(fromWallet.getBalancePaise() - amountPaise);
        toWallet.setBalancePaise(toWallet.getBalancePaise() + amountPaise);
        walletRepository.saveAll(List.of(fromWallet, toWallet));
    }

    private void cacheUpdatedWallets(WalletEntity fromWallet, WalletEntity toWallet) {
//        TODO
//        cacheManager.ifPresent(cm -> {
//            Cache cache = cm.getCache("walletBalances");
//            if (cache != null) {
//                cache.put(fromWallet.getWalletId(), fromWallet.getBalancePaise());
//                cache.put(toWallet.getWalletId(), toWallet.getBalancePaise());
//                logger.debug("Updated cache balances for wallets {} and {}", fromWallet.getWalletId(), toWallet.getWalletId());
//            }
//        });
    }

    private TransferResponse saveCompletedTransfer(CreateTransferRequest request, String idempotencyKey) {
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
            TransferResponse response = TransferMapper.toResponse(transferRepository.saveAndFlush(transferEntity));
            walletMetricsService.incrementTransferCreated();
            return response;
        } catch (DataIntegrityViolationException ex) {
            logger.info("Completed transfer insert conflict for idempotencyKey={}, fetching existing", idempotencyKey);
            Optional<TransferEntity> existing = idempotencyService.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                walletMetricsService.incrementTransferCreated();
                return TransferMapper.toResponse(existing.get());
            }
            logger.error("Failed to save completed transfer and no existing transfer found for idempotencyKey={}", idempotencyKey, ex);
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
