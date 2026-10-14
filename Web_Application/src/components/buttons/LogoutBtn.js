import { logoutUser } from "../../services/authService.js?v=2";
import { openAuthModal } from "../modals/AuthModal.js";

// Utility to initialize and attach event listeners to all logout buttons
export function initLogoutButtons() {
    const logoutBtn = document.getElementById("logoutBtn");
    const topLogoutBtn = document.getElementById("topLogoutBtn");
    const navLogout = document.getElementById("navLogout");

    const handleLogout = async (e) => {
        if (e) e.preventDefault();

        // 1. Automatically close all open drawers
        const profileDrawer = document.getElementById("profileDrawer");
        const navDrawer = document.getElementById("navDrawer");
        if (profileDrawer) profileDrawer.classList.add("hidden");
        if (navDrawer) navDrawer.classList.add("hidden");

        try {
            // 2. Perform Firebase Auth signOut
            await logoutUser();
            console.log("User logged out successfully");

            // 3. Clear session hash & set signin hash indicator
            window.location.hash = "#signin";

            // 4. Open Sign In / Auth modal in sign-in tab
            openAuthModal("🔒 You have signed out. Please sign in to access your account.", "signin");
        } catch (err) {
            console.error("Logout error:", err);
            alert("Failed to log out: " + err.message);
        }
    };

    if (logoutBtn) logoutBtn.addEventListener("click", handleLogout);
    if (topLogoutBtn) topLogoutBtn.addEventListener("click", handleLogout);
    if (navLogout) navLogout.addEventListener("click", handleLogout);

    return handleLogout;
}

export { logoutUser };

