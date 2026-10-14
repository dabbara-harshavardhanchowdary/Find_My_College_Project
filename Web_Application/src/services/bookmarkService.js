import { auth, db } from "../config/firebase-config.js";
import { doc, onSnapshot, setDoc, arrayUnion, arrayRemove } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

// LocalStorage-first Bookmark Service (Works 100% offline and online with Firestore real-time sync)
const STORAGE_KEY = 'find_my_college_bookmarks';
const ALT_STORAGE_KEY = 'findmycollege_bookmarks';

let activeFirestoreUnsubscribe = null;

// Get all bookmarked college IDs from LocalStorage
export function getBookmarkedIds() {
    try {
        const data = localStorage.getItem(STORAGE_KEY) || localStorage.getItem(ALT_STORAGE_KEY);
        return data ? JSON.parse(data) : [];
    } catch (e) {
        console.error('Error reading bookmarks:', e);
        return [];
    }
}

export const getBookmarks = getBookmarkedIds;

// Helper to save bookmarks to LocalStorage across both keys
function saveLocalBookmarks(list) {
    try {
        const json = JSON.stringify(list);
        localStorage.setItem(STORAGE_KEY, json);
        localStorage.setItem(ALT_STORAGE_KEY, json);
    } catch (e) {
        console.error('Error saving bookmarks:', e);
    }
}

// Check if a single college is bookmarked
export function isCollegeBookmarked(collegeId) {
    if (!collegeId) return false;
    const list = getBookmarkedIds();
    return list.includes(String(collegeId));
}

export const isBookmarked = isCollegeBookmarked;

// Add or remove a college ID
export function toggleCollegeBookmark(collegeId) {
    if (!collegeId) return false;
    const strId = String(collegeId);
    let list = getBookmarkedIds();
    let isNowBookmarked = false;

    if (list.includes(strId)) {
        list = list.filter(id => id !== strId);
        isNowBookmarked = false;
    } else {
        list.push(strId);
        isNowBookmarked = true;
    }

    saveLocalBookmarks(list);

    // Sync with Firestore in real time if user is authenticated
    const currentUser = auth?.currentUser;
    if (currentUser?.uid) {
        try {
            const userRef = doc(db, "users", currentUser.uid);
            setDoc(userRef, {
                bookmarks: isNowBookmarked ? arrayUnion(strId) : arrayRemove(strId),
                bookmarkedCollegeIds: isNowBookmarked ? arrayUnion(strId) : arrayRemove(strId)
            }, { merge: true }).catch(err => {
                console.warn("Could not sync bookmark to Firestore:", err);
            });
        } catch (err) {
            console.warn("Error initiating Firestore bookmark sync:", err);
        }
    }

    // Update badge counters across the app
    updateBookmarkBadges();

    // Fire events so all open views update automatically
    window.dispatchEvent(new CustomEvent('bookmarksUpdated', { detail: list }));
    window.dispatchEvent(new CustomEvent('bookmarksChanged', { detail: list }));

    return isNowBookmarked;
}

export const toggleBookmark = toggleCollegeBookmark;

// Update bookmark count badges on Header, Nav Drawer, Profile, and Bookmarks section
export function updateBookmarkBadges() {
    const list = getBookmarkedIds();
    const count = list.length;

    // 1. Header bookmark badge
    const headerBadge = document.getElementById("bookmarkCountBadge");
    if (headerBadge) {
        headerBadge.textContent = String(count);
    }

    // 2. Navigation drawer bookmark badge
    const navBadge = document.getElementById("navBookmarkBadge");
    if (navBadge) {
        navBadge.textContent = String(count);
    }

    // 3. User profile drawer bookmark badge
    const profileBadge = document.getElementById("profileBookmarksBadge");
    if (profileBadge) {
        profileBadge.textContent = `${count} Saved`;
    }

    // 4. Saved colleges section total label
    const totalLabel = document.getElementById("bookmarkTotalLabel");
    if (totalLabel) {
        totalLabel.textContent = `${count} Saved`;
    }
}

// Render Bookmarks View in the Bookmarks Section
export async function renderBookmarksView(colleges = []) {
    const grid = document.getElementById("bookmarkGrid");
    const totalLabel = document.getElementById("bookmarkTotalLabel");
    const bookmarkedIds = getBookmarkedIds();

    if (totalLabel) {
        totalLabel.textContent = `${bookmarkedIds.length} Saved`;
    }

    if (!grid) return;

    grid.innerHTML = "";

    if (!bookmarkedIds || bookmarkedIds.length === 0) {
        grid.innerHTML = `
            <div class="col-span-full text-center py-16 bg-slate-800/40 rounded-2xl border border-slate-700/50">
                <span class="text-4xl block mb-2">🔖</span>
                <p class="text-slate-300 font-semibold mb-1">No bookmarked colleges yet</p>
                <p class="text-xs text-slate-500">Explore colleges and tap the bookmark icon to save institutions to your shortlist.</p>
            </div>
        `;
        return;
    }

    try {
        const { createCollegeCard } = await import("../components/cards/CollegeCard.js");

        const bookmarkedColleges = (colleges || []).filter(col => {
            const instId = String(col["Institute ID"] || col.instituteId || col.id || "");
            return bookmarkedIds.includes(instId);
        });

        if (bookmarkedColleges.length === 0) {
            grid.innerHTML = `
                <div class="col-span-full text-center py-16 bg-slate-800/40 rounded-2xl border border-slate-700/50">
                    <span class="text-4xl block mb-2">🔖</span>
                    <p class="text-slate-300 font-semibold mb-1">Bookmarked colleges not found</p>
                    <p class="text-xs text-slate-500">No matching colleges found in the database for saved bookmarks.</p>
                </div>
            `;
            return;
        }

        bookmarkedColleges.forEach(col => {
            const card = createCollegeCard(col);
            grid.appendChild(card);
        });
    } catch (err) {
        console.error("Error rendering bookmarks view:", err);
    }
}

// Real-time Firestore bookmarks listener tied to user auth state
export function initFirestoreBookmarksListener(userOrUid) {
    // Unsubscribe from any prior listener
    if (activeFirestoreUnsubscribe) {
        activeFirestoreUnsubscribe();
        activeFirestoreUnsubscribe = null;
    }

    const uid = typeof userOrUid === "string" ? userOrUid : (userOrUid?.uid || null);

    if (!uid) {
        // User is logged out or browsing as guest: update UI from local storage
        updateBookmarkBadges();
        return;
    }

    try {
        const userRef = doc(db, "users", uid);
        activeFirestoreUnsubscribe = onSnapshot(userRef, (snapshot) => {
            if (snapshot.exists()) {
                const data = snapshot.data();
                const remoteBookmarks = data?.bookmarks || data?.bookmarkedCollegeIds || [];
                if (Array.isArray(remoteBookmarks)) {
                    const local = getBookmarkedIds();
                    const merged = Array.from(new Set([...local, ...remoteBookmarks.map(String)]));
                    saveLocalBookmarks(merged);
                    updateBookmarkBadges();
                    window.dispatchEvent(new CustomEvent('bookmarksUpdated', { detail: merged }));
                    window.dispatchEvent(new CustomEvent('bookmarksChanged', { detail: merged }));
                }
            } else {
                // If user document is new, push local bookmarks to Firestore
                const local = getBookmarkedIds();
                if (local.length > 0) {
                    setDoc(userRef, { bookmarks: local, bookmarkedCollegeIds: local }, { merge: true }).catch(console.warn);
                }
            }
        }, (err) => {
            console.warn("Firestore bookmarks listener error:", err);
            updateBookmarkBadges();
        });
    } catch (e) {
        console.warn("Could not initialize Firestore bookmarks listener:", e);
        updateBookmarkBadges();
    }
}

// Bundle for backward compatibility with window.bookmarkService
const bookmarkService = {
    getBookmarks,
    getBookmarkedIds,
    isBookmarked,
    isCollegeBookmarked,
    toggleBookmark,
    toggleCollegeBookmark,
    updateBookmarkBadges,
    renderBookmarksView,
    initFirestoreBookmarksListener
};

window.bookmarkService = bookmarkService;
export default bookmarkService;