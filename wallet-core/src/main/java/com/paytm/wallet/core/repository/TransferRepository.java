package com.paytm.wallet.core.repository;

import com.paytm.wallet.core.entity.TransferEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferRepository extends JpaRepository<TransferEntity, Long> {

    Optional<TransferEntity> findByIdempotencyKey(String idempotencyKey);
}
