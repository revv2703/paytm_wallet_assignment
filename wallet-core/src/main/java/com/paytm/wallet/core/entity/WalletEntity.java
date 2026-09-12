package com.paytm.wallet.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "wallets")
public class WalletEntity {

    @Id
    @Column(name = "wallet_id", nullable = false, unique = true)
    private String walletId;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "balance_paise", nullable = false)
    private Long balancePaise;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public WalletEntity() {
    }

    public WalletEntity(String walletId, String userId, Long balancePaise, Instant createdAt) {
        this.walletId = walletId;
        this.userId = userId;
        this.balancePaise = balancePaise;
        this.createdAt = createdAt;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getBalancePaise() {
        return balancePaise;
    }

    public void setBalancePaise(Long balancePaise) {
        this.balancePaise = balancePaise;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
