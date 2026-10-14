import { logoutUser } from "../../services/authService.js?v=2";
import { initCompareScreen } from "../../screens/CompareScreen.js";
import { initHomeScreen } from "../../screens/HomeScreen.js";
import { initCollegesScreen } from "../../screens/CollegesScreen.js";
import { renderBookmarksView, updateBookmarkBadges } from "../../services/bookmarkService.js";
import { fetchColleges } from "../../services/collegeService.js";
import { auth } from "../../config/firebase-config.js";
import { openAuthModal } from "../modals/AuthModal.js";

export function initNavDrawer() {
    const menuBtn = document.getElementById("menuBtn");
    const closeNavBtn = document.getElementById("closeNavBtn");
    const navDrawer = document.getElementById("navDrawer");
    const brandHomeBtn = document.getElementById("brandHomeBtn");

    const navHome = document.getElementById("navHome");
    const navColleges = document.getElementById("navColleges");
    const navBookmarks = document.getElementById("navBookmarks");
    const navCompare = document.getElementById("navCompare");
    const navProfile = document.getElementById("navProfile");
    const navAbout = document.getElementById("navAbout");
    const navLogout = document.getElementById("navLogout");
    const headerBookmarkBtn = document.getElementById("headerBookmarkBtn");

    const homeSection = document.getElementById("homeSection");
    const collegesSection = document.getElementById("collegesSection");
    const bookmarksSection = document.getElementById("bookmarksSection");
    const compareSection = document.getElementById("compareSection");
    const aboutSection = document.getElementById("aboutSection");
    const profileDrawer = document.getElementById("profileDrawer");

    // Initial badge update
    updateBookmarkBadges();

    // Open drawer
    if (menuBtn && navDrawer) {
        menuBtn.addEventListener("click", () => {
            navDrawer.classList.remove("hidden");
        });
    }

    // Close drawer on button click
    if (closeNavBtn && navDrawer) {
        closeNavBtn.addEventListener("click", () => {
            navDrawer.classList.add("hidden");
        });
    }

    // Close drawer on backdrop click
    if (navDrawer) {
        navDrawer.addEventListener("click", (e) => {
            if (e.target === navDrawer) {
                navDrawer.classList.add("hidden");
            }
        });
    }

    // Helper to switch sections
    function switchView(selectedNav, activeSection) {
        if (homeSection) homeSection.classList.add("hidden");
        if (collegesSection) collegesSection.classList.add("hidden");
        if (bookmarksSection) bookmarksSection.classList.add("hidden");
        if (compareSection) compareSection.classList.add("hidden");
        if (aboutSection) aboutSection.classList.add("hidden");

        const navButtons = [navHome, navColleges, navBookmarks, navCompare, navProfile, navAbout];
        navButtons.forEach(btn => {
            if (btn) btn.className = "w-full flex items-center justify-between px-4 py-3 rounded-xl text-slate-300 hover:bg-slate-800 hover:text-white text-sm font-bold transition";
        });

        if (selectedNav) {
            selectedNav.className = "w-full flex items-center justify-between px-4 py-3 rounded-xl bg-blue-600/20 text-blue-400 border border-blue-500/30 text-sm font-bold transition";
        }
        
        if (activeSection) activeSection.classList.remove("hidden");
        if (navDrawer) navDrawer.classList.add("hidden");
    }

    const openHomeView = () => onNavigateByName("home");
    const openCollegesView = () => onNavigateByName("colleges");
    const openBookmarksView = () => onNavigateByName("bookmarks");
    const openCompareView = () => onNavigateByName("compare");
    const openAboutView = () => onNavigateByName("about");

    function onNavigateByName(screenName, updateHistory = true) {
        if (!screenName) return;
        const page = String(screenName).toLowerCase().replace("#", "").trim();

        if (updateHistory) {
            try {
                localStorage.setItem("activePage", page);
                if (window.location.hash !== `#${page}`) {
                    window.history.replaceState(null, null, `#${page}`);
                }
            } catch (e) {
                console.warn("Unable to save active page state:", e);
            }
        }

        if (page === "home") {
            switchView(navHome, homeSection);
            initHomeScreen(onNavigateByName);
        } else if (page === "colleges") {
            switchView(navColleges, collegesSection);
            initCollegesScreen();
        } else if (page === "bookmarks") {
            switchView(navBookmarks, bookmarksSection);
            fetchColleges().then(cols => renderBookmarksView(cols));
        } else if (page === "compare") {
            switchView(navCompare, compareSection);
            initCompareScreen();
        } else if (page === "about") {
            switchView(navAbout, aboutSection);
        }
    }

    if (navHome) navHome.addEventListener("click", openHomeView);
    if (navColleges) navColleges.addEventListener("click", openCollegesView);
    if (navBookmarks) navBookmarks.addEventListener("click", openBookmarksView);
    if (headerBookmarkBtn) headerBookmarkBtn.addEventListener("click", openBookmarksView);
    if (navCompare) navCompare.addEventListener("click", openCompareView);
    if (navAbout) navAbout.addEventListener("click", openAboutView);
    if (brandHomeBtn) brandHomeBtn.addEventListener("click", openHomeView);

    // Listen for hash changes (browser back/forward or manual hash change)
    window.addEventListener("hashchange", () => {
        const rawHash = window.location.hash.replace("#", "").toLowerCase().trim();
        if (rawHash && ["home", "colleges", "bookmarks", "compare", "about"].includes(rawHash)) {
            onNavigateByName(rawHash, false);
        }
    });

    // Auto-restore present page state on page refresh / initial load
    const currentHash = window.location.hash.replace("#", "").toLowerCase().trim();
    const savedPage = localStorage.getItem("activePage");
    const validPages = ["home", "colleges", "bookmarks", "compare", "about"];
    const initialPage = (currentHash && validPages.includes(currentHash))
        ? currentHash
        : (savedPage && validPages.includes(savedPage) ? savedPage : "home");

    onNavigateByName(initialPage, true);

    if (navProfile) {
        navProfile.addEventListener("click", () => {
            if (navDrawer) navDrawer.classList.add("hidden");
            if (!auth.currentUser) {
                openAuthModal("🔒 Unlock Your Personal Experience: Register or Sign In to save shortlists and sync across devices.");
                return;
            }
            if (profileDrawer) profileDrawer.classList.remove("hidden");
        });
    }

    if (navLogout) {
        navLogout.addEventListener("click", async (e) => {
            if (e) e.preventDefault();
            if (navDrawer) navDrawer.classList.add("hidden");
            if (profileDrawer) profileDrawer.classList.add("hidden");

            try {
                await logoutUser();
                console.log("User signed out from Nav Drawer");
                window.location.hash = "#signin";
                openAuthModal("🔒 You have signed out. Please sign in to access your account.", "signin");
            } catch (err) {
                console.error("Logout error:", err);
                alert("Failed to log out: " + err.message);
            }
        });
    }

    // Listen for bookmarks change event
    window.addEventListener("bookmarksChanged", () => {
        updateBookmarkBadges();
        if (bookmarksSection && !bookmarksSection.classList.contains("hidden")) {
            fetchColleges().then(cols => renderBookmarksView(cols));
        }
    });

    // Expose global navigator helper for inline HTML buttons
    window.openCollegeAppScreen = onNavigateByName;
}