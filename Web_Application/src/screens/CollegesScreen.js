import { fetchColleges } from "../services/collegeService.js";
import { createCollegeCard } from "../components/cards/CollegeCard.js";
import { filterColleges } from "../utils/searchFilter.js";

let allColleges = [];
let selectedStream = "All";

export function getInferredStream(college) {
    const rawStream = college.stream || college.Stream;
    if (rawStream && typeof rawStream === 'string' && rawStream.trim().length > 0) {
        return rawStream.trim();
    }
    const name = String(college.Name || college.name || "").toLowerCase();
    if (name.includes("dental")) return "Dental";
    if (name.includes("law")) return "Law";
    if (name.includes("management") || name.includes("iim") || name.includes("business")) return "Management";
    if (name.includes("technology") || name.includes("engineering") || name.includes("iit") || name.includes("nit")) return "Engineering";
    return "Engineering";
}

export function getSelectedStream() {
    return selectedStream;
}

export function updateStateFilterOptions(colleges) {
    const stateFilterSelect = document.getElementById("stateFilterSelect");
    if (!stateFilterSelect) return;
    const currentState = stateFilterSelect.value;
    const states = [...new Set(colleges.map(c => c.state || c.State).filter(s => typeof s === 'string' && s.trim().length > 0))].sort();
    stateFilterSelect.innerHTML = '<option value="">All States</option>';
    states.forEach(st => {
        const opt = document.createElement("option");
        opt.value = st;
        opt.textContent = st;
        if (st === currentState) opt.selected = true;
        stateFilterSelect.appendChild(opt);
    });
}

export async function setSelectedStream(stream) {
    selectedStream = stream || "All";
    const loader = document.getElementById("loader");
    const grid = document.getElementById("collegesGrid");
    if (loader) {
        loader.classList.remove("hidden");
        loader.style.display = "flex";
    }

    try {
        allColleges = await fetchColleges(selectedStream);
        updateStateFilterOptions(allColleges);
    } catch (err) {
        console.error("Error fetching colleges for stream:", stream, err);
    } finally {
        if (loader) {
            loader.classList.add("hidden");
            loader.style.display = "none";
        }
        if (grid) {
            grid.classList.remove("hidden");
        }
        if (typeof window.reapplyCollegesFilter === "function") {
            window.reapplyCollegesFilter();
        }
    }
}

export async function initCollegesScreen() {
    console.log("Initializing CollegesScreen...");

    const loader = document.getElementById("loader");
    const searchInput = document.getElementById("searchInput");
    const stateFilterSelect = document.getElementById("stateFilterSelect");
    const collegeCountBadge = document.getElementById("collegeCount");
    let grid = document.getElementById("collegesGrid");

    if (!grid) return;

    try {
        if (allColleges.length === 0) {
            allColleges = await fetchColleges(selectedStream);
        }

        // Hide loader
        if (loader) {
            loader.classList.add("hidden");
            loader.style.display = "none";
        }

        // Populate State Filter options dynamically with All States default
        updateStateFilterOptions(allColleges);

        // Grid visible
        grid.classList.remove("hidden");

        function applyFilterAndRender() {
            const query = searchInput ? searchInput.value : "";
            const selectedState = stateFilterSelect ? stateFilterSelect.value : "";

            let filtered = filterColleges(allColleges, query);

            if (selectedStream && selectedStream !== "All") {
                const s = selectedStream.toLowerCase();
                filtered = filtered.filter(c => getInferredStream(c).toLowerCase() === s);
            }

            if (selectedState) {
                filtered = filtered.filter(c => (c.State || c.state) === selectedState);
            }

            grid.innerHTML = "";

            if (!filtered || filtered.length === 0) {
                grid.innerHTML = `
                    <div class="col-span-full text-center py-16 bg-slate-800/40 rounded-2xl border border-slate-700/50">
                        <span class="text-4xl block mb-2">🔍</span>
                        <p class="text-slate-300 font-semibold mb-1">No matching colleges found</p>
                        <p class="text-xs text-slate-500">Try adjusting search keywords, stream filter, or state filter</p>
                    </div>
                `;
            } else {
                filtered.forEach(col => {
                    const card = createCollegeCard(col);
                    grid.appendChild(card);
                });
            }

            if (collegeCountBadge) {
                collegeCountBadge.textContent = (query.trim() || selectedState || (selectedStream && selectedStream !== "All"))
                    ? `${filtered.length} of ${allColleges.length} Colleges`
                    : `${allColleges.length} Colleges`;
            }

            const titleEl = document.getElementById("collegeDirectoryTitle");
            if (titleEl) {
                titleEl.textContent = (selectedStream === "All") ? "NIRF Colleges Directory" : `NIRF ${selectedStream} Colleges Directory`;
            }
        }

        window.reapplyCollegesFilter = applyFilterAndRender;
        applyFilterAndRender();

        if (searchInput) {
            searchInput.oninput = () => applyFilterAndRender();
        }

        if (stateFilterSelect) {
            stateFilterSelect.onchange = () => applyFilterAndRender();
        }

    } catch (err) {
        console.error("CollegesScreen initialization error:", err);
        if (loader) {
            loader.innerHTML = `<p class="text-red-400 py-4 font-semibold text-center">Failed to load colleges directory.</p>`;
        }
    }
}
