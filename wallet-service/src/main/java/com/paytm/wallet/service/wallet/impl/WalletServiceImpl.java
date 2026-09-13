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
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.paytm.wallet.common.util.Constants.WALLET_ID_PREFIX;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final Clock clock;

    public WalletServiceImpl(WalletRepository walletRepository, Clock clock) {
        this.walletRepository = walletRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public WalletResponse createOrGetWallet(CreateWalletRequest request) {
        String userId = SecurityUtil.resolveUserId(request);

        WalletEntity existing = walletRepository.findByUserId(userId).orElse(null);
        if (existing != null) {
            return WalletMapper.toResponse(existing);
        }

        WalletEntity newWallet = new WalletEntity(
                WALLET_ID_PREFIX + UUID.randomUUID(),
                userId,
                0L,
                Instant.now(clock)
        );

        try {
            return WalletMapper.toResponse(walletRepository.saveAndFlush(newWallet));
        } catch (DataIntegrityViolationException ex) {
            WalletEntity created = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new ApiException("Unable to create wallet for user: " + userId, HttpStatus.INTERNAL_SERVER_ERROR));
            return WalletMapper.toResponse(created);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWallet(String id) {
        if (id == null || id.isBlank()) {
            throw new ApiException("Wallet id is required", HttpStatus.BAD_REQUEST);
        }

        WalletEntity wallet = walletRepository.findByWalletId(id)
                .orElseThrow(() -> new ApiException("Wallet not found", HttpStatus.NOT_FOUND));

        return WalletMapper.toResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponse creditWallet(String walletId, long amountPaise) {
        WalletEntity wallet = walletRepository.findByWalletId(walletId)
                .orElseThrow(() -> new ApiException("Wallet not found", HttpStatus.NOT_FOUND));

        wallet.setBalancePaise(wallet.getBalancePaise() + amountPaise);
        return WalletMapper.toResponse(walletRepository.saveAndFlush(wallet));
    }


}
