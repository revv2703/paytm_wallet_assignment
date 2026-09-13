package com.paytm.wallet.core.repository;

import com.paytm.wallet.core.entity.WalletEntity;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends JpaRepository<WalletEntity, String> {

    Optional<WalletEntity> findByUserId(String userId);

    Optional<WalletEntity> findByWalletId(String walletId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletEntity w where w.walletId = :walletId")
    Optional<WalletEntity> findByWalletIdForUpdate(@Param("walletId") String walletId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletEntity w where w.walletId in :ids order by w.walletId asc")
    List<WalletEntity> findAllByWalletIdInForUpdate(@Param("ids") List<String> ids);
}
