/**
 * ============================================================================
 * FIND MY COLLEGE - PRODUCTION BASELINE / LOAD PERFORMANCE TESTER
 * ============================================================================
 * Specifications:
 *  - 100 Virtual Users (Concurrent Workers)
 *  - 1 Minute Continuous Execution (60 Seconds Duration)
 *  - Measures Requests Per Second (RPS)
 *  - Measures Response Time (Min, Average, Median, p90, p95, p99, Max)
 *  - Detailed Latency Distribution & Status Code Analytics
 * ============================================================================
 */

const http = require('http');
const https = require('https');
const url = require('url');
const fs = require('fs');
const path = require('path');
const { performance } = require('perf_hooks');

// Parse CLI Arguments or Environment Variables
const args = process.argv.slice(2);
function getArg(flag, defaultValue) {
    const idx = args.indexOf(flag);
    if (idx !== -1 && args[idx + 1]) return args[idx + 1];
    return defaultValue;
}

const CONFIG = {
    targetUrl: getArg('--url', process.env.TARGET_URL || 'http://localhost:5500'),
    virtualUsers: parseInt(getArg('--users', process.env.CONCURRENCY || '100'), 10),
    durationSeconds: parseInt(getArg('--duration', process.env.DURATION || '60'), 10),
    timeoutMs: parseInt(getArg('--timeout', '10000'), 10),
    endpoints: [
        '/',
        '/api/health',
        '/api/colleges',
        '/api/colleges?stream=Engineering',
        '/api/colleges?stream=Management',
        '/api/colleges/search?q=Delhi',
        '/api/colleges/iit-madras',
        '/api/auth/status'
    ],
    resultsDir: path.join(__dirname, 'results')
};

if (!fs.existsSync(CONFIG.resultsDir)) {
    fs.mkdirSync(CONFIG.resultsDir, { recursive: true });
}

// Global Metrics Accumulator
const metrics = {
    startTime: 0,
    endTime: 0,
    totalRequests: 0,
    successfulRequests: 0,
    failedRequests: 0,
    totalBytes: 0,
    latencies: [],
    statusCodes: {},
    timeSeries: [] // Per-second metrics
};

// High-speed HTTP Agent with Keep-Alive connection pooling
const httpAgent = new http.Agent({
    keepAlive: true,
    maxSockets: CONFIG.virtualUsers + 20,
    maxFreeSockets: 50,
    timeout: CONFIG.timeoutMs
});

const httpsAgent = new https.Agent({
    keepAlive: true,
    maxSockets: CONFIG.virtualUsers + 20,
    maxFreeSockets: 50,
    timeout: CONFIG.timeoutMs
});

/**
 * Executes a single HTTP request and measures high-resolution latency
 */
function sendRequest(targetPath) {
    return new Promise((resolve) => {
        const fullUrl = new URL(targetPath, CONFIG.targetUrl);
        const isHttps = fullUrl.protocol === 'https:';
        const client = isHttps ? https : http;
        const agent = isHttps ? httpsAgent : httpAgent;

        const options = {
            protocol: fullUrl.protocol,
            hostname: fullUrl.hostname,
            port: fullUrl.port || (isHttps ? 443 : 80),
            path: fullUrl.pathname + fullUrl.search,
            method: 'GET',
            agent: agent,
            headers: {
                'User-Agent': 'FindMyCollege-LoadTester/1.0 (100VUs)',
                'Accept': 'application/json, text/html, */*'
            },
            timeout: CONFIG.timeoutMs
        };

        const tStart = performance.now();

        const req = client.request(options, (res) => {
            let bytesReceived = 0;
            res.on('data', (chunk) => {
                bytesReceived += chunk.length;
            });

            res.on('end', () => {
                const duration = performance.now() - tStart;
                resolve({
                    success: res.statusCode >= 200 && res.statusCode < 400,
                    statusCode: res.statusCode,
                    duration,
                    bytes: bytesReceived
                });
            });
        });

        req.on('error', (err) => {
            const duration = performance.now() - tStart;
            resolve({
                success: false,
                statusCode: err.code || 'ERR',
                duration,
                bytes: 0
            });
        });

        req.on('timeout', () => {
            req.destroy();
            const duration = performance.now() - tStart;
            resolve({
                success: false,
                statusCode: 'TIMEOUT',
                duration,
                bytes: 0
            });
        });

        req.end();
    });
}

/**
 * Worker representing one Virtual User (VU) running in an asynchronous loop
 */
async function runVirtualUser(workerId, stopSignal) {
    let endpointIndex = workerId % CONFIG.endpoints.length;

    while (!stopSignal.stopped) {
        const targetPath = CONFIG.endpoints[endpointIndex];
        endpointIndex = (endpointIndex + 1) % CONFIG.endpoints.length;

        const result = await sendRequest(targetPath);
        
        metrics.totalRequests++;
        metrics.totalBytes += result.bytes;
        metrics.latencies.push(result.duration);

        if (result.success) {
            metrics.successfulRequests++;
        } else {
            metrics.failedRequests++;
        }

        const codeKey = String(result.statusCode);
        metrics.statusCodes[codeKey] = (metrics.statusCodes[codeKey] || 0) + 1;
    }
}

/**
 * Formats milliseconds into clean string
 */
function formatMs(ms) {
    if (ms === undefined || isNaN(ms)) return '0.00ms';
    if (ms >= 1000) return `${(ms / 1000).toFixed(2)}s`;
    return `${ms.toFixed(1)}ms`;
}

/**
 * Calculates statistical percentiles from array of numbers
 */
function calculateStats(values) {
    if (!values || values.length === 0) {
        return { min: 0, max: 0, avg: 0, p50: 0, p90: 0, p95: 0, p99: 0 };
    }
    const sorted = [...values].sort((a, b) => a - b);
    const sum = sorted.reduce((acc, v) => acc + v, 0);
    const avg = sum / sorted.length;
    const min = sorted[0];
    const max = sorted[sorted.length - 1];
    
    function getPercentile(p) {
        const idx = Math.min(Math.floor((p / 100) * sorted.length), sorted.length - 1);
        return sorted[idx];
    }

    return {
        min,
        max,
        avg,
        p50: getPercentile(50),
        p90: getPercentile(90),
        p95: getPercentile(95),
        p99: getPercentile(99)
    };
}

/**
 * Master Load Test Orchestrator
 */
async function runLoadTest() {
    console.log('\n' + '='.repeat(80));
    console.log('       FIND MY COLLEGE — BASELINE LOAD PERFORMANCE TEST SUITE');
    console.log('='.repeat(80));
    console.log(`Target URL             : ${CONFIG.targetUrl}`);
    console.log(`Concurrent Users (VUs) : ${CONFIG.virtualUsers} virtual users`);
    console.log(`Test Duration          : ${CONFIG.durationSeconds} seconds (1 minute continuous run)`);
    console.log(`Target Endpoints       : ${CONFIG.endpoints.length} varied routes (Static, Directory, Search, Detail, Health)`);
    console.log('='.repeat(80) + '\n');

    // Check if target is responding
    console.log('Verifying target connectivity...');
    const healthCheck = await sendRequest('/api/health');
    if (!healthCheck.success && healthCheck.statusCode !== 404 && healthCheck.statusCode !== 200) {
        console.warn(`Target at ${CONFIG.targetUrl} not responding immediately. Spawning embedded local server...`);
        const server = require('./server.js');
        await new Promise((r) => server.listen(5500, '0.0.0.0', r));
        console.log('Embedded server started successfully on http://localhost:5500\n');
    } else {
        console.log(`Target connectivity verified! HTTP ${healthCheck.statusCode} (${healthCheck.duration.toFixed(1)}ms)\n`);
    }

    console.log(`🚀 Ramping up to ${CONFIG.virtualUsers} Virtual Users...`);
    console.log('   Running continuously for 60 seconds. Collecting telemetry:\n');

    const stopSignal = { stopped: false };
    metrics.startTime = performance.now();

    // Spawn 100 Virtual Users
    const workers = [];
    for (let i = 0; i < CONFIG.virtualUsers; i++) {
        workers.push(runVirtualUser(i, stopSignal));
    }

    let lastReqCount = 0;
    let elapsedSeconds = 0;

    // Per-second telemetry monitor
    const monitorInterval = setInterval(() => {
        elapsedSeconds++;
        const currentReqCount = metrics.totalRequests;
        const rpsInSecond = currentReqCount - lastReqCount;
        lastReqCount = currentReqCount;

        const currentLatencies = metrics.latencies.slice(-rpsInSecond);
        const secondStats = calculateStats(currentLatencies);

        metrics.timeSeries.push({
            second: elapsedSeconds,
            rps: rpsInSecond,
            avgLatency: secondStats.avg,
            minLatency: secondStats.min,
            maxLatency: secondStats.max,
            cumulativeRequests: currentReqCount
        });

        // Live CLI progress output
        const remaining = Math.max(0, CONFIG.durationSeconds - elapsedSeconds);
        const percent = Math.min(100, Math.floor((elapsedSeconds / CONFIG.durationSeconds) * 100));
        const barLength = 20;
        const filled = Math.floor((percent / 100) * barLength);
        const bar = '█'.repeat(filled) + '░'.repeat(barLength - filled);

        process.stdout.write(
            `\r[${bar}] ${percent}% | Elapsed: ${elapsedSeconds}s/${CONFIG.durationSeconds}s | ` +
            `Current RPS: \x1b[32m${rpsInSecond} req/s\x1b[0m | ` +
            `Avg: \x1b[36m${formatMs(secondStats.avg)}\x1b[0m | ` +
            `Total: \x1b[33m${currentReqCount.toLocaleString()}\x1b[0m reqs   `
        );

        if (elapsedSeconds >= CONFIG.durationSeconds) {
            clearInterval(monitorInterval);
        }
    }, 1000);

    // Run for exact duration
    await new Promise((resolve) => setTimeout(resolve, CONFIG.durationSeconds * 1000));
    stopSignal.stopped = true;
    metrics.endTime = performance.now();
    clearInterval(monitorInterval);

    // Wait for all in-flight workers to complete
    await Promise.all(workers);

    const actualDurationSeconds = (metrics.endTime - metrics.startTime) / 1000;
    const stats = calculateStats(metrics.latencies);
    const overallRps = Math.round(metrics.totalRequests / actualDurationSeconds);
    const peakRps = Math.max(...metrics.timeSeries.map(t => t.rps), overallRps);
    const totalMb = (metrics.totalBytes / (1024 * 1024)).toFixed(2);
    const throughputMbSec = (metrics.totalBytes / (1024 * 1024 * actualDurationSeconds)).toFixed(2);
    const successRate = ((metrics.successfulRequests / metrics.totalRequests) * 100).toFixed(2);

    console.log('\n\n' + '='.repeat(80));
    console.log('                 BASELINE LOAD TEST RESULTS & METRICS');
    console.log('='.repeat(80));

    console.log('\n📊 THROUGHPUT & REQUESTS PER SECOND (RPS):');
    console.log(`   • Average Throughput  : \x1b[32m\x1b[1m${overallRps.toLocaleString()} req/sec\x1b[0m`);
    console.log(`     (Meaning your API is handling about ${overallRps.toLocaleString()} requests every second)`);
    console.log(`   • Peak Throughput     : ${peakRps.toLocaleString()} req/sec`);
    console.log(`   • Total Requests Sent : ${metrics.totalRequests.toLocaleString()} requests in ${actualDurationSeconds.toFixed(1)}s`);
    console.log(`   • Data Transferred    : ${totalMb} MB (${throughputMbSec} MB/s)`);

    console.log('\n⏱️ RESPONSE TIME (LATENCY):');
    console.log(`   • Average Response    : \x1b[36m\x1b[1m${formatMs(stats.avg)}\x1b[0m`);
    console.log(`   • Minimum (Fastest)   : \x1b[32m${formatMs(stats.min)}\x1b[0m`);
    console.log(`   • Maximum (Slowest)   : \x1b[31m${formatMs(stats.max)}\x1b[0m`);
    console.log(`   • 50th Percentile (p50): ${formatMs(stats.p50)} (Median user experience)`);
    console.log(`   • 90th Percentile (p90): ${formatMs(stats.p90)}`);
    console.log(`   • 95th Percentile (p95): ${formatMs(stats.p95)}`);
    console.log(`   • 99th Percentile (p99): ${formatMs(stats.p99)}`);

    console.log('\n📈 SUCCESS & STATUS CODES:');
    console.log(`   • Success Rate        : \x1b[32m${successRate}%\x1b[0m (${metrics.successfulRequests.toLocaleString()} successful)`);
    console.log(`   • Failed Requests     : ${metrics.failedRequests.toLocaleString()}`);
    console.log('   • HTTP Status Breakdown:');
    for (const [code, count] of Object.entries(metrics.statusCodes)) {
        console.log(`       HTTP ${code.padEnd(8)} : ${count.toLocaleString()} requests`);
    }

    console.log('\n⚖️ PERFORMANCE EVALUATION AGAINST TARGET SLAs:');
    const slaLatencyPass = stats.avg < 300;
    const slaSuccessPass = parseFloat(successRate) >= 99.0;
    const slaRpsPass = overallRps >= 100;

    console.log(`   [${slaLatencyPass ? 'PASS ✅' : 'FAIL ❌'}] Average Latency < 300ms (${formatMs(stats.avg)})`);
    console.log(`   [${slaSuccessPass ? 'PASS ✅' : 'FAIL ❌'}] Success Rate >= 99.0% (${successRate}%)`);
    console.log(`   [${slaRpsPass ? 'PASS ✅' : 'FAIL ❌'}] Throughput >= 100 req/sec (${overallRps} req/sec)`);
    console.log('='.repeat(80) + '\n');

    // Save JSON results file
    const resultsPayload = {
        config: CONFIG,
        summary: {
            actualDurationSeconds,
            totalRequests: metrics.totalRequests,
            successfulRequests: metrics.successfulRequests,
            failedRequests: metrics.failedRequests,
            successRatePercent: parseFloat(successRate),
            overallRps,
            peakRps,
            totalDataMb: parseFloat(totalMb),
            throughputMbSec: parseFloat(throughputMbSec)
        },
        latency: stats,
        statusCodes: metrics.statusCodes,
        timeSeries: metrics.timeSeries
    };

    const jsonPath = path.join(CONFIG.resultsDir, 'load_test_results.json');
    fs.writeFileSync(jsonPath, JSON.stringify(resultsPayload, null, 2));
    console.log(`📄 Saved raw JSON benchmark results to: ${jsonPath}`);

    return resultsPayload;
}

if (require.main === module) {
    runLoadTest()
        .then(() => process.exit(0))
        .catch((err) => {
            console.error('Fatal load test error:', err);
            process.exit(1);
        });
}

module.exports = { runLoadTest, CONFIG };
