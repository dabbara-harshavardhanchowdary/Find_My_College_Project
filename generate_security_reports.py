import os
import openpyxl
from openpyxl.styles import Font, Alignment, PatternFill, Border, Side
from openpyxl.utils import get_column_letter

def generate_security_artifacts():
    out_dir = os.path.abspath("Vulnerability Test Results")
    os.makedirs(out_dir, exist_ok=True)
    
    font_family = "Calibri"
    
    # Styles
    title_font = Font(name=font_family, size=16, bold=True, color="0F172A")
    subtitle_font = Font(name=font_family, size=10, italic=True, color="64748B")
    section_font = Font(name=font_family, size=12, bold=True, color="1E293B")
    header_font = Font(name=font_family, size=10, bold=True, color="FFFFFF")
    data_font = Font(name=font_family, size=9, color="1E293B")
    data_font_bold = Font(name=font_family, size=9, bold=True, color="1E293B")
    code_font = Font(name="Consolas", size=9, color="0F172A")

    header_fill = PatternFill(start_color="0F172A", end_color="0F172A", fill_type="solid")
    sub_header_fill = PatternFill(start_color="1E293B", end_color="1E293B", fill_type="solid")
    zebra_even = PatternFill(start_color="F8FAFC", end_color="F8FAFC", fill_type="solid")
    zebra_odd = PatternFill(start_color="FFFFFF", end_color="FFFFFF", fill_type="solid")

    crit_fill = PatternFill(start_color="FEE2E2", end_color="FEE2E2", fill_type="solid")
    crit_font = Font(name=font_family, size=9, bold=True, color="991B1B")

    high_fill = PatternFill(start_color="FFEDD5", end_color="FFEDD5", fill_type="solid")
    high_font = Font(name=font_family, size=9, bold=True, color="9A3412")

    med_fill = PatternFill(start_color="FEF3C7", end_color="FEF3C7", fill_type="solid")
    med_font = Font(name=font_family, size=9, bold=True, color="92400E")

    low_fill = PatternFill(start_color="DBEAFE", end_color="DBEAFE", fill_type="solid")
    low_font = Font(name=font_family, size=9, bold=True, color="1E40AF")

    thin_border_side = Side(style='thin', color='CBD5E1')
    data_border = Border(left=thin_border_side, right=thin_border_side, top=thin_border_side, bottom=thin_border_side)

    align_center = Alignment(horizontal="center", vertical="center", wrap_text=True)
    align_left = Alignment(horizontal="left", vertical="center", wrap_text=True)

    # =========================================================================
    # 1. SECURITY FINDINGS DATASET
    # =========================================================================
    findings = [
        {
            "id": "SEC-VULN-001",
            "title": "Hardcoded Google Cloud Service Account Private Key & IAM Credentials",
            "severity": "Critical",
            "cvss": "9.8",
            "cwe": "CWE-798: Use of Hardcoded Credentials",
            "owasp": "A07:2021-Identification and Authentication Failures",
            "file": "serviceAccountKey.json, Backend-api/serviceAccountKey.json",
            "endpoint": "Backend Ingestion Pipeline / Firebase Admin SDK",
            "description": "A full 2048-bit RSA private key and service account credentials for 'firebase-adminsdk-fbsvc@find-my-college-222.iam.gserviceaccount.com' are committed in plaintext directly to the repository.",
            "impact": "Full administrative compromise of the Google Cloud Project and Firebase backend. An attacker can read, modify, or permanently destroy all database collections, impersonate service accounts, and exhaust project cloud budgets.",
            "remediation": "1. Immediately revoke this service account key in Google Cloud IAM Console.\n2. Add 'serviceAccountKey.json' to .gitignore.\n3. Utilize Google Cloud Secret Manager or environment variables (GOOGLE_APPLICATION_CREDENTIALS) injected via CI/CD secrets."
        },
        {
            "id": "SEC-VULN-002",
            "title": "Missing Server-Side Authorization & Direct Client-Side Firestore Access",
            "severity": "Critical",
            "cvss": "9.1",
            "cwe": "CWE-285: Improper Authorization / CWE-602: Client-Side Security",
            "owasp": "A01:2021-Broken Access Control",
            "file": "Web_Application/src/config/firebase-config.js, Mobile_Application/app/src/main/java/com/findmycollege/app/ui/screens/ProfileScreen.kt",
            "endpoint": "Cloud Firestore Collections (users, engineering_colleges, etc.)",
            "description": "Frontend web and mobile apps connect directly to Cloud Firestore without an intermediary API gateway enforcing authorization. The repository contains no 'firestore.rules' definition, leaving collections vulnerable to default permissive rule sets.",
            "impact": "Any unauthenticated user or guest can query, write, overwrite, or delete documents across all Firestore collections if rules are in test mode ('allow read, write: if true;').",
            "remediation": "1. Deploy strict, declarative Firestore Security Rules ('firestore.rules') verifying 'request.auth != null && request.auth.uid == userId'.\n2. Disallow all client writes to public college ranking collections.\n3. Implement a backend proxy for sensitive administrative tasks."
        },
        {
            "id": "SEC-VULN-003",
            "title": "Insecure Direct Object Reference (IDOR) on User Profile Documents",
            "severity": "High",
            "cvss": "8.5",
            "cwe": "CWE-639: Authorization Bypass Through User-Controlled Key",
            "owasp": "A01:2021-Broken Access Control",
            "file": "Mobile_Application/app/src/main/java/com/findmycollege/app/ui/screens/ProfileScreen.kt:716",
            "endpoint": "firestore.collection('users').document(uid).delete() / get()",
            "description": "User profile operations directly take a 'uid' string and execute delete/update without enforcing in the security layer that 'request.auth.uid == uid'.",
            "impact": "Malicious authenticated users can delete or tamper with arbitrary student profiles, alter bookmarks, and corrupt peer account data by submitting target UIDs.",
            "remediation": "Enforce document path binding in security rules: 'match /users/{userId} { allow read, write, delete: if request.auth != null && request.auth.uid == userId; }'."
        },
        {
            "id": "SEC-VULN-004",
            "title": "Client-Side Exposure of Gemini AI API Key in URL Query Parameter",
            "severity": "High",
            "cvss": "7.5",
            "cwe": "CWE-598: Use of GET Request with Sensitive Query String",
            "owasp": "A02:2021-Cryptographic Failures / A05:2021-Security Misconfiguration",
            "file": "Web_Application/src/services/aiChatService.js:38",
            "endpoint": "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}",
            "description": "The Gemini AI API key is read from window.GEMINI_API_KEY and appended as a plain query parameter in direct browser fetch requests.",
            "impact": "API keys sent in query parameters are logged in plaintext in HTTP access logs, browser history, proxy caches, and Referer headers, leading to key leakage and quota hijacking.",
            "remediation": "Proxy AI inference through a secure backend API endpoint (e.g., Firebase Cloud Functions or Node backend) where the API key is stored strictly server-side."
        },
        {
            "id": "SEC-VULN-005",
            "title": "Unvalidated Web Scraping & Ingestion Pipeline Vulnerable to SSRF / Data Corruption",
            "severity": "High",
            "cvss": "7.4",
            "cwe": "CWE-20: Improper Input Validation / CWE-918: SSRF",
            "owasp": "A10:2021-Server-Side Request Forgery / A08:2021-Software and Data Integrity",
            "file": "scrape_and_push_nirf.py:36, Backend-api/seed_engineering.py:29",
            "endpoint": "External scraping pipeline -> Firestore Batch Write",
            "description": "The scraping engine fetches arbitrary external HTML tables and inserts parsed text directly into Firestore batches without schema validation, content-type verification, or sanitize/escape routines.",
            "impact": "If the upstream portal is intercepted (DNS poisoning / MITM) or returns malicious payload text, malicious content will be permanently seeded into the production database.",
            "remediation": "1. Validate response schema strictly against a Pydantic / Zod model.\n2. Pin HTTPS certificates and verify TLS integrity.\n3. Sanitize all parsed text before persisting to database collections."
        },
        {
            "id": "SEC-VULN-006",
            "title": "Missing Content Security Policy (CSP) & Standard Security Headers",
            "severity": "Medium",
            "cvss": "5.3",
            "cwe": "CWE-1021: Improper Restriction of Rendered UI Layers / CWE-693",
            "owasp": "A05:2021-Security Misconfiguration",
            "file": "Web_Application/index.html",
            "endpoint": "Web Frontend HTTP Response",
            "description": "The web application does not define a Content-Security-Policy (CSP) meta tag or HTTP response headers (X-Frame-Options, X-Content-Type-Options, Referrer-Policy, Strict-Transport-Security).",
            "impact": "Enables Clickjacking attacks via malicious iframe embedding and elevates risks of Cross-Site Scripting (XSS) from third-party scripts.",
            "remediation": "Configure HTTP security headers on web server or hosting platform (e.g. Firebase Hosting headers config in firebase.json):\n- Content-Security-Policy: default-src 'self' ...\n- X-Frame-Options: DENY\n- X-Content-Type-Options: nosniff"
        },
        {
            "id": "SEC-VULN-007",
            "title": "Stored Cross-Site Scripting (XSS) Risk in Profile Name and Bio Rendering",
            "severity": "Medium",
            "cvss": "6.1",
            "cwe": "CWE-79: Improper Neutralization of Input During Web Page Generation",
            "owasp": "A03:2021-Injection",
            "file": "Web_Application/src/components/modals/AuthModal.js:68, ProfileDrawer.js",
            "endpoint": "document.getElementById('authAlert').innerHTML = msg",
            "description": "User inputs and error messages are written directly to DOM elements using innerHTML rather than textContent, creating injection avenues for crafted payloads.",
            "impact": "Execution of arbitrary JavaScript in the victim's browser session, allowing session hijacking, token theft, or unauthorized account actions.",
            "remediation": "Use 'textContent' or DOMPurify.sanitize() before assigning HTML to prevent script execution."
        },
        {
            "id": "SEC-VULN-008",
            "title": "Insecure Client-Side Storage of Guest Bypass & State Flags",
            "severity": "Low",
            "cvss": "3.8",
            "cwe": "CWE-922: Insecure Storage of Sensitive Information",
            "owasp": "A04:2021-Insecure Design",
            "file": "Web_Application/src/components/modals/AuthModal.js:320",
            "endpoint": "sessionStorage.setItem('isGuest', 'true')",
            "description": "Guest bypass authorization state is stored entirely in client-side sessionStorage without cryptographic signing or backend verification.",
            "impact": "Users can trivially manipulate storage in DevTools to spoof guest states or bypass client restrictions.",
            "remediation": "Treat client storage as untrusted; ensure all restricted actions validate backend authentication tokens."
        },
        {
            "id": "SEC-VULN-009",
            "title": "Unpinned and Unmanaged Dependencies in Backend Seeding Scripts",
            "severity": "Low",
            "cvss": "3.3",
            "cwe": "CWE-1104: Use of Unmaintained / Unpinned Third-Party Components",
            "owasp": "A06:2021-Vulnerable and Outdated Components",
            "file": "Backend-api/package.json, scrape_and_push_nirf.py",
            "endpoint": "Build & Deployment Pipeline",
            "description": "Backend python scripts lack a requirements.txt with pinned hashes or lockfiles, and Backend-api/package.json uses caret semver (^10.8.0) without a package-lock.json.",
            "impact": "Risk of upstream supply-chain attacks, broken builds, or pulling vulnerable transitive packages during deployment.",
            "remediation": "Generate pinned requirements.txt with hashes using pip-compile/pipenv, and commit package-lock.json."
        }
    ]

    # =========================================================================
    # 2. ENDPOINT INVENTORY DATASET
    # =========================================================================
    endpoints = [
        {"endpoint": "/users/{userId}", "method": "GET", "auth": "Required (Firebase Auth)", "roles": "Owner (userId)", "file": "Mobile_Application/app/.../ProfileScreen.kt", "desc": "Retrieve student user profile & preferences"},
        {"endpoint": "/users/{userId}", "method": "POST/SET", "auth": "Required (Firebase Auth)", "roles": "Owner (userId)", "file": "Mobile_Application/app/.../AuthViewModel.kt", "desc": "Create or initialize student user record"},
        {"endpoint": "/users/{userId}", "method": "PATCH/UPDATE", "auth": "Required (Firebase Auth)", "roles": "Owner (userId)", "file": "Mobile_Application/app/.../ProfileScreen.kt", "desc": "Update student bio, phone number, and program"},
        {"endpoint": "/users/{userId}", "method": "DELETE", "auth": "Required (Firebase Auth)", "roles": "Owner (userId)", "file": "Mobile_Application/app/.../ProfileScreen.kt", "desc": "Permanently delete student account and profile data"},
        {"endpoint": "/users/{userId}/bookmarks", "method": "GET", "auth": "Required (Firebase Auth)", "roles": "Owner (userId)", "file": "Mobile_Application/app/.../CollegeViewModel.kt", "desc": "Fetch user bookmarked college IDs list"},
        {"endpoint": "/users/{userId}/bookmarks", "method": "PUT/MERGE", "auth": "Required (Firebase Auth)", "roles": "Owner (userId)", "file": "Mobile_Application/app/.../CollegeRepository.kt", "desc": "Add or remove college ID from user shortlist"},
        {"endpoint": "/engineering_colleges", "method": "GET", "auth": "Public / Optional", "roles": "Public / Guest", "file": "Web_Application/app.js:47", "desc": "Query engineering college directory & rankings"},
        {"endpoint": "/engineering_colleges/{id}", "method": "GET", "auth": "Public / Optional", "roles": "Public / Guest", "file": "Web_Application/src/components/modals/CollegeDetailModal.js", "desc": "Retrieve detailed NIRF metrics for engineering college"},
        {"endpoint": "/management_colleges", "method": "GET", "auth": "Public / Optional", "roles": "Public / Guest", "file": "Web_Application/app.js:47", "desc": "Query management college directory & rankings"},
        {"endpoint": "/management_colleges/{id}", "method": "GET", "auth": "Public / Optional", "roles": "Public / Guest", "file": "Web_Application/src/components/modals/CollegeDetailModal.js", "desc": "Retrieve detailed NIRF metrics for management institute"},
        {"endpoint": "/law_colleges", "method": "GET", "auth": "Public / Optional", "roles": "Public / Guest", "file": "Web_Application/app.js:47", "desc": "Query law college directory & rankings"},
        {"endpoint": "/law_colleges/{id}", "method": "GET", "auth": "Public / Optional", "roles": "Public / Guest", "file": "Web_Application/src/components/modals/CollegeDetailModal.js", "desc": "Retrieve detailed NIRF metrics for law college"},
        {"endpoint": "/dental_colleges", "method": "GET", "auth": "Public / Optional", "roles": "Public / Guest", "file": "Web_Application/app.js:47", "desc": "Query dental college directory & rankings"},
        {"endpoint": "/dental_colleges/{id}", "method": "GET", "auth": "Public / Optional", "roles": "Public / Guest", "file": "Web_Application/src/components/modals/CollegeDetailModal.js", "desc": "Retrieve detailed NIRF metrics for dental institute"},
        {"endpoint": "/auth/signInWithEmailAndPassword", "method": "POST", "auth": "Public", "roles": "Public / Candidate", "file": "Web_Application/src/services/authService.js:45", "desc": "Authenticate registered student with email & password"},
        {"endpoint": "/auth/createUserWithEmailAndPassword", "method": "POST", "auth": "Public", "roles": "Public / Candidate", "file": "Web_Application/src/services/authService.js:20", "desc": "Register new student account with Firebase Identity"},
        {"endpoint": "/auth/sendEmailVerification", "method": "POST", "auth": "Required (Firebase User)", "roles": "Unverified Student", "file": "Web_Application/src/services/authService.js:77", "desc": "Dispatch official email activation link to student"},
        {"endpoint": "/auth/sendPasswordResetEmail", "method": "POST", "auth": "Public", "roles": "Public / Candidate", "file": "Web_Application/src/services/authService.js:82", "desc": "Dispatch password reset link to registered email"},
        {"endpoint": "/auth/signInWithPopup(Google)", "method": "POST", "auth": "Public", "roles": "Public / Google User", "file": "Web_Application/src/services/authService.js:87", "desc": "OAuth 2.0 social authentication with Google identity"},
        {"endpoint": "/gemini/generateContent", "method": "POST", "auth": "API Key Query Param", "roles": "Public / Browser Client", "file": "Web_Application/src/services/aiChatService.js:38", "desc": "Generate college admission advice via Gemini 1.5 Flash"}
    ]

    # =========================================================================
    # 3. DEPENDENCY VULNERABILITIES DATASET
    # =========================================================================
    dependencies = [
        {"pkg": "firebase", "version": "^10.8.0", "ecosystem": "npm", "location": "Backend-api/package.json", "status": "Outdated (Current: 10.12+)", "cve": "CVE-2024-21500 (transitive)", "severity": "Medium", "remedy": "Upgrade to firebase@latest and commit package-lock.json"},
        {"pkg": "beautifulsoup4", "version": "unpinned", "ecosystem": "PyPI", "location": "scrape_and_push_nirf.py", "status": "Unpinned", "cve": "Supply-Chain Risk", "severity": "Low", "remedy": "Add requirements.txt with beautifulsoup4==4.12.3"},
        {"pkg": "requests", "version": "unpinned", "ecosystem": "PyPI", "location": "Backend-api/seed_engineering.py", "status": "Unpinned", "cve": "CVE-2023-32681 (if <2.31.0)", "severity": "Medium", "remedy": "Pin requests>=2.31.0 in requirements.txt"},
        {"pkg": "firebase-admin", "version": "unpinned", "ecosystem": "PyPI", "location": "Backend-api/seed_law.py", "status": "Unpinned", "cve": "Supply-Chain Risk", "severity": "Low", "remedy": "Pin firebase-admin>=6.4.0 in requirements.txt"},
        {"pkg": "androidx.compose:compose-bom", "version": "2024.02.00", "ecosystem": "Maven", "location": "Mobile_Application/app/build.gradle.kts", "status": "Maintained", "cve": "None", "severity": "Informational", "remedy": "Regularly update BOM versions"},
        {"pkg": "com.google.firebase:firebase-firestore-ktx", "version": "24.10.2", "ecosystem": "Maven", "location": "Mobile_Application/app/build.gradle.kts", "status": "Deprecated artifact notation", "cve": "Deprecation Notice", "severity": "Low", "remedy": "Migrate from -ktx artifact to primary Firebase BoM notation"}
    ]

    # =========================================================================
    # BUILD WORKBOOK: findings.xlsx
    # =========================================================================
    wb_findings = openpyxl.Workbook()
    
    # --- Sheet 1: Security Findings ---
    ws_f = wb_findings.active
    ws_f.title = "Security Findings"
    ws_f.views.sheetView[0].showGridLines = True
    ws_f.freeze_panes = "A2"

    f_headers = [
        ("Finding ID", 14),
        ("Vulnerability Title", 38),
        ("Severity", 12),
        ("CVSS Score", 12),
        ("CWE Classification", 28),
        ("OWASP Top 10", 25),
        ("Affected File / Component", 32),
        ("Affected Endpoint", 28),
        ("Vulnerability Description", 45),
        ("Exploitation Scenario & Impact", 45),
        ("Recommended Remediation", 45)
    ]
    for col_idx, (h_name, width) in enumerate(f_headers, start=1):
        c_let = get_column_letter(col_idx)
        cell = ws_f[f"{c_let}1"]
        cell.value = h_name
        cell.font = header_font
        cell.fill = header_fill
        cell.border = data_border
        cell.alignment = align_center if col_idx in [1, 3, 4] else align_left
        ws_f.column_dimensions[c_let].width = width
    ws_f.row_dimensions[1].height = 26

    for r_idx, f in enumerate(findings, start=2):
        row_fill = zebra_even if r_idx % 2 == 0 else zebra_odd
        vals = [
            f["id"], f["title"], f["severity"], f["cvss"], f["cwe"], f["owasp"],
            f["file"], f["endpoint"], f["description"], f["impact"], f["remediation"]
        ]
        for c_idx, val in enumerate(vals, start=1):
            c_let = get_column_letter(c_idx)
            cell = ws_f[f"{c_let}{r_idx}"]
            cell.value = val
            cell.font = data_font_bold if c_idx == 1 else data_font
            cell.fill = row_fill
            cell.border = data_border
            cell.alignment = align_center if c_idx in [1, 3, 4] else align_left
            
            # Highlight Severity
            if c_idx == 3:
                if val == "Critical":
                    cell.fill, cell.font = crit_fill, crit_font
                elif val == "High":
                    cell.fill, cell.font = high_fill, high_font
                elif val == "Medium":
                    cell.fill, cell.font = med_fill, med_font
                elif val == "Low":
                    cell.fill, cell.font = low_fill, low_font
        ws_f.row_dimensions[r_idx].height = 36

    # --- Sheet 2: Endpoint Inventory ---
    ws_e = wb_findings.create_sheet(title="Endpoint Inventory")
    ws_e.views.sheetView[0].showGridLines = True
    ws_e.freeze_panes = "A2"

    e_headers = [
        ("Endpoint Route / Path", 32),
        ("HTTP Method", 14),
        ("Authentication Required", 24),
        ("Expected Roles / Permissions", 24),
        ("Controller / Source File Path", 35),
        ("Endpoint Description & Purpose", 45)
    ]
    for col_idx, (h_name, width) in enumerate(e_headers, start=1):
        c_let = get_column_letter(col_idx)
        cell = ws_e[f"{c_let}1"]
        cell.value = h_name
        cell.font = header_font
        cell.fill = header_fill
        cell.border = data_border
        cell.alignment = align_center if col_idx == 2 else align_left
        ws_e.column_dimensions[c_let].width = width
    ws_e.row_dimensions[1].height = 26

    for r_idx, ep in enumerate(endpoints, start=2):
        row_fill = zebra_even if r_idx % 2 == 0 else zebra_odd
        vals = [ep["endpoint"], ep["method"], ep["auth"], ep["roles"], ep["file"], ep["desc"]]
        for c_idx, val in enumerate(vals, start=1):
            c_let = get_column_letter(c_idx)
            cell = ws_e[f"{c_let}{r_idx}"]
            cell.value = val
            cell.font = code_font if c_idx in [1, 5] else data_font_bold if c_idx == 2 else data_font
            cell.fill = row_fill
            cell.border = data_border
            cell.alignment = align_center if c_idx == 2 else align_left
        ws_e.row_dimensions[r_idx].height = 24

    # --- Sheet 3: Dependency Vulnerabilities ---
    ws_d = wb_findings.create_sheet(title="Dependency Vulnerabilities")
    ws_d.views.sheetView[0].showGridLines = True
    ws_d.freeze_panes = "A2"

    d_headers = [
        ("Package Name", 24),
        ("Installed Version", 18),
        ("Ecosystem", 14),
        ("Manifest File Path", 32),
        ("Package Status", 25),
        ("CVE / Advisory Identifier", 25),
        ("Severity", 14),
        ("Remediation Action", 40)
    ]
    for col_idx, (h_name, width) in enumerate(d_headers, start=1):
        c_let = get_column_letter(col_idx)
        cell = ws_d[f"{c_let}1"]
        cell.value = h_name
        cell.font = header_font
        cell.fill = header_fill
        cell.border = data_border
        cell.alignment = align_center if col_idx in [3, 7] else align_left
        ws_d.column_dimensions[c_let].width = width
    ws_d.row_dimensions[1].height = 26

    for r_idx, dep in enumerate(dependencies, start=2):
        row_fill = zebra_even if r_idx % 2 == 0 else zebra_odd
        vals = [dep["pkg"], dep["version"], dep["ecosystem"], dep["location"], dep["status"], dep["cve"], dep["severity"], dep["remedy"]]
        for c_idx, val in enumerate(vals, start=1):
            c_let = get_column_letter(c_idx)
            cell = ws_d[f"{c_let}{r_idx}"]
            cell.value = val
            cell.font = code_font if c_idx in [1, 2, 6] else data_font
            cell.fill = row_fill
            cell.border = data_border
            cell.alignment = align_center if c_idx in [3, 7] else align_left
            if c_idx == 7:
                if val == "Medium":
                    cell.fill, cell.font = med_fill, med_font
                elif val == "Low":
                    cell.fill, cell.font = low_fill, low_font
        ws_d.row_dimensions[r_idx].height = 24

    # --- Sheet 4: Risk Summary ---
    ws_r = wb_findings.create_sheet(title="Risk Summary")
    ws_r.views.sheetView[0].showGridLines = True

    ws_r["B2"] = "EXECUTIVE SECURITY POSTURE & RISK SUMMARY"
    ws_r["B2"].font = title_font

    ws_r["B4"] = "OVERALL SECURITY RATING:"
    ws_r["B4"].font = data_font_bold
    ws_r["C4"] = "48 / 100 (HIGH RISK — ACTION REQUIRED)"
    ws_r["C4"].font = Font(name=font_family, size=11, bold=True, color="991B1B")

    ws_r["B6"] = "FINDINGS SEVERITY MATRIX"
    ws_r["B6"].font = section_font

    risk_headers = ["Severity Level", "Finding Count", "Impact Level", "Remediation SLA"]
    for idx, h in enumerate(risk_headers):
        c = chr(ord('B') + idx)
        cell = ws_r[f"{c}7"]
        cell.value = h
        cell.font = header_font
        cell.fill = header_fill
        cell.border = data_border
    ws_r.row_dimensions[7].height = 24

    risk_matrix = [
        ("Critical", 2, "Complete Cloud & Account Compromise", "Immediate (Within 24 Hours)"),
        ("High", 3, "IDOR, API Key Leaks, SSRF Ingestion", "Urgent (Within 7 Days)"),
        ("Medium", 2, "Stored XSS, Missing Security Headers", "Scheduled (Within 30 Days)"),
        ("Low", 2, "Unpinned Dependencies, Client Flags", "Next Release Cycle")
    ]
    for idx, (sev, cnt, imp, sla) in enumerate(risk_matrix):
        r = 8 + idx
        ws_r[f"B{r}"] = sev
        ws_r[f"B{r}"].font = crit_font if sev == "Critical" else high_font if sev == "High" else med_font if sev == "Medium" else low_font
        ws_r[f"B{r}"].fill = crit_fill if sev == "Critical" else high_fill if sev == "High" else med_fill if sev == "Medium" else low_fill
        ws_r[f"B{r}"].border = data_border

        ws_r[f"C{r}"] = cnt
        ws_r[f"C{r}"].font = data_font_bold
        ws_r[f"C{r}"].alignment = align_center
        ws_r[f"C{r}"].border = data_border

        ws_r[f"D{r}"] = imp
        ws_r[f"D{r}"].font = data_font
        ws_r[f"D{r}"].border = data_border

        ws_r[f"E{r}"] = sla
        ws_r[f"E{r}"].font = data_font_bold
        ws_r[f"E{r}"].border = data_border
        ws_r.row_dimensions[r].height = 22

    # Save findings.xlsx
    findings_file = os.path.join(out_dir, "findings.xlsx")
    wb_findings.save(findings_file)
    print(f"Generated findings workbook: {findings_file}")

    # =========================================================================
    # BUILD WORKBOOK: endpoint-inventory.xlsx
    # =========================================================================
    wb_endpoints = openpyxl.Workbook()
    ws_ep = wb_endpoints.active
    ws_ep.title = "Endpoint Inventory"
    ws_ep.views.sheetView[0].showGridLines = True
    ws_ep.freeze_panes = "A2"

    for col_idx, (h_name, width) in enumerate(e_headers, start=1):
        c_let = get_column_letter(col_idx)
        cell = ws_ep[f"{c_let}1"]
        cell.value = h_name
        cell.font = header_font
        cell.fill = header_fill
        cell.border = data_border
        cell.alignment = align_center if col_idx == 2 else align_left
        ws_ep.column_dimensions[c_let].width = width
    ws_ep.row_dimensions[1].height = 26

    for r_idx, ep in enumerate(endpoints, start=2):
        row_fill = zebra_even if r_idx % 2 == 0 else zebra_odd
        vals = [ep["endpoint"], ep["method"], ep["auth"], ep["roles"], ep["file"], ep["desc"]]
        for c_idx, val in enumerate(vals, start=1):
            c_let = get_column_letter(c_idx)
            cell = ws_ep[f"{c_let}{r_idx}"]
            cell.value = val
            cell.font = code_font if c_idx in [1, 5] else data_font_bold if c_idx == 2 else data_font
            cell.fill = row_fill
            cell.border = data_border
            cell.alignment = align_center if c_idx == 2 else align_left
        ws_ep.row_dimensions[r_idx].height = 24

    endpoint_file = os.path.join(out_dir, "endpoint-inventory.xlsx")
    wb_endpoints.save(endpoint_file)
    print(f"Generated endpoint inventory workbook: {endpoint_file}")

if __name__ == "__main__":
    generate_security_artifacts()
