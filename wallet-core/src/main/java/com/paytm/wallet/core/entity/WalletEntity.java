package com.paytm.wallet.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "wallets")
public class WalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false)
    private Long balancePaise;

    @Column(nullable = false)
    private Instant createdAt;

    public WalletEntity() {
    }

    public WalletEntity(String userId, Long balancePaise, Instant createdAt) {
        this.userId = userId;
        this.balancePaise = balancePaise;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
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
