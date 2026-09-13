package com.paytm.wallet.api.controller;

import com.paytm.wallet.common.dto.CreateTransferRequest;
import com.paytm.wallet.common.dto.TransferResponse;
import com.paytm.wallet.common.util.Constants;
import com.paytm.wallet.service.transfer.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
@Tag(name = "Transfers", description = "Peer-to-peer transfer processing APIs")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @Operation(summary = "Create a wallet transfer with idempotency protection")
    public ResponseEntity<TransferResponse> createTransfer(@Valid @RequestBody CreateTransferRequest request) {
        TransferResponse response = transferService.createTransfer(request);
        if (Constants.TRANSFER_COMPLETED.equals(response.status())) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
    if (Constants.TRANSFER_INSUFFICIENT_BALANCE.equals(response.declinedReason())) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(response);
        }
        // For declined or other non-completed statuses, return 200 OK with the resource state
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch transfer status by transfer id")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable String id) {
        return ResponseEntity.ok(transferService.getTransfer(id));
    }
}
