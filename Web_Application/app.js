import { db } from "./src/config/firebase-config.js";
import { collection, getDocs } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";
import { initHomeScreen } from "./src/screens/HomeScreen.js";
import { initCompareScreen } from "./src/screens/CompareScreen.js";
import { initAuthListener } from "./src/services/authService.js?v=2";
import { initNavDrawer } from "./src/components/drawers/NavDrawer.js";
import { initProfileDrawer, updateProfileUI } from "./src/components/drawers/ProfileDrawer.js";
import { initCollegeDetailModal } from "./src/components/modals/CollegeDetailModal.js";
import { initAuthModal, openAuthModal, closeAuthModal } from "./src/components/modals/AuthModal.js";
import { initLogoutButtons } from "./src/components/buttons/LogoutBtn.js";
import { updateBookmarkBadges } from "./src/services/bookmarkService.js";
import { initChatWidget } from "./src/components/chat/ChatWidget.js";
import { setSelectedStream, getInferredStream, updateStateFilterOptions } from "./src/screens/CollegesScreen.js";
import { createCollegeCard } from "./src/components/cards/CollegeCard.js";

// Ensure compat interface db.collection(...).get() is available on the Firestore instance
if (!db.collection) {
    db.collection = (collectionName) => ({
        get: () => getDocs(collection(db, collectionName))
    });
}

// 1. Define stream collections mapping
export const STREAM_COLLECTIONS = {
    "Dental": "dental_colleges",
    "Engineering": "engineering_colleges",
    "Law": "law_colleges",
    "Management": "management_colleges"
};

// State tracking for academic stream filter
export let selectedStream = "All";

// 2. Fetch function:
// - Specific stream: db.collection(STREAM_COLLECTIONS[stream]).get()
// - "All": query across all 4 collections using Promise.all and merge results
export async function fetchColleges(stream = "All") {
    let colleges = [];
    const streamNorm = (stream || "All").trim();
    const matchedStream = Object.keys(STREAM_COLLECTIONS).find(
        k => k.toLowerCase() === streamNorm.toLowerCase()
    );

    if (matchedStream && matchedStream !== "All" && STREAM_COLLECTIONS[matchedStream]) {
        // Query specific stream collection
        const collectionName = STREAM_COLLECTIONS[matchedStream];
        const snapshot = await db.collection(collectionName).get();
        snapshot.forEach((doc) => {
            const data = doc.data();
            colleges.push({
                id: doc.id,
                ...data,
                name: data.name || data.Name || "Unknown College",
                city: data.city || data.City || "",
                state: data.state || data.State || "",
                nirfRank: data.nirfRank !== undefined ? data.nirfRank : (data.Rank ?? data.rank),
                score: data.score !== undefined ? data.score : (data.Score ?? "N/A"),
                stream: data.stream || data.Stream || matchedStream
            });
        });
    } else {
        // When "All" is selected, fetch across all 4 collections using Promise.all and merge
        const entries = Object.entries(STREAM_COLLECTIONS);
        const snapshots = await Promise.all(
            entries.map(async ([streamName, collectionName]) => {
                const snap = await db.collection(collectionName).get();
                return { streamName, snap };
            })
        );

        snapshots.forEach(({ streamName, snap }) => {
            snap.forEach((doc) => {
                const data = doc.data();
                colleges.push({
                    id: doc.id,
                    ...data,
                    name: data.name || data.Name || "Unknown College",
                    city: data.city || data.City || "",
                    state: data.state || data.State || "",
                    nirfRank: data.nirfRank !== undefined ? data.nirfRank : (data.Rank ?? data.rank),
                    score: data.score !== undefined ? data.score : (data.Score ?? "N/A"),
                    stream: data.stream || data.Stream || streamName
                });
            });
        });
    }

    // Sort ascending by NIRF rank
    colleges.sort((a, b) => {
        const rA = Number(String(a.nirfRank ?? "999").replace(/[^0-9]/g, '')) || 999;
        const rB = Number(String(b.nirfRank ?? "999").replace(/[^0-9]/g, '')) || 999;
        return rA - rB;
    });

    // 4. Console log showing how many documents were retrieved to make debugging easy
    console.log(`Retrieved ${colleges.length} documents for stream: ${stream}`);

    return colleges;
}

// 3. Make sure the cards render:
// - college.name, college.city, college.state, college.nirfRank, and college.score
// - Display college.stream
export function renderCollegeCard(college) {
    return createCollegeCard(college);
}

export function renderColleges(colleges) {
    const grid = document.getElementById("collegesGrid");
    if (!grid) return;
    grid.innerHTML = "";

    if (!colleges || colleges.length === 0) {
        grid.innerHTML = `
            <div class="col-span-full text-center py-16 bg-slate-800/40 rounded-2xl border border-slate-700/50">
                <span class="text-4xl block mb-2">🔍</span>
                <p class="text-slate-300 font-semibold mb-1">No colleges found</p>
                <p class="text-xs text-slate-500">No institutions found for the selected filter</p>
            </div>
        `;
        return;
    }

    colleges.forEach((college) => {
        const card = renderCollegeCard(college);
        grid.appendChild(card);
    });
}

export async function loadAndRenderColleges(stream = "All") {
    const loader = document.getElementById("loader");
    const grid = document.getElementById("collegesGrid");
    const collegeCountBadge = document.getElementById("collegeCount");
    const titleEl = document.getElementById("collegeDirectoryTitle");

    if (loader) {
        loader.classList.remove("hidden");
        loader.style.display = "flex";
    }
    if (grid) {
        grid.classList.add("hidden");
    }

    try {
        const colleges = await fetchColleges(stream);

        if (titleEl) {
            titleEl.textContent = (stream === "All") ? "NIRF Colleges Directory" : `NIRF ${stream} Colleges Directory`;
        }

        renderColleges(colleges);

        if (collegeCountBadge) {
            collegeCountBadge.textContent = `${colleges.length} Colleges`;
        }

        if (typeof updateStateFilterOptions === "function") {
            updateStateFilterOptions(colleges);
        }

        window._allCollegesCache = colleges;

        if (typeof setSelectedStream === "function") {
            setSelectedStream(stream);
        }

        return colleges;
    } catch (err) {
        console.error("Error loading colleges for stream:", stream, err);
        if (grid) {
            grid.innerHTML = `
                <div class="col-span-full text-center py-16 bg-slate-800/40 rounded-2xl border border-red-500/30">
                    <span class="text-4xl block mb-2">⚠️</span>
                    <p class="text-slate-200 font-semibold mb-1">Failed to load colleges</p>
                    <p class="text-xs text-red-400">${err.message || err}</p>
                </div>
            `;
            grid.classList.remove("hidden");
        }
    } finally {
        if (loader) {
            loader.classList.add("hidden");
            loader.style.display = "none";
        }
        if (grid) {
            grid.classList.remove("hidden");
        }
    }
}

export function initStreamFilter() {
    const container = document.getElementById("streamFilterContainer");
    if (!container) return;

    const streamButtons = container.querySelectorAll(".stream-pill-btn");
    streamButtons.forEach((btn) => {
        btn.addEventListener("click", async () => {
            const stream = btn.dataset.stream || "All";
            selectedStream = stream;

            // Toggle active CSS class
            streamButtons.forEach((b) => {
                if (b === btn) {
                    b.className = "stream-pill-btn active-stream-pill px-4 py-2 rounded-xl text-xs font-bold transition border border-blue-500 bg-blue-600 text-white shadow-md shadow-blue-600/30";
                } else {
                    b.className = "stream-pill-btn px-4 py-2 rounded-xl text-xs font-bold transition border border-slate-700 bg-slate-900 text-slate-300 hover:border-slate-500 hover:text-white";
                }
            });

            // Fetch from the active collection (dental_colleges, engineering_colleges, etc.) and update view
            await loadAndRenderColleges(stream);

            // Ensure colleges section is visible if currently navigated away
            const collegesSection = document.getElementById("collegesSection");
            if (collegesSection && collegesSection.classList.contains("hidden")) {
                if (typeof window.openCollegeAppScreen === "function") {
                    window.openCollegeAppScreen("colleges");
                }
            }
        });
    });
}

// Comparison logic & Stream-Matching Validation
export function getCollegeStream(college) {
    if (!college) return "Engineering";
    return getInferredStream(college);
}

export function validateCollegeStreams(college1, college2) {
    if (!college1 || !college2) return { matches: true };
    const stream1 = getCollegeStream(college1);
    const stream2 = getCollegeStream(college2);
    const matches = stream1.trim().toLowerCase() === stream2.trim().toLowerCase();
    const promptMsg = matches
        ? null
        : `Please select a college from the same stream (${college1.stream || stream1}) as your previously selected college.`;
    return {
        matches,
        stream1: college1.stream || stream1,
        stream2: college2.stream || stream2,
        message: promptMsg
    };
}

export function showCompareStreamToast(message) {
    let container = document.getElementById("compareToastContainer");
    if (!container) {
        container = document.createElement("div");
        container.id = "compareToastContainer";
        container.className = "fixed bottom-5 right-5 z-[9999] flex flex-col gap-2 pointer-events-none max-w-md w-full px-4";
        document.body.appendChild(container);
    }
    const toast = document.createElement("div");
    toast.className = "p-4 rounded-xl shadow-2xl border text-xs sm:text-sm font-semibold flex items-center gap-3 transition-all duration-300 pointer-events-auto bg-slate-900/95 text-amber-300 border-amber-500/40 backdrop-blur-md";
    toast.innerHTML = `<span class="text-base sm:text-lg">⚠️</span><div class="flex-1">${message}</div>`;
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = "0";
        setTimeout(() => toast.remove(), 300);
    }, 4500);
}

export function canCompareColleges(college1, college2) {
    if (!college1 || !college2) return true;
    const stream1 = college1.stream || getInferredStream(college1);
    const stream2 = college2.stream || getInferredStream(college2);
    if (!stream1 || !stream2) return true;
    if (stream1.trim().toLowerCase() !== stream2.trim().toLowerCase()) {
        const streamDisplay = college1.stream || stream1;
        const message = `Please select a college from the same stream (${streamDisplay}) as your previously selected college.`;
        showCompareStreamToast(message);
        alert(message);
        return false;
    }
    return true;
}

// Global attachment for seamless interop across Web Application
window.db = db;
window.STREAM_COLLECTIONS = STREAM_COLLECTIONS;
window.fetchColleges = fetchColleges;
window.renderColleges = renderColleges;
window.renderCollegeCard = renderCollegeCard;
window.loadAndRenderColleges = loadAndRenderColleges;
window.validateCollegeStreams = validateCollegeStreams;
window.canCompareColleges = canCompareColleges;
window.showCompareStreamToast = showCompareStreamToast;
window.getCollegeStream = getCollegeStream;

// Helper function to hide the auth/login container and display main app
export function showMainCollegeApp() {
    const authWrapper = document.getElementById("authContainer") || document.getElementById("authScreen");
    if (authWrapper) {
        authWrapper.style.setProperty("display", "none", "important");
        authWrapper.classList.add("hidden");
    }

    const mainApp = document.getElementById("mainApp") || document.querySelector("main");
    if (mainApp) {
        mainApp.classList.remove("hidden");
        mainApp.style.display = "block";
    }
}

window.showMainCollegeApp = showMainCollegeApp;

document.addEventListener("DOMContentLoaded", () => {
    console.log("Find My College Web Application Initializing...");

    // Initialize UI components, Auth Modal & Bookmarks Badge
    initAuthModal();
    initNavDrawer();
    initProfileDrawer();
    initCollegeDetailModal();
    initLogoutButtons();
    updateBookmarkBadges();
    initChatWidget();
    initStreamFilter();

    // Setup clear search input (✕) button listeners
    setupGlobalSearchClearButtons();

    // Show main app by default for guest public browsing
    showMainCollegeApp();

    // Pre-initialize compare screen data
    initCompareScreen();

    // If active page is colleges, trigger initial load for selected stream
    if (localStorage.getItem("activePage") === "colleges" || window.location.hash === "#colleges") {
        loadAndRenderColleges(selectedStream);
    }

    // Listen for authentication state changes
    initAuthListener((user) => {
        if (user) {
            console.log("Logged-in user detected:", user.email);
            sessionStorage.removeItem('isGuest');
            closeAuthModal();
            showMainCollegeApp();
            updateProfileUI(user);
        } else {
            console.log("User state: Logged out / Guest session.");
            sessionStorage.setItem('isGuest', 'true');
            updateProfileUI(null);

            // Automatically close drawers on logout
            const profileDrawer = document.getElementById("profileDrawer");
            const navDrawer = document.getElementById("navDrawer");
            if (profileDrawer) profileDrawer.classList.add("hidden");
            if (navDrawer) navDrawer.classList.add("hidden");

            // Only show auth modal if explicitly requested via #signin hash
            if (window.location.hash === "#signin") {
                if (window.history.replaceState) {
                    window.history.replaceState(null, null, window.location.pathname);
                } else {
                    window.location.hash = "";
                }
                openAuthModal("🔒 Please sign in to access your student profile and bookmarks.", "signin");
            } else {
                closeAuthModal();
                showMainCollegeApp();
            }
        }
    });

    // Handle 'Continue as Guest' button clicks
    document.addEventListener("click", (e) => {
        if (e.target && e.target.textContent && e.target.textContent.toLowerCase().includes("continue as guest")) {
            e.preventDefault();
            sessionStorage.setItem('isGuest', 'true');
            closeAuthModal();
            showMainCollegeApp();
        }
    });

    const guestBtn = document.getElementById("guestBtn");
    if (guestBtn) {
        guestBtn.addEventListener("click", (e) => {
            e.preventDefault();
            sessionStorage.setItem('isGuest', 'true');
            closeAuthModal();
            showMainCollegeApp();
        });
    }

    // Fetch and render home overview screen
    if (!localStorage.getItem("activePage") || localStorage.getItem("activePage") === "home") {
        initHomeScreen((screen) => {
            if (window.openCollegeAppScreen) window.openCollegeAppScreen(screen);
        });
    }
});

function setupGlobalSearchClearButtons() {
    const inputs = document.querySelectorAll("#searchInput, #heroSearchInput");
    inputs.forEach(input => {
        const container = input.parentElement;
        if (!container) return;
        const clearBtn = container.querySelector("#search-clear-btn, .search-clear-btn, #clearSearchInputBtn, #clearHeroSearchBtn");
        if (!clearBtn) return;

        const toggleClearBtn = () => {
            if (input.value.length > 0) {
                clearBtn.style.display = "block";
                clearBtn.classList.remove("hidden");
            } else {
                clearBtn.style.display = "none";
                clearBtn.classList.add("hidden");
            }
        };

        input.addEventListener("input", toggleClearBtn);
        toggleClearBtn();

        clearBtn.addEventListener("click", (e) => {
            e.preventDefault();
            input.value = "";
            clearBtn.style.display = "none";
            clearBtn.classList.add("hidden");
            input.focus();
            input.dispatchEvent(new Event("input", { bubbles: true }));
            input.dispatchEvent(new Event("change", { bubbles: true }));
        });
    });
}