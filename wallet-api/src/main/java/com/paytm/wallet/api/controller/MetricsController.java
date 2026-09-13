package com.paytm.wallet.api.controller;

import com.paytm.wallet.service.metrics.WalletMetricsService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsController {

    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);

    private final WalletMetricsService walletMetricsService;
    private final String dashboardHtml;

    public MetricsController(WalletMetricsService walletMetricsService) {
        this.walletMetricsService = walletMetricsService;
        this.dashboardHtml = loadDashboardHtml();
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics() {
        logger.info("REST request to fetch live metrics snapshot");
        return ResponseEntity.ok(walletMetricsService.snapshot());
    }

    @GetMapping(value = "/dashboard", produces = MediaType.TEXT_HTML_VALUE)
    public String dashboard() {
        logger.info("REST request to serve visual metrics dashboard HTML page");
        return dashboardHtml;
    }

    private String loadDashboardHtml() {
        logger.info("Loading dashboard.html template from classpath resources...");
        try (InputStream is = getClass().getResourceAsStream("/dashboard.html")) {
            if (is == null) {
                logger.error("Failed to load dashboard.html: Resource not found in classpath!");
                throw new IllegalStateException("dashboard.html is missing");
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            logger.info("dashboard.html successfully loaded and cached in memory.");
            return content;
        } catch (IOException e) {
            logger.error("Exception occurred while loading dashboard.html template", e);
            throw new IllegalStateException("Failed to load dashboard.html", e);
        }
    }
}
