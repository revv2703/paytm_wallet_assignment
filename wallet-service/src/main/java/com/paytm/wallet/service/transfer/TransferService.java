package com.paytm.wallet.service.transfer;

import com.paytm.wallet.common.dto.CreateTransferRequest;
import com.paytm.wallet.common.dto.TransferResponse;

public interface TransferService {

    TransferResponse createTransfer(CreateTransferRequest request);

    TransferResponse getTransfer(String id);
}
