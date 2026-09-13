package com.paytm.wallet.api.controller;

import com.paytm.wallet.service.metrics.WalletMetricsService;
import java.util.Map;
import org.springframework.http.MediaType;
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

    @GetMapping(value = "/dashboard", produces = MediaType.TEXT_HTML_VALUE)
    public String dashboard() {
        return """
        <!DOCTYPE html>
        <html lang="en" class="dark">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Paytm Wallet Metrics Dashboard</title>
            <script src="https://cdn.tailwindcss.com"></script>
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
            <script>
                tailwind.config = {
                    darkMode: 'class',
                    theme: {
                        extend: {
                            colors: {
                                brand: {
                                    blue: '#002E6E',
                                    cyan: '#00B9F5'
                                }
                            }
                        }
                    }
                }
            </script>
        </head>
        <body class="bg-slate-950 text-slate-100 min-h-screen flex flex-col font-sans">
            <header class="bg-slate-900 border-b border-slate-800 py-4 px-6 sticky top-0 z-50 shadow-md">
                <div class="max-w-7xl mx-auto flex justify-between items-center">
                    <div class="flex items-center space-x-3">
                        <span class="text-2xl font-black tracking-wider text-transparent bg-clip-text bg-gradient-to-r from-brand-cyan to-blue-400">PAYTM WALLET</span>
                        <span class="bg-slate-800 text-brand-cyan text-xs font-semibold px-2.5 py-0.5 rounded border border-brand-cyan/20 font-mono">LIVE METRICS</span>
                    </div>
                    <div class="flex items-center space-x-4 text-xs text-slate-400">
                        <span id="connection-status" class="flex items-center space-x-1.5 bg-emerald-950 text-emerald-400 px-3 py-1 rounded-full border border-emerald-500/20 font-bold font-mono">
                            <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                            <span>CONNECTED</span>
                        </span>
                        <span id="last-updated" class="font-mono text-slate-500">Last update: --</span>
                    </div>
                </div>
            </header>

            <main class="flex-1 max-w-7xl w-full mx-auto p-6 space-y-6">
                <!-- Stats Grid -->
                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                    <div class="bg-slate-900 p-6 rounded-xl border border-slate-800 shadow-sm relative overflow-hidden">
                        <div class="absolute top-0 right-0 w-24 h-24 bg-brand-cyan/5 rounded-full filter blur-xl translate-x-8 -translate-y-8"></div>
                        <h3 class="text-xs font-bold uppercase tracking-wider text-slate-400">Total Requests</h3>
                        <p id="stat-requests" class="text-4xl font-extrabold mt-2 text-slate-50 font-mono">0</p>
                        <div class="text-xs text-brand-cyan mt-1 font-bold uppercase tracking-wide">Processed live</div>
                    </div>
                    
                    <div class="bg-slate-900 p-6 rounded-xl border border-slate-800 shadow-sm relative overflow-hidden">
                        <div class="absolute top-0 right-0 w-24 h-24 bg-emerald-500/5 rounded-full filter blur-xl translate-x-8 -translate-y-8"></div>
                        <h3 class="text-xs font-bold uppercase tracking-wider text-slate-400">Successful Transfers</h3>
                        <p id="stat-created" class="text-4xl font-extrabold mt-2 text-emerald-400 font-mono">0</p>
                        <div id="created-percent" class="text-xs text-slate-500 mt-1 font-semibold uppercase">--% of total</div>
                    </div>

                    <div class="bg-slate-900 p-6 rounded-xl border border-slate-800 shadow-sm relative overflow-hidden">
                        <div class="absolute top-0 right-0 w-24 h-24 bg-amber-500/5 rounded-full filter blur-xl translate-x-8 -translate-y-8"></div>
                        <h3 class="text-xs font-bold uppercase tracking-wider text-slate-400">Declined (No Funds)</h3>
                        <p id="stat-declined" class="text-4xl font-extrabold mt-2 text-amber-500 font-mono">0</p>
                        <div id="declined-percent" class="text-xs text-slate-500 mt-1 font-semibold uppercase">--% of total</div>
                    </div>

                    <div class="bg-slate-900 p-6 rounded-xl border border-slate-800 shadow-sm relative overflow-hidden">
                        <div class="absolute top-0 right-0 w-24 h-24 bg-rose-500/5 rounded-full filter blur-xl translate-x-8 -translate-y-8"></div>
                        <h3 class="text-xs font-bold uppercase tracking-wider text-slate-400">Idempotency Replays</h3>
                        <p id="stat-replays" class="text-4xl font-extrabold mt-2 text-violet-400 font-mono">0</p>
                        <div id="replays-percent" class="text-xs text-slate-500 mt-1 font-semibold uppercase">--% of total</div>
                    </div>
                </div>

                <!-- Charts & Details -->
                <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    <!-- Left: Chart -->
                    <div class="bg-slate-900 p-6 rounded-xl border border-slate-800 lg:col-span-2 flex flex-col justify-between">
                        <div class="flex justify-between items-center mb-4">
                            <h3 class="text-lg font-extrabold tracking-tight">Active Transfer Activity</h3>
                            <div class="text-xs text-slate-500 font-semibold uppercase tracking-wider">Historical trend (last 10 updates)</div>
                        </div>
                        <div class="relative h-64 w-full flex items-center justify-center">
                            <canvas id="metricsChart"></canvas>
                        </div>
                    </div>

                    <!-- Right: System Info & Logs -->
                    <div class="bg-slate-900 p-6 rounded-xl border border-slate-800 flex flex-col justify-between">
                        <div>
                            <h3 class="text-lg font-extrabold tracking-tight mb-4">System Performance</h3>
                            <div class="space-y-4 font-mono text-sm">
                                <div class="flex justify-between items-center border-b border-slate-800 pb-3">
                                    <span class="text-slate-400 font-semibold uppercase tracking-wider text-xs">Average Error Rate</span>
                                    <span id="perf-errors" class="font-bold text-slate-100">0%</span>
                                </div>
                                <div class="flex justify-between items-center border-b border-slate-800 pb-3">
                                    <span class="text-slate-400 font-semibold uppercase tracking-wider text-xs">P99 Response Latency</span>
                                    <span id="perf-latency" class="font-bold text-brand-cyan">0 ms</span>
                                </div>
                                <div class="flex justify-between items-center border-b border-slate-800 pb-3">
                                    <span class="text-slate-400 font-semibold uppercase tracking-wider text-xs">DB Pool Connections</span>
                                    <span class="font-bold text-emerald-400">Hikari [Active]</span>
                                </div>
                                <div class="flex justify-between items-center">
                                    <span class="text-slate-400 font-semibold uppercase tracking-wider text-xs">Platform Runtime</span>
                                    <span class="font-bold text-slate-100">Java 25 + Spring 4</span>
                                </div>
                            </div>
                        </div>
                        <div class="mt-6 pt-6 border-t border-slate-800">
                            <div class="text-xs text-slate-500 text-center font-semibold uppercase tracking-wider">
                                Securely monitored Paytm peer-to-peer wallet service
                            </div>
                        </div>
                    </div>
                </div>
            </main>

            <footer class="bg-slate-900/50 border-t border-slate-800 py-4 px-6 text-center text-xs text-slate-500 mt-auto font-mono">
                <p>&copy; 2026 Paytm Wallet Service. Designed for high availability & exact consistency.</p>
            </footer>

            <script>
                const chartCtx = document.getElementById('metricsChart').getContext('2d');
                const labels = Array(15).fill('');
                const requestsData = Array(15).fill(0);
                const createdData = Array(15).fill(0);

                const metricsChart = new Chart(chartCtx, {
                    type: 'line',
                    data: {
                        labels: labels,
                        datasets: [
                            {
                                label: 'Total Requests',
                                data: requestsData,
                                borderColor: '#00B9F5',
                                backgroundColor: 'rgba(0, 185, 245, 0.05)',
                                borderWidth: 3,
                                tension: 0.3,
                                fill: true
                            },
                            {
                                label: 'Successful Transfers',
                                data: createdData,
                                borderColor: '#34D399',
                                backgroundColor: 'rgba(52, 211, 153, 0.05)',
                                borderWidth: 3,
                                tension: 0.3,
                                fill: true
                            }
                        ]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                labels: { color: '#94A3B8', font: { family: 'monospace', size: 11, weight: 'bold' } }
                            }
                        },
                        scales: {
                            x: { grid: { color: '#1E293B' }, ticks: { color: '#64748B', font: { family: 'monospace' } } },
                            y: { grid: { color: '#1E293B' }, ticks: { color: '#64748B', font: { family: 'monospace' } } }
                        }
                    }
                });

                async function fetchMetrics() {
                    try {
                        const response = await fetch('/metrics');
                        if (!response.ok) throw new Error('Failed to fetch');
                        const data = await response.json();
                        
                        document.getElementById('connection-status').className = 'flex items-center space-x-1.5 bg-emerald-950 text-emerald-400 px-3 py-1 rounded-full border border-emerald-500/20 font-bold font-mono';
                        document.getElementById('last-updated').innerText = 'Last update: ' + new Date().toLocaleTimeString();

                        // Update Stats
                        const requests = data.requestCount || 0;
                        const created = data.domainCounters.transfersCreated || 0;
                        const declined = data.domainCounters.transfersDeclinedInsufficientFunds || 0;
                        const replays = data.domainCounters.transfersIdempotentReplay || 0;

                        document.getElementById('stat-requests').innerText = requests.toLocaleString();
                        document.getElementById('stat-created').innerText = created.toLocaleString();
                        document.getElementById('stat-declined').innerText = declined.toLocaleString();
                        document.getElementById('stat-replays').innerText = replays.toLocaleString();

                        if (requests > 0) {
                            document.getElementById('created-percent').innerText = ((created / requests) * 100).toFixed(1) + '% of total';
                            document.getElementById('declined-percent').innerText = ((declined / requests) * 100).toFixed(1) + '% of total';
                            document.getElementById('replays-percent').innerText = ((replays / requests) * 100).toFixed(1) + '% of total';
                        }

                        document.getElementById('perf-errors').innerText = (data.errorRatePercent || 0).toFixed(2) + '%';
                        document.getElementById('perf-latency').innerText = (data.latencyP99Ms || 0) + ' ms';

                        // Update Chart
                        requestsData.shift();
                        requestsData.push(requests);
                        createdData.shift();
                        createdData.push(created);

                        metricsChart.update();

                    } catch (err) {
                        document.getElementById('connection-status').className = 'flex items-center space-x-1.5 bg-rose-950 text-rose-400 px-3 py-1 rounded-full border border-rose-500/20 font-bold font-mono';
                        console.error(err);
                    }
                }

                setInterval(fetchMetrics, 2000);
                fetchMetrics();
            </script>
        </body>
        </html>
        """;
    }
}
