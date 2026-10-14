import { fetchColleges } from "../services/collegeService.js";
import { createCollegeCard } from "../components/cards/CollegeCard.js";

let allColleges = [];

export async function initHomeScreen(onNavigate) {
    console.log("Initializing HomeScreen...");

    const top3Grid = document.getElementById("top3CollegesGrid");
    const heroSearchInput = document.getElementById("heroSearchInput");
    const heroSearchBtn = document.getElementById("heroSearchBtn");
    const clearHeroSearchBtn = document.getElementById("clearHeroSearchBtn");
    const exploreCtaBtn = document.getElementById("exploreCollegesCtaBtn");
    const homeCompareBtn = document.getElementById("homeCompareBtn");
    const homeBookmarksBtn = document.getElementById("homeBookmarksBtn");

    try {
        if (allColleges.length === 0) {
            allColleges = await fetchColleges();
        }

        // Dynamic Total Colleges Count update
        const totalCountElement = document.getElementById("totalCollegesCount") || document.querySelector(".college-count-badge");
        if (totalCountElement && allColleges.length > 0) {
            totalCountElement.textContent = `${allColleges.length}+ Verified Institutions`;
        }

        const collegeCountBadge = document.getElementById("collegeCount");
        if (collegeCountBadge && allColleges.length > 0) {
            collegeCountBadge.textContent = `${allColleges.length} Colleges`;
        }

        // Render Top 3 Ranked Colleges
        if (top3Grid) {
            top3Grid.innerHTML = "";
            const sortedByRank = [...allColleges].sort((a, b) => (Number(a.Rank || a.rank || a.nirfRank) || 999) - (Number(b.Rank || b.rank || b.nirfRank) || 999));
            const top3 = sortedByRank.slice(0, 3);

            top3.forEach((col) => {
                const card = createCollegeCard(col);
                top3Grid.appendChild(card);
            });
        }

        // Attach action handlers for quick search & CTA buttons
        if (exploreCtaBtn) {
            exploreCtaBtn.onclick = () => {
                if (onNavigate) onNavigate("colleges");
            };
        }

        if (heroSearchInput) {
            const heroClearBtn = document.getElementById("search-clear-btn") || clearHeroSearchBtn;
            const toggleHeroClearBtn = () => {
                if (heroClearBtn) {
                    if (heroSearchInput.value.length > 0) {
                        heroClearBtn.style.display = "block";
                        heroClearBtn.classList.remove("hidden");
                    } else {
                        heroClearBtn.style.display = "none";
                        heroClearBtn.classList.add("hidden");
                    }
                }
            };

            heroSearchInput.addEventListener("input", toggleHeroClearBtn);
            toggleHeroClearBtn();

            if (heroClearBtn) {
                heroClearBtn.onclick = () => {
                    heroSearchInput.value = "";
                    heroClearBtn.style.display = "none";
                    heroClearBtn.classList.add("hidden");
                    heroSearchInput.focus();
                };
            }

            const handleHeroSearch = () => {
                const q = heroSearchInput.value.trim();
                const mainSearch = document.getElementById("searchInput");
                if (mainSearch) {
                    mainSearch.value = q;
                    mainSearch.dispatchEvent(new Event("input", { bubbles: true }));
                }
                if (onNavigate) onNavigate("colleges");
            };

            if (heroSearchBtn) heroSearchBtn.onclick = handleHeroSearch;
            heroSearchInput.onkeypress = (e) => {
                if (e.key === "Enter") handleHeroSearch();
            };
        }

        if (homeCompareBtn) {
            homeCompareBtn.onclick = () => {
                if (onNavigate) onNavigate("compare");
            };
        }

        if (homeBookmarksBtn) {
            homeBookmarksBtn.onclick = () => {
                if (onNavigate) onNavigate("bookmarks");
            };
        }

    } catch (err) {
        console.error("HomeScreen initialization error:", err);
    }
}