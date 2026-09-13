package com.paytm.wallet.api.controller;

import com.paytm.wallet.common.dto.CreateWalletRequest;
import com.paytm.wallet.common.dto.WalletResponse;
import com.paytm.wallet.service.wallet.WalletService;
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
@RequestMapping("/wallets")
@Tag(name = "Wallets", description = "Wallet create and lookup APIs")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    @Operation(summary = "Create or fetch a wallet for a user")
    public ResponseEntity<WalletResponse> createOrGetWallet(@Valid @RequestBody CreateWalletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(walletService.createOrGetWallet(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch wallet balance by wallet id")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable String id) {
        return ResponseEntity.ok(walletService.getWallet(id));
    }

    @PostMapping("/{id}/credit")
    @Operation(summary = "Credit wallet balance by wallet id")
    public ResponseEntity<WalletResponse> creditWallet(@PathVariable String id, @RequestBody long amount) {
        return ResponseEntity.ok(walletService.creditWallet(id, amount));
    }
}
