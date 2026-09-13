package com.paytm.wallet.api.controller;

import com.paytm.wallet.service.metrics.WalletMetricsService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsController {

    private final WalletMetricsService walletMetricsService;

    public MetricsController(WalletMetricsService walletMetricsService) {
        this.walletMetricsService = walletMetricsService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics() {
        return ResponseEntity.ok(walletMetricsService.snapshot());
    }
}
