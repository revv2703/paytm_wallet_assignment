package com.paytm.wallet.core.repository;

import com.paytm.wallet.core.entity.TransferEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferRepository extends JpaRepository<TransferEntity, String> {

    Optional<TransferEntity> findByTransferId(String transferId);

    Optional<TransferEntity> findByIdempotencyKey(String idempotencyKey);
}
