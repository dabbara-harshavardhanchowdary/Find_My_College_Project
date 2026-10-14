import { loginUser, registerUser, loginWithGoogle, sendUserVerificationEmail, sendUserPasswordResetEmail } from "../../services/authService.js?v=2";
import { auth } from "../../config/firebase-config.js";

let isSignupMode = false;
let isForgotPasswordMode = false;
let registeredEmailForVerification = "";

export function openAuthModal(gatingMessage = null, initialTab = 'signin') {
    const authContainer = document.getElementById("authContainer");
    if (!authContainer) return;

    authContainer.classList.remove("hidden");
    authContainer.style.display = "flex";

    const banner = document.getElementById("authGatingBanner");
    if (banner) {
        if (gatingMessage) {
            banner.textContent = gatingMessage;
            banner.classList.remove("hidden");
        } else {
            banner.classList.add("hidden");
        }
    }

    if (initialTab === 'signin' && typeof window.switchToSigninTab === 'function') {
        window.switchToSigninTab();
    } else if (initialTab === 'signup' && typeof window.switchToSignupTab === 'function') {
        window.switchToSignupTab();
    }
}

export function closeAuthModal() {
    const authContainer = document.getElementById("authContainer");
    if (authContainer) {
        authContainer.classList.add("hidden");
        authContainer.style.display = "none";
    }
}

window.openAuthModal = openAuthModal;
window.closeAuthModal = closeAuthModal;

export function initAuthModal() {
    const authContainer = document.getElementById("authContainer");
    if (!authContainer) return;

    const tabSignin = document.getElementById("tabSignin");
    const tabSignup = document.getElementById("tabSignup");
    const signupFields = document.getElementById("signupFields");
    const signupConfirmPasswordField = document.getElementById("signupConfirmPasswordField");
    const emailVerificationSentSection = document.getElementById("emailVerificationSentSection");
    const forgotPasswordSection = document.getElementById("forgotPasswordSection");
    const forgotPasswordBtn = document.getElementById("forgotPasswordBtn");
    const sendForgotPasswordBtn = document.getElementById("sendForgotPasswordBtn");
    const backToSigninFromForgotBtn = document.getElementById("backToSigninFromForgotBtn");
    const verificationSentText = document.getElementById("verificationSentText");
    const resendVerificationLinkBtn = document.getElementById("resendVerificationLinkBtn");
    const backToSigninFromVerificationBtn = document.getElementById("backToSigninFromVerificationBtn");
    
    const authForm = document.getElementById("authForm");
    const authSubmitBtn = document.getElementById("authSubmitBtn");
    const googleAuthBtn = document.getElementById("googleAuthBtn");
    const guestBtn = document.getElementById("guestBtn");
    const authAlert = document.getElementById("authAlert");

    function showAlert(msg, isSuccess = false) {
        if (!authAlert) return;
        authAlert.innerHTML = msg;
        authAlert.className = `mb-4 p-3 rounded-xl text-xs text-left ${isSuccess ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40' : 'bg-red-600/20 text-red-300 border border-red-500/40'}`;
        authAlert.classList.remove("hidden");
    }

    function hideAlert() {
        if (authAlert) authAlert.classList.add("hidden");
    }

    // Switch to Sign In Tab
    function switchToSignin() {
        isSignupMode = false;
        isForgotPasswordMode = false;
        hideAlert();
        if (tabSignin) tabSignin.className = "w-1/2 py-2 text-xs font-bold rounded-lg bg-blue-600 text-white transition";
        if (tabSignup) tabSignup.className = "w-1/2 py-2 text-xs font-bold rounded-lg text-slate-400 hover:text-white transition";
        if (signupFields) signupFields.classList.add("hidden");
        if (signupConfirmPasswordField) signupConfirmPasswordField.classList.add("hidden");
        if (emailVerificationSentSection) emailVerificationSentSection.classList.add("hidden");
        if (forgotPasswordSection) forgotPasswordSection.classList.add("hidden");
        if (authForm) authForm.classList.remove("hidden");
        
        // Show Sign in elements
        const emailDiv = document.getElementById("authEmail")?.parentElement;
        const pwdDiv = document.getElementById("authPassword")?.parentElement?.parentElement;
        if (emailDiv) emailDiv.classList.remove("hidden");
        if (pwdDiv) pwdDiv.classList.remove("hidden");

        if (authSubmitBtn) {
            authSubmitBtn.textContent = "Sign In";
            authSubmitBtn.classList.remove("hidden");
        }
        if (googleAuthBtn) googleAuthBtn.classList.remove("hidden");
    }

    // Switch to Register Tab
    function switchToSignup() {
        isSignupMode = true;
        isForgotPasswordMode = false;
        hideAlert();
        if (tabSignup) tabSignup.className = "w-1/2 py-2 text-xs font-bold rounded-lg bg-blue-600 text-white transition";
        if (tabSignin) tabSignin.className = "w-1/2 py-2 text-xs font-bold rounded-lg text-slate-400 hover:text-white transition";
        if (signupFields) signupFields.classList.remove("hidden");
        if (signupConfirmPasswordField) signupConfirmPasswordField.classList.remove("hidden");
        if (emailVerificationSentSection) emailVerificationSentSection.classList.add("hidden");
        if (forgotPasswordSection) forgotPasswordSection.classList.add("hidden");
        if (authForm) authForm.classList.remove("hidden");

        const emailDiv = document.getElementById("authEmail")?.parentElement;
        const pwdDiv = document.getElementById("authPassword")?.parentElement?.parentElement;
        if (emailDiv) emailDiv.classList.remove("hidden");
        if (pwdDiv) pwdDiv.classList.remove("hidden");

        if (authSubmitBtn) {
            authSubmitBtn.textContent = "Create Account & Send Verification Link";
            authSubmitBtn.classList.remove("hidden");
        }
        if (googleAuthBtn) googleAuthBtn.classList.add("hidden");
    }

    // Switch to Forgot Password Mode
    function switchToForgotPassword() {
        isForgotPasswordMode = true;
        hideAlert();
        if (signupFields) signupFields.classList.add("hidden");
        if (signupConfirmPasswordField) signupConfirmPasswordField.classList.add("hidden");
        if (emailVerificationSentSection) emailVerificationSentSection.classList.add("hidden");
        
        const emailDiv = document.getElementById("authEmail")?.parentElement;
        const pwdDiv = document.getElementById("authPassword")?.parentElement?.parentElement;
        if (emailDiv) emailDiv.classList.add("hidden");
        if (pwdDiv) pwdDiv.classList.add("hidden");

        if (authSubmitBtn) authSubmitBtn.classList.add("hidden");
        if (googleAuthBtn) googleAuthBtn.classList.add("hidden");

        if (forgotPasswordSection) {
            const currentEmail = document.getElementById("authEmail")?.value?.trim();
            const fpInput = document.getElementById("forgotPasswordEmail");
            if (fpInput && currentEmail) fpInput.value = currentEmail;
            forgotPasswordSection.classList.remove("hidden");
        }
    }

    if (forgotPasswordBtn) forgotPasswordBtn.onclick = switchToForgotPassword;
    if (backToSigninFromForgotBtn) backToSigninFromForgotBtn.onclick = switchToSignin;

    if (sendForgotPasswordBtn) {
        sendForgotPasswordBtn.onclick = async () => {
            const emailInput = document.getElementById("forgotPasswordEmail");
            const email = emailInput ? emailInput.value.trim() : "";

            if (!email) {
                showAlert("Please enter your registered student email address.");
                return;
            }

            sendForgotPasswordBtn.disabled = true;
            sendForgotPasswordBtn.textContent = "Sending Link...";

            try {
                await sendUserPasswordResetEmail(email);
                showAlert(`📧 Password reset link sent to your email inbox (<b>${email}</b>)! Please check your spam folder as well.`, true);
            } catch (err) {
                console.error("Forgot password error:", err);
                if (err.code === "auth/user-not-found") {
                    showAlert("User not found. No account exists with this email address.");
                } else if (err.code === "auth/invalid-email") {
                    showAlert("Invalid email address format. Please check your entry.");
                } else {
                    showAlert("Error: " + (err.message || "Failed to send password reset email."));
                }
            } finally {
                sendForgotPasswordBtn.disabled = false;
                sendForgotPasswordBtn.textContent = "🔑 Send Password Reset Link";
            }
        };
    }

    // Expose tab switches globally for openAuthModal
    window.switchToSigninTab = switchToSignin;
    window.switchToSignupTab = switchToSignup;

    if (tabSignin) tabSignin.onclick = switchToSignin;
    if (tabSignup) tabSignup.onclick = switchToSignup;

    // Show Email Verification Confirmation Screen
    function showVerificationSentScreen(email) {
        registeredEmailForVerification = email;
        hideAlert();
        if (authForm) authForm.classList.add("hidden");
        if (googleAuthBtn) googleAuthBtn.classList.add("hidden");
        if (emailVerificationSentSection) {
            if (verificationSentText) {
                verificationSentText.innerHTML = `We have sent an official verification link to <b>${email}</b>. Please click the link in your inbox (or spam folder) to activate your account.`;
            }
            emailVerificationSentSection.classList.remove("hidden");
        }
    }

    // Form Submit Handler
    if (authForm) {
        authForm.onsubmit = async (e) => {
            e.preventDefault();
            hideAlert();

            const email = document.getElementById("authEmail")?.value?.trim();
            const password = document.getElementById("authPassword")?.value?.trim();

            if (!isSignupMode) {
                // SIGN IN
                try {
                    const userCredential = await loginUser(email, password);
                    const user = userCredential.user;

                    // Reload user to check latest email verification status
                    await user.reload();

                    if (!user.emailVerified) {
                        showAlert(`
                            <div class="space-y-2">
                                <div>⚠️ <b>Email Not Verified:</b> Please verify your email address (${user.email}) before proceeding.</div>
                                <button type="button" id="alertResendBtn" class="px-3 py-1 bg-amber-500/30 text-amber-300 hover:bg-amber-500/50 rounded-lg text-xs font-bold transition">
                                    📧 Click here to resend verification link
                                </button>
                            </div>
                        `);

                        const alertResendBtn = document.getElementById("alertResendBtn");
                        if (alertResendBtn) {
                            alertResendBtn.onclick = async () => {
                                try {
                                    await sendUserVerificationEmail();
                                    showAlert(`📧 Verification link resent to <b>${user.email}</b>! Please check your inbox or spam folder.`, true);
                                } catch (resendErr) {
                                    showAlert("Resend Error: " + resendErr.message);
                                }
                            };
                        }
                        return;
                    }

                    closeAuthModal();
                } catch (err) {
                    showAlert("Sign In Error: " + err.message);
                }
            } else {
                // REGISTER USER (Step 1 -> Send Firebase Email Verification)
                const fullName = document.getElementById("authFullName")?.value?.trim();
                const confirmPassword = document.getElementById("authConfirmPassword")?.value?.trim();

                if (!fullName) return showAlert("Please enter your Full Name.");
                if (!email) return showAlert("Please enter your Email Address.");
                if (!password || password.length < 6) return showAlert("Password must be at least 6 characters.");
                if (password !== confirmPassword) return showAlert("Passwords do not match.");

                try {
                    authSubmitBtn.disabled = true;
                    authSubmitBtn.textContent = "Sending Verification Link...";

                    await registerUser(email, password, fullName);
                    showVerificationSentScreen(email);
                } catch (err) {
                    showAlert("Registration Error: " + err.message);
                } finally {
                    authSubmitBtn.disabled = false;
                    authSubmitBtn.textContent = "Create Account & Send Verification Link";
                }
            }
        };
    }

    // Resend Verification Link Button Handler
    if (resendVerificationLinkBtn) {
        resendVerificationLinkBtn.onclick = async () => {
            try {
                resendVerificationLinkBtn.disabled = true;
                resendVerificationLinkBtn.textContent = "Sending Link...";
                await sendUserVerificationEmail();
                showAlert(`📧 Verification link resent to <b>${registeredEmailForVerification || 'your email'}</b>!`, true);
            } catch (err) {
                showAlert("Resend Error: " + err.message);
            } finally {
                resendVerificationLinkBtn.disabled = false;
                resendVerificationLinkBtn.textContent = "📧 Resend Verification Link";
            }
        };
    }

    if (backToSigninFromVerificationBtn) {
        backToSigninFromVerificationBtn.onclick = () => {
            switchToSignin();
        };
    }

    // Google Auth Button
    if (googleAuthBtn) {
        googleAuthBtn.onclick = async () => {
            try {
                await loginWithGoogle();
                closeAuthModal();
            } catch (err) {
                showAlert("Google Sign-in Error: " + err.message);
            }
        };
    }

    // Continue as Guest Button & Close Modal Handlers
    const closeAuthModalBtn = document.getElementById("closeAuthModalBtn");

    const handleGuestAction = (e) => {
        if (e) e.preventDefault();
        sessionStorage.setItem('isGuest', 'true');
        closeAuthModal();
        if (typeof window.showMainCollegeApp === 'function') {
            window.showMainCollegeApp();
        }
        if (typeof window.openCollegeAppScreen === 'function') {
            window.openCollegeAppScreen('home');
        }
    };

    if (guestBtn) guestBtn.onclick = handleGuestAction;
    if (closeAuthModalBtn) closeAuthModalBtn.onclick = handleGuestAction;
}