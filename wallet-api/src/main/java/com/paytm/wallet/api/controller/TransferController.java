package com.paytm.wallet.api.controller;

import com.paytm.wallet.common.dto.CreateTransferRequest;
import com.paytm.wallet.common.dto.TransferResponse;
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
        return ResponseEntity.status(HttpStatus.CREATED).body(transferService.createTransfer(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch transfer status by transfer id")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable String id) {
        return ResponseEntity.ok(transferService.getTransfer(id));
    }
}
