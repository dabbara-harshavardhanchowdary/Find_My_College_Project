/**
 * ============================================================================
 * FIND MY COLLEGE - HIGH-PERFORMANCE BACKEND API & STATIC SERVER
 * Purpose: Dedicated local server for Baseline & Load Performance Testing
 * Technology: Native Node.js HTTP (Zero external dependencies)
 * ============================================================================
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');

const PORT = process.env.PORT || 5500;
const WEB_APP_DIR = path.join(__dirname, '..', 'Web_Application');

// Sample college dataset for realistic API responses
const SAMPLE_COLLEGES = [
    { id: "iit-madras", name: "Indian Institute of Technology Madras", rank: 1, score: 90.04, stream: "Engineering", city: "Chennai", state: "Tamil Nadu", fees: "₹8.5 Lakhs", avgPlacement: "₹21.4 LPA", tlr: 95.2, rpc: 96.1, go: 85.3, oi: 76.5, perception: 100.0 },
    { id: "iit-delhi", name: "Indian Institute of Technology Delhi", rank: 2, score: 88.12, stream: "Engineering", city: "New Delhi", state: "Delhi", fees: "₹8.8 Lakhs", avgPlacement: "₹20.8 LPA", tlr: 93.1, rpc: 94.5, go: 84.1, oi: 74.2, perception: 98.5 },
    { id: "iit-bombay", name: "Indian Institute of Technology Bombay", rank: 3, score: 87.40, stream: "Engineering", city: "Mumbai", state: "Maharashtra", fees: "₹9.1 Lakhs", avgPlacement: "₹21.8 LPA", tlr: 92.4, rpc: 93.8, go: 83.7, oi: 75.1, perception: 99.0 },
    { id: "iit-kanpur", name: "Indian Institute of Technology Kanpur", rank: 4, score: 86.85, stream: "Engineering", city: "Kanpur", state: "Uttar Pradesh", fees: "₹8.4 Lakhs", avgPlacement: "₹19.5 LPA", tlr: 90.5, rpc: 91.2, go: 82.0, oi: 73.0, perception: 95.0 },
    { id: "iit-kharagpur", name: "Indian Institute of Technology Kharagpur", rank: 5, score: 85.90, stream: "Engineering", city: "Kharagpur", state: "West Bengal", fees: "₹8.6 Lakhs", avgPlacement: "₹18.9 LPA", tlr: 89.2, rpc: 90.1, go: 81.5, oi: 72.8, perception: 94.2 },
    { id: "iim-ahmedabad", name: "Indian Institute of Management Ahmedabad", rank: 1, score: 83.20, stream: "Management", city: "Ahmedabad", state: "Gujarat", fees: "₹24.0 Lakhs", avgPlacement: "₹34.3 LPA", tlr: 91.0, rpc: 89.5, go: 88.0, oi: 71.0, perception: 100.0 },
    { id: "iim-bangalore", name: "Indian Institute of Management Bangalore", rank: 2, score: 81.50, stream: "Management", city: "Bengaluru", state: "Karnataka", fees: "₹23.5 Lakhs", avgPlacement: "₹33.8 LPA", tlr: 89.5, rpc: 88.0, go: 86.5, oi: 70.2, perception: 98.0 },
    { id: "nlsiu-bengaluru", name: "National Law School of India University", rank: 1, score: 81.25, stream: "Law", city: "Bengaluru", state: "Karnataka", fees: "₹3.8 Lakhs", avgPlacement: "₹16.0 LPA", tlr: 88.0, rpc: 84.5, go: 85.0, oi: 78.0, perception: 99.0 },
    { id: "saveetha-dental", name: "Saveetha Institute of Medical and Technical Sciences", rank: 1, score: 84.50, stream: "Dental", city: "Chennai", state: "Tamil Nadu", fees: "₹4.5 Lakhs", avgPlacement: "₹10.5 LPA", tlr: 90.0, rpc: 91.0, go: 80.0, oi: 81.0, perception: 88.0 }
];

const MIME_TYPES = {
    '.html': 'text/html; charset=utf-8',
    '.js': 'application/javascript; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon'
};

const server = http.createServer((req, res) => {
    const parsedUrl = url.parse(req.url, true);
    const pathname = parsedUrl.pathname;

    // CORS & Performance Headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    res.setHeader('Server', 'FindMyCollege-BenchmarkEngine/1.0');

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        return res.end();
    }

    // --- API ENDPOINTS ---
    if (pathname === '/api/health') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ status: 'ok', uptime: process.uptime(), timestamp: Date.now() }));
    }

    if (pathname === '/api/colleges') {
        const streamFilter = (parsedUrl.query.stream || 'All').toLowerCase();
        let results = SAMPLE_COLLEGES;
        if (streamFilter !== 'all') {
            results = SAMPLE_COLLEGES.filter(c => c.stream.toLowerCase() === streamFilter);
        }
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ total: results.length, data: results }));
    }

    if (pathname.startsWith('/api/colleges/search')) {
        const query = (parsedUrl.query.q || '').toLowerCase();
        const results = SAMPLE_COLLEGES.filter(c => 
            c.name.toLowerCase().includes(query) || 
            c.city.toLowerCase().includes(query) || 
            c.state.toLowerCase().includes(query)
        );
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ query, count: results.length, data: results }));
    }

    if (pathname.startsWith('/api/colleges/')) {
        const collegeId = pathname.replace('/api/colleges/', '').trim();
        const college = SAMPLE_COLLEGES.find(c => c.id === collegeId) || SAMPLE_COLLEGES[0];
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ success: true, data: college }));
    }

    if (pathname === '/api/auth/status') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ authenticated: false, role: 'guest', sessionDuration: 3600 }));
    }

    // --- STATIC FILES (Web_Application) ---
    let filePath = path.join(WEB_APP_DIR, pathname === '/' ? 'index.html' : pathname);
    
    fs.stat(filePath, (err, stats) => {
        if (err || !stats.isFile()) {
            // Default fallback to index.html for SPA routing
            filePath = path.join(WEB_APP_DIR, 'index.html');
        }

        const ext = path.extname(filePath).toLowerCase();
        const contentType = MIME_TYPES[ext] || 'text/plain';

        fs.readFile(filePath, (readErr, content) => {
            if (readErr) {
                res.writeHead(404, { 'Content-Type': 'text/plain' });
                return res.end('404 Not Found');
            }
            res.writeHead(200, { 'Content-Type': contentType });
            res.end(content);
        });
    });
});

if (require.main === module) {
    server.listen(PORT, '0.0.0.0', () => {
        console.log(`🚀 Find My College Server running at http://localhost:${PORT}`);
        console.log(`   - Static Frontend: http://localhost:${PORT}/`);
        console.log(`   - API Health:     http://localhost:${PORT}/api/health`);
        console.log(`   - API Colleges:   http://localhost:${PORT}/api/colleges`);
    });
}

module.exports = server;
