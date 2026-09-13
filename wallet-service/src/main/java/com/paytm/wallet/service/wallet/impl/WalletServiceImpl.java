package com.paytm.wallet.service.wallet.impl;

import com.paytm.wallet.common.dto.CreateWalletRequest;
import com.paytm.wallet.common.dto.WalletResponse;
import com.paytm.wallet.common.exception.ApiException;
import com.paytm.wallet.core.entity.WalletEntity;
import com.paytm.wallet.core.repository.WalletRepository;
import com.paytm.wallet.service.util.SecurityUtil;
import com.paytm.wallet.service.mapper.WalletMapper;
import com.paytm.wallet.service.wallet.WalletService;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static com.paytm.wallet.common.util.Constants.WALLET_ID_PREFIX;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WalletServiceImpl implements WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletServiceImpl.class);

    private final WalletRepository walletRepository;
    private final Clock clock;
    private final Optional<CacheManager> cacheManager;
    private final TransactionTemplate transactionTemplate;

    public WalletServiceImpl(WalletRepository walletRepository, Clock clock, Optional<CacheManager> cacheManager, PlatformTransactionManager transactionManager) {
        this.walletRepository = walletRepository;
        this.clock = clock;
        this.cacheManager = cacheManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public WalletResponse createOrGetWallet(CreateWalletRequest request) {
        String userId = SecurityUtil.resolveUserId(request);
        logger.info("createOrGetWallet called for user={}", userId);

        WalletEntity existing = walletRepository.findByUserId(userId).orElse(null);
        if (existing != null) {
            logger.info("Existing wallet found for user={}", userId);
            return WalletMapper.toResponse(existing);
        }

        try {
            WalletEntity created = transactionTemplate.execute(status -> {
                WalletEntity newWallet = buildNewWallet(userId);
                return walletRepository.saveAndFlush(newWallet);
            });
            if (created != null) {
                cacheWalletBalance(created);
                return WalletMapper.toResponse(created);
            }
        } catch (DataIntegrityViolationException ex) {
            logger.warn("Wallet creation race for user={}, reloading existing wallet", userId, ex);
        }

        WalletEntity reloaded = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException("Unable to create wallet for user: " + userId, HttpStatus.INTERNAL_SERVER_ERROR));
        cacheWalletBalance(reloaded);
        return WalletMapper.toResponse(reloaded);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWallet(String id) {
        if (id == null || id.isBlank()) {
            throw new ApiException("Wallet id is required", HttpStatus.BAD_REQUEST);
        }

        WalletEntity wallet = walletRepository.findByWalletId(id)
                .orElseThrow(() -> new ApiException("Wallet not found", HttpStatus.NOT_FOUND));

        cacheWalletBalance(wallet);
        return WalletMapper.toResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponse creditWallet(String walletId, long amountPaise) {
        WalletEntity wallet = walletRepository.findByWalletId(walletId)
                .orElseThrow(() -> new ApiException("Wallet not found", HttpStatus.NOT_FOUND));

        wallet.setBalancePaise(wallet.getBalancePaise() + amountPaise);
        WalletEntity saved = walletRepository.saveAndFlush(wallet);
        cacheWalletBalance(saved);
        return WalletMapper.toResponse(saved);
    }

    private WalletEntity buildNewWallet(String userId) {
        return new WalletEntity(
                WALLET_ID_PREFIX + UUID.randomUUID(),
                userId,
                0L,
                Instant.now(clock)
        );
    }

    private void cacheWalletBalance(WalletEntity wallet) {
        cacheManager.ifPresent(cm -> {
            Cache cache = cm.getCache("walletBalances");
            if (cache != null) {
                cache.put(wallet.getWalletId(), wallet.getBalancePaise());
                logger.debug("Cached balance for walletId={} with value={}", wallet.getWalletId(), wallet.getBalancePaise());
            }
        });
    }
}
