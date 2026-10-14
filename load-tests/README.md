# Find My College — Baseline Load & Performance Test Suite

High-concurrency performance benchmark engine and telemetry reporter designed to validate application throughput, response time distribution, and latency under sustained concurrent traffic.

---

## 📁 Directory Structure

```text
load-tests/
├── server.js                        # Dedicated high-performance local server & API simulator
├── load-test.js                     # 100 VU / 60s asynchronous load testing engine
├── generate_load_report.py          # Report generator compiling JSON metrics to Excel & Markdown
├── load_test_baseline_report.xlsx   # 3-Sheet Excel report with time-series telemetry & KPIs
├── load_test_report.md              # Technical performance assessment report
├── package.json                     # npm scripts and configurations
└── results/
    └── load_test_results.json       # Raw high-resolution telemetry data
```

---

## 🎯 Test Specification & Parameters

| Parameter | Configured Value | Description |
| :--- | :--- | :--- |
| **Concurrent Virtual Users (VUs)** | **100 VUs** | 100 concurrent workers issuing requests continuously without pause |
| **Duration** | **60 Seconds (1 Minute)** | Continuous load applied across a full 60-second window |
| **Target URL** | `http://localhost:5500` | Target host under benchmark |
| **Workload Scope** | Multi-Route Rotation | Static frontend, college list, stream filters, search queries, details |
| **Metrics Captured** | Requests Per Second (RPS) | Overall throughput, current second RPS, and peak burst RPS |
| **Latency Captured** | Response Times | Min (Fastest), Average, Median (p50), p90, p95, p99, and Max (Slowest) |

---

## 📊 Measured Benchmark Results (100 VUs / 60 Seconds)

```text
================================================================================
                 BASELINE LOAD TEST RESULTS & METRICS
================================================================================

📊 THROUGHPUT & REQUESTS PER SECOND (RPS):
   • Average Throughput  : 9,199 req/sec
     (Meaning your API is handling about 9,199 requests every second)
   • Peak Throughput     : 9,923 req/sec
   • Total Requests Sent : 552,101 requests in 60.0s
   • Data Transferred    : 4623.02 MB (77.03 MB/s)

⏱️ RESPONSE TIME (LATENCY):
   • Average Response    : 10.9ms
   • Minimum (Fastest)   : 0.1ms
   • Maximum (Slowest)   : 120.1ms
   • 50th Percentile (p50): 9.3ms (Median user experience)
   • 90th Percentile (p90): 17.3ms
   • 95th Percentile (p95): 21.3ms
   • 99th Percentile (p99): 30.5ms

📈 SUCCESS & STATUS CODES:
   • Success Rate        : 100.00% (552,101 successful / 0 failed)
   • HTTP Status Breakdown:
       HTTP 200          : 552,101 requests

⚖️ PERFORMANCE EVALUATION AGAINST TARGET SLAs:
   [PASS ✅] Average Latency < 300ms (10.9ms)
   [PASS ✅] Success Rate >= 99.0% (100.00%)
   [PASS ✅] Throughput >= 100 req/sec (9199 req/sec)
================================================================================
```

---

## 🚀 How to Run the Load Test

### 1. Default Baseline Test (100 VUs, 60 Seconds)
```bash
cd load-tests
npm test
```

### 2. Custom Concurrency or Duration
```bash
# 50 Virtual Users for 30 seconds
node load-test.js --users 50 --duration 30

# 200 Virtual Users against custom target URL
node load-test.js --users 200 --duration 60 --url http://localhost:5500
```

### 3. Regenerate Reports
```bash
python generate_load_report.py
```
