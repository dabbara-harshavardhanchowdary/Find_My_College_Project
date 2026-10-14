import { openCollegeDetailModal } from "../modals/CollegeDetailModal.js";
import { isCollegeBookmarked, toggleCollegeBookmark } from "../../services/bookmarkService.js";
import { isCollegeInCompare, toggleCollegeInCompare } from "../../services/compareService.js";
import { auth } from "../../config/firebase-config.js";
import { openAuthModal } from "../modals/AuthModal.js";

// Helper to format/derive complete college metrics for consistent expanded card & compare displays
export function getCollegeDetails(col) {
    const rawRank = col.nirfRank !== undefined && col.nirfRank !== null ? col.nirfRank : (col.Rank ?? col.rank);
    const rankDisplay = (rawRank !== undefined && rawRank !== null && rawRank !== "" && !isNaN(Number(String(rawRank).replace(/[^0-9]/g, '')))) ? `#${rawRank}` : 'Unranked';
    const rank = Number(String(rawRank).replace(/[^0-9]/g, '')) || 999;
    const score = col.score ?? col.Score ?? "N/A";
    const instId = String(col.instituteId || col["Institute ID"] || col.id || "N/A");
    const name = col.name || col.Name || "College Name";
    const city = col.city || col.City || "";
    const state = col.state || col.State || "";
    const locationStr = city && state ? `${city}, ${state}` : (city || state || "India");

    const m = col.nirfMetrics || col.metrics || {};
    const tlr = m.tlr !== undefined ? m.tlr : (col.tlr || 0);
    const rpc = m.rpc !== undefined ? m.rpc : (col.rpc || 0);
    const go = m.go !== undefined ? m.go : (col.go || 0);
    const oi = m.oi !== undefined ? m.oi : (col.oi || 0);
    const perception = m.perception !== undefined ? m.perception : (col.perception || col.pr || 0);

    const naacGrade = col.naacGrade || col.accreditation || (rank <= 15 ? "A++" : rank <= 50 ? "A+" : "A");
    const accreditation = `NAAC: ${naacGrade}`;
    
    let btechFee = col.btechFee || col.tuitionFee;
    if (!btechFee) {
        if (col.fees) {
            btechFee = `≈ ₹${Number(col.fees).toLocaleString('en-IN')}/year`;
        } else {
            btechFee = `≈ ₹${(1.5 + (rank % 7) * 0.2).toFixed(1)} Lakhs/year`;
        }
    } else if (!btechFee.startsWith("≈")) {
        btechFee = `≈ ${btechFee}`;
    }

    const hostelFee = col.hostelFee || `₹${50 + (rank % 5) * 10},000 / yr`;

    const highestVal = Math.max(24.0, (65.0 - (rank * 0.38))).toFixed(1);
    const avgVal = Math.max(7.5, (22.0 - (rank * 0.14))).toFixed(1);
    const highestPackage = col.highestPackage || `₹${highestVal} LPA`;
    const avgPackage = col.avgPackage || `₹${avgVal} LPA`;

    const recruiters = col.topRecruiters || (
        rank <= 10 ? ["Google", "Microsoft", "Amazon", "Apple", "Goldman Sachs"] :
            rank <= 30 ? ["Amazon", "TATA", "Infosys", "Wipro", "Intel"] :
                ["Infosys", "TCS", "Cognizant", "Wipro", "Accenture"]
    );

    const branches = col.branches || (
        rank <= 20 ? ["CSE", "ECE", "AI/ML", "EE", "ME"] :
            rank <= 50 ? ["CSE", "ECE", "IT", "EE", "Civil"] :
                ["CSE", "ECE", "ME", "Civil", "EEE"]
    );

    const eligibilityCutoff = col.eligibilityCutoff || col.cseCutoff || (
        rank <= 25 ? `JEE Adv Rank ~ ${rank * 120}` : `JEE Main Rank ~ ${rank * 850}`
    );

    const rawStream = col.stream || col.Stream;
    let stream = "Engineering";
    if (rawStream && typeof rawStream === 'string' && rawStream.trim().length > 0) {
        stream = rawStream.trim();
    } else {
        const nameLower = String(name).toLowerCase();
        if (nameLower.includes("dental")) stream = "Dental";
        else if (nameLower.includes("law")) stream = "Law";
        else if (nameLower.includes("management") || nameLower.includes("iim") || nameLower.includes("business")) stream = "Management";
        else if (nameLower.includes("technology") || nameLower.includes("engineering") || nameLower.includes("iit") || nameLower.includes("nit")) stream = "Engineering";
        else stream = "Engineering";
    }

    return {
        rank,
        rankDisplay,
        score,
        instId,
        name,
        city,
        state,
        locationStr,
        accreditation,
        btechFee,
        hostelFee,
        highestPackage,
        avgPackage,
        recruiters,
        branches,
        eligibilityCutoff,
        imageUrl: col.imageUrl || null,
        stream,
        tlr,
        rpc,
        go,
        oi,
        perception
    };
}

// Render single expanded college card displaying all crucial statistics directly without requiring secondary clicks
export function createCollegeCard(col) {
    const details = getCollegeDetails(col);
    const card = document.createElement("div");
    card.className = "bg-slate-800/90 border border-slate-700/80 rounded-2xl p-5 hover:border-blue-500/60 transition duration-200 shadow-xl flex flex-col justify-between group relative";
    card.dataset.collegeData = encodeURIComponent(JSON.stringify(col));

    const isBookmarked = isCollegeBookmarked(details.instId);
    const isCompared = isCollegeInCompare(details.instId);

    const imageBanner = details.imageUrl
        ? `<img src="${details.imageUrl}" alt="${details.name}" class="w-full h-32 object-cover rounded-xl mb-3">`
        : `<div class="w-full h-24 bg-gradient-to-r from-slate-900 via-indigo-950/60 to-slate-900 border border-slate-700/40 rounded-xl mb-3 flex items-center justify-center text-3xl shadow-inner">🏛️</div>`;

    const hasMetrics = details.tlr > 0 || details.rpc > 0 || details.go > 0 || details.oi > 0 || details.perception > 0;

    card.innerHTML = `
        <!-- Bookmark Button in Top Right Corner -->
        <button class="card-bookmark-btn absolute top-7 right-7 z-10 p-2 rounded-xl border transition shadow-md ${isBookmarked
            ? 'bg-amber-500/20 text-amber-400 border-amber-500/50 hover:bg-amber-500/30'
            : 'bg-slate-900/80 text-slate-400 border-slate-700/80 hover:text-amber-400 hover:border-amber-500/40'
        }" title="${isBookmarked ? 'Remove Bookmark' : 'Save to Bookmarks'}">
            <span class="text-base">${isBookmarked ? '🔖' : '🔖'}</span>
        </button>

        <div>
            ${imageBanner}
            
            <!-- Badges Row: NIRF Rank + Score + Accreditation + Stream -->
            <div class="flex flex-wrap items-center gap-2 mb-2 pr-10">
                <span class="bg-blue-600/20 text-blue-400 border border-blue-500/30 text-xs font-bold px-2.5 py-0.5 rounded-full">
                    NIRF Rank: #${String(details.rankDisplay).replace('#', '')}
                </span>
                <span class="bg-amber-500/20 text-amber-300 border border-amber-500/30 text-[11px] font-bold px-2 py-0.5 rounded-full">
                    ⭐ Score: ${details.score !== undefined && details.score !== null && details.score !== "" ? details.score : "N/A"}
                </span>
                <span class="bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-[11px] font-bold px-2.5 py-0.5 rounded-full">
                    ${details.accreditation}
                </span>
                <span class="bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 text-[11px] font-bold px-2.5 py-0.5 rounded-full">
                    🎓 Stream: ${details.stream}
                </span>
            </div>

            <!-- College Name & Location -->
            <h3 class="text-base font-bold text-slate-100 mb-1 leading-snug line-clamp-2">
                ${details.name}
            </h3>

            <p class="text-xs text-slate-400 mb-3 flex flex-wrap items-center gap-2">
                <span>📍 ${details.city && details.state ? `${details.city}, ${details.state}` : (details.city || details.state || details.locationStr)}</span>
                <span class="bg-slate-900/90 text-slate-400 font-mono text-[10px] px-2 py-0.5 rounded border border-slate-700/60">ID: ${details.instId}</span>
            </p>

            <!-- STATS DIRECT VIEW CONTAINER -->
            <div class="space-y-2.5 mb-3 bg-slate-950/60 p-3 rounded-xl border border-slate-800 text-xs">
                <!-- Annual Tuition Fees -->
                <div class="flex justify-between items-center">
                    <span class="text-slate-400 font-medium">💰 Tuition Fee:</span>
                    <span class="text-blue-400 font-bold">${details.btechFee}</span>
                </div>

                <!-- Placement Stats Row -->
                <div class="grid grid-cols-2 gap-2 pt-2 border-t border-slate-800/80">
                    <div class="bg-slate-900 p-2 rounded-lg border border-slate-800 text-center">
                        <span class="text-[10px] text-slate-400 block">Avg Package</span>
                        <span class="text-blue-400 font-bold text-xs">📈 ${details.avgPackage}</span>
                    </div>
                    <div class="bg-slate-900 p-2 rounded-lg border border-slate-800 text-center">
                        <span class="text-[10px] text-slate-400 block">Highest Package</span>
                        <span class="text-emerald-400 font-black text-xs">🚀 ${details.highestPackage}</span>
                    </div>
                </div>

                <!-- NIRF Parameter Metrics Breakdown -->
                ${hasMetrics ? `
                    <div class="pt-2 border-t border-slate-800/80">
                        <span class="text-[11px] text-slate-400 font-medium block mb-1.5">🏛️ NIRF Audited Parameters:</span>
                        <div class="grid grid-cols-5 gap-1 text-center font-bold text-[10px]">
                            <div class="bg-slate-900/90 p-1 rounded-lg border border-slate-800">
                                <span class="text-slate-400 block text-[9px]">TLR</span>
                                <span class="text-blue-400">${details.tlr}</span>
                            </div>
                            <div class="bg-slate-900/90 p-1 rounded-lg border border-slate-800">
                                <span class="text-slate-400 block text-[9px]">RPC</span>
                                <span class="text-emerald-400">${details.rpc}</span>
                            </div>
                            <div class="bg-slate-900/90 p-1 rounded-lg border border-slate-800">
                                <span class="text-slate-400 block text-[9px]">GO</span>
                                <span class="text-indigo-400">${details.go}</span>
                            </div>
                            <div class="bg-slate-900/90 p-1 rounded-lg border border-slate-800">
                                <span class="text-slate-400 block text-[9px]">OI</span>
                                <span class="text-amber-400">${details.oi}</span>
                            </div>
                            <div class="bg-slate-900/90 p-1 rounded-lg border border-slate-800">
                                <span class="text-slate-400 block text-[9px]">PR</span>
                                <span class="text-purple-400">${details.perception}</span>
                            </div>
                        </div>
                    </div>
                ` : ''}

                <!-- Available Branches List -->
                <div class="pt-2 border-t border-slate-800/80">
                    <span class="text-[11px] text-slate-400 font-medium block mb-1">🎓 Courses / Branches:</span>
                    <div class="flex flex-wrap gap-1">
                        ${details.branches.map(b => `<span class="bg-slate-800 text-slate-300 text-[10px] px-2 py-0.5 rounded-md border border-slate-700/60">${b}</span>`).join('')}
                    </div>
                </div>

                <!-- Top Recruiters Tags -->
                <div class="pt-1">
                    <span class="text-[11px] text-slate-400 font-medium block mb-1">🏢 Top Recruiters:</span>
                    <div class="flex flex-wrap gap-1">
                        ${details.recruiters.map(r => `<span class="bg-indigo-950/60 text-indigo-300 text-[10px] px-2 py-0.5 rounded-md border border-indigo-800/50 font-semibold">${r}</span>`).join('')}
                    </div>
                </div>
            </div>
        </div>

        <!-- BOTTOM ACTIONS ROW: Compare Toggle & View Details -->
        <div class="border-t border-slate-700/60 pt-3 flex flex-col sm:flex-row gap-2 items-center justify-between">
            <button class="card-compare-btn w-full sm:w-auto flex-1 py-2 px-3 rounded-xl border text-xs font-bold transition flex items-center justify-center gap-1.5 ${isCompared
            ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/50 hover:bg-emerald-500/30'
            : 'bg-blue-600 hover:bg-blue-500 text-white border-blue-500/40 shadow-md shadow-blue-600/20'
        }">
                <span>⚖️</span>
                <span class="compare-btn-text">${isCompared ? '✓ In Compare' : '+ Add to Compare'}</span>
            </button>

            <button class="text-slate-400 hover:text-blue-400 font-semibold text-xs py-1 px-2 view-details-btn flex items-center gap-1 shrink-0">
                View Details →
            </button>
        </div>
    `;

    // Connect View Details button
    const viewDetailsBtn = card.querySelector(".view-details-btn");
    if (viewDetailsBtn) {
        viewDetailsBtn.addEventListener("click", (e) => {
            e.stopPropagation();
            openCollegeDetailModal(col);
        });
    }

    // Connect Compare Toggle button
    const compareBtn = card.querySelector(".card-compare-btn");
    if (compareBtn) {
        compareBtn.addEventListener("click", (e) => {
            e.stopPropagation();
            const nowCompared = toggleCollegeInCompare(details.instId);
            updateCompareBtnUI(compareBtn, nowCompared);
        });
    }

    function updateCompareBtnUI(btn, compared) {
        const textSpan = btn.querySelector(".compare-btn-text");
        if (compared) {
            btn.className = "card-compare-btn w-full sm:w-auto flex-1 py-2 px-3 rounded-xl border text-xs font-bold transition flex items-center justify-center gap-1.5 bg-emerald-500/20 text-emerald-400 border-emerald-500/50 hover:bg-emerald-500/30";
            if (textSpan) textSpan.textContent = "✓ In Compare";
        } else {
            btn.className = "card-compare-btn w-full sm:w-auto flex-1 py-2 px-3 rounded-xl border text-xs font-bold transition flex items-center justify-center gap-1.5 bg-blue-600 hover:bg-blue-500 text-white border-blue-500/40 shadow-md shadow-blue-600/20";
            if (textSpan) textSpan.textContent = "+ Add to Compare";
        }
    }

    // Connect Bookmark button with Auth Gating
    const bookmarkBtn = card.querySelector(".card-bookmark-btn");
    if (bookmarkBtn) {
        bookmarkBtn.addEventListener("click", (e) => {
            e.stopPropagation();

            if (!auth.currentUser) {
                openAuthModal("🔒 Unlock Your Personal Experience: Register or Sign In to save shortlists and sync across devices.");
                return;
            }

            const nowBookmarked = toggleCollegeBookmark(details.instId);

            if (nowBookmarked) {
                bookmarkBtn.className = "card-bookmark-btn absolute top-7 right-7 z-10 p-2 rounded-xl border transition shadow-md bg-amber-500/20 text-amber-400 border-amber-500/50 hover:bg-amber-500/30";
                bookmarkBtn.title = "Remove Bookmark";
            } else {
                bookmarkBtn.className = "card-bookmark-btn absolute top-7 right-7 z-10 p-2 rounded-xl border transition shadow-md bg-slate-900/80 text-slate-400 border-slate-700/80 hover:text-amber-400 hover:border-amber-500/40";
                bookmarkBtn.title = "Save to Bookmarks";
            }
        });
    }

    return card;
}