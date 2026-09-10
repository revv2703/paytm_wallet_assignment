package com.paytm.wallet.service.transfer.impl;

import com.paytm.wallet.common.dto.CreateTransferRequest;
import com.paytm.wallet.common.dto.TransferResponse;
import com.paytm.wallet.service.transfer.TransferService;
import org.springframework.stereotype.Service;

@Service
public class TransferServiceImpl implements TransferService {

    @Override
    public TransferResponse createTransfer(CreateTransferRequest request) {
        return null;
    }

    @Override
    public TransferResponse getTransfer(String id) {
        return null;
    }
}
