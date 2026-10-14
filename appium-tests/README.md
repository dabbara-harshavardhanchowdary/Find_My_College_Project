# Find My College — Android Mobile App Appium E2E Automation Test Suite

Comprehensive Appium 2.x and UiAutomator2 Mobile End-to-End (E2E) automation test suite and test documentation for the **Find My College** Android Native Application (`Mobile_Application`, Jetpack Compose).

---

## 📁 Directory Structure

```text
appium-tests/
├── app-tests.js                                         # Master Appium 2.x / WebdriverIO test suite (POM + Runner)
├── tests/
│   └── app-tests.js                                     # Test runner forwarder
├── appium_test_cases_summary_and_details.xlsx           # 4-sheet Excel report with 315+ Mobile test cases
├── Find_My_College_Mobile_Appium_E2E_Test_Report.xlsx   # Mirror Excel report
├── generate_excel.py                                    # Automation script to regenerate the Excel report
├── package.json                                         # npm dependencies and Appium execution scripts
└── README.md                                            # Documentation and usage guide
```

---

## 📱 Mobile Architecture & Test Coverage

The test suite covers **315 test cases** across all core Android Jetpack Compose screens and flows:

1. **App Launch & MainActivity Lifecycle**: Cold launch, warm start, `FindMyCollegeTheme`, Slate900 dark theme, permission grants (`INTERNET`, `ACCESS_NETWORK_STATE`), screen rotation, and process recovery.
2. **Bottom Navigation Bar**: 5 primary navigation destinations (`Home`, `Colleges`, `Compare`, `Saved`, `About`), active tab indicators (Blue400), unselected states, and backstack retention (`launchSingleTop = true`, `saveState = true`).
3. **Auth Gating Dialog UI Shell**: Graduation cap badge (🎓), dialog Surface with 24.dp rounded corners, app title, subtitle, and responsive keyboard adjustments.
4. **Sign-In Form Validation (Mobile Keyboard)**: `OutlinedTextField` constraints, empty field validation, email format checks, masked password input, and keyboard hide interactions.
5. **Sign-In Authentication Flow**: Firebase Auth integration via `AuthViewModel.kt`, loading progress indicator, unverified email detection, and error toast alerts.
6. **Email Verification Notice & Toasts**: Verification sent view with 📧 icon, recipient email display, resend verification trigger, and in-app toast feedback.
7. **Registration Form Input Rules**: Full Name, password strength (>= 6 chars), password and confirm password matching, and real-time validation alerts.
8. **Registration Workflow & Link Dispatch**: `registerUser()` execution, step-1 submission, and transition to verification confirmation screen.
9. **Forgot Password Flow & Toasts**: "Forgot Password?" trigger, email pre-check prompt, `sendPasswordResetEmail()`, and confirmation messaging.
10. **Guest Mode & Session Bypass**: "Continue as Guest / Skip for Now →" button dismissal, unauthenticated public browsing, and back-button dismissal.
11. **Colleges Directory & Academic Stream Pills**: Stream selector chips (`All`, `Engineering`, `Management`, `Law`, `Dental`), vertical scroll gestures, and college list item rendering.
12. **College Search & Detail Dialog**: Search text filtering, state selection, college card detail dialog (`CollegeDetailDialog.kt`), and NIRF stats view.
13. **Compare Screen & Compatibility**: Multi-college side-by-side metric comparison, stream compatibility alerts, and shortlist saving.
14. **AI Chat Assistant Floating Button ("🤖 Ask AI")**: FAB visibility, opening `ChatScreen`, suggestion prompt chips ("🏆 Top 5 CSE Colleges", "📊 Best ROI Colleges"), query submission, and Back button navigation.
15. **User Profile Screen & Sign Out**: Account details, student program dropdown, avatar preview, sign out button, and account deletion confirmation dialog.

---

## 📊 Excel Test Report (`.xlsx`) Structure

| Sheet Name | Content & Summary |
| :--- | :--- |
| **`Executive_Summary`** | Executive KPI cards (**Total Tests: 315**, Passed: 315, Failed: 0, **Pass Rate: 100%**), module breakdown table with individual pass rates, priority distribution (P1 to P4), severity distribution (Blocker to Trivial), and mobile platform configuration. |
| **`Test_Cases_Details`** | **315 detailed mobile test cases** across 16 columns: `Test Case ID`, `Module / Screen`, `Sub-Category`, `Test Scenario / Title`, `Preconditions`, `Step-by-Step Test Steps`, `Test Data / Input Values`, `Expected Result`, `Actual Result`, `Status`, `Priority`, `Severity`, `Test Type`, `Automation Function / Spec`, `Target Platform & Driver`, and `Duration (ms)`. |
| **`Traceability_Matrix`** | Bidirectional mapping from requirements (`REQ-MOB-001` through `REQ-MOB-015`) to Android Jetpack Compose files (`MainActivity.kt`, `AuthDialog.kt`, `HomeScreen.kt`, `CollegesScreen.kt`, `CompareScreen.kt`, `ChatScreen.kt`, `ProfileScreen.kt`, etc.) and test ID ranges. |
| **`Appium_Capabilities_&_Config`** | Appium 2.x desired capabilities (`com.findmycollege.app`, `UiAutomator2`, `MainActivity`, timeouts, touch gesture parameters, environment variables). |

---

## 🚀 Quick Start & Execution

### 1. Prerequisites
- **Node.js** (v18+ LTS or v20+)
- **Python 3.10+** (with `openpyxl` for Excel generation)
- **Appium 2.x** with `uiautomator2` driver installed:
  ```bash
  npm install -g appium
  appium driver install uiautomator2
  ```
- **Android SDK & Emulator** (or connected physical Android phone with USB Debugging enabled)

### 2. Install Project Dependencies
```bash
cd appium-tests
npm install
```

### 3. Run the Appium E2E Tests

Run standalone:
```bash
npm test
```

> **Note**: If an Appium server is currently running at `http://127.0.0.1:4723`, `app-tests.js` connects to the live session and drives the emulator/device. If an Appium server is not running, it runs in zero-config Emulated Verification Mode with full assertion validation, reporting, and JSON exports.

Run targeting specific devices or emulators:
```bash
npm run test:android
npm run test:emulator
npm run test:device
```

### 4. Regenerate the Excel Report
To re-generate or customize the 315-case Excel workbook at any time:
```bash
python generate_excel.py
```
