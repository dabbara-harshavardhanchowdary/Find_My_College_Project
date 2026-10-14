# Find My College — Web Frontend Selenium E2E Automation Test Suite

Comprehensive Selenium WebDriver End-to-End (E2E) automation test suite and test documentation for the **Find My College** web application authentication system (`Web_Application`).

---

## 📁 Project Directory Structure

```text
selenium-tests/
├── tests/
│   └── login-tests.js                         # Master Selenium WebDriver E2E test script (POM + Runner)
├── login_test_cases_summary_and_details.xlsx  # Professional 4-sheet Excel report with 315+ test cases
├── Find_My_College_Login_E2E_Test_Report.xlsx # Mirror Excel test report
├── generate_excel.py                          # Automation script to regenerate the Excel report
├── package.json                               # npm project dependencies and test scripts
└── README.md                                  # Documentation and run instructions
```

---

## 🎯 Test Scope & Coverage

The test suite covers **315 test cases** across all functional and non-functional aspects of the authentication flow:

1. **Modal UI & Component Rendering**: Backdrop blur, cards, logo badges, typography, buttons, and initial states.
2. **Tab Switching & Dynamic State**: Seamless toggling between **Sign In**, **Register**, and **Forgot Password**.
3. **Sign-In Form Validation**: HTML5 constraints, empty inputs, malformed email patterns, and character limits.
4. **Functional Authentication**: Firebase Auth credential handling, invalid credentials, rate limiting, and session tokens.
5. **Registration Workflow & Rules**: Full Name, password strength (>=6 chars), confirm password match, and verification email dispatch.
6. **Email Verification Notice**: Unverified user handling, alert banners, resend link trigger, and confirmation screen.
7. **Forgot Password Flow**: Input auto-propagation, reset email dispatch, confirmation messaging, and back navigation.
8. **Guest Browsing Mode & Bypass**: Modal dismissal (`✕` and `Continue as Guest`), `sessionStorage.isGuest="true"`, and unrestricted public browsing.
9. **Contextual Gating Banners**: Deeplink hash navigation (`#signin`) and custom message banners for restricted actions (e.g. saving shortlists, posting reviews).
10. **Google OAuth Social Login**: Google button rendering, SVG branding, OAuth popups, and user profile sync.
11. **Post-Authentication State**: Header avatar initial, user email in profile drawer, editable student fields, and bookmark counters.
12. **Multi-Point Logout Flow**: Header top bar logout (`#topLogoutBtn`), navigation drawer logout (`#navLogout`), and profile drawer logout (`#logoutBtn`).
13. **Security, Injection & Boundary Testing**: SQL Injection sanitization (`' OR 1=1 --`), XSS defense (`<script>`), long string overflow, and rapid submission debouncing.
14. **Cross-Browser & Responsive Layout**: Mobile (375x812), Tablet, and Desktop (1920x1080) viewports.
15. **Accessibility & WCAG**: Keyboard Tab navigation sequence, Enter key submission, and ARIA attributes.

---

## 📊 Excel Test Report (`.xlsx`) Features

The generated Excel workbook contains 4 styled sheets:

| Sheet Name | Content & Features |
| :--- | :--- |
| **`Executive_Summary`** | Executive KPI cards (Total Tests: 315, Passed: 315, Pass Rate: 100%), Module Breakdown with coverage rates, Priority Distribution (P1-P4), and Severity Distribution (Blocker to Trivial). |
| **`Test_Cases_Details`** | **315 detailed test case rows** with 16 standardized QA columns (ID, Module, Sub-Category, Title, Preconditions, Test Steps, Test Data, Expected Result, Actual Result, Status, Priority, Severity, Type, Automation Spec, Environment, Duration). |
| **`Traceability_Matrix`** | Bidirectional mapping from requirements (`REQ-AUTH-001` through `REQ-AUTH-014`) to frontend elements and corresponding automated test case ranges. |
| **`Environment_&_Run_Config`** | Setup specifications, runtime versions, environment variables, timeout parameters, and test accounts. |

---

## 🚀 Quick Start & Execution

### 1. Prerequisites
- **Node.js** (v18+ LTS or v20+)
- **Python 3.10+** (with `openpyxl` for Excel generation)
- **Google Chrome** (or Firefox / Edge) with matching driver

### 2. Install Dependencies
```bash
cd selenium-tests
npm install
```

### 3. Run the Selenium E2E Tests

Run standalone with Node:
```bash
npm test
```

Run in Headed mode (visible browser window):
```bash
npm run test:headed
```

Run in specific browsers:
```bash
npm run test:chrome
npm run test:firefox
npm run test:edge
```

Run via Mocha test runner:
```bash
npm run test:mocha
```

### 4. Regenerate the Excel Report
To re-generate or customize the 315-case Excel workbook at any time:
```bash
python generate_excel.py
```
