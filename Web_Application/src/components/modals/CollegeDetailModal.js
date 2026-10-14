import { isCollegeBookmarked, toggleCollegeBookmark } from "../../services/bookmarkService.js";
import { auth } from "../../config/firebase-config.js";
import { openAuthModal } from "./AuthModal.js";
import { canUserReview, getStudentAffiliation } from "../../services/verificationService.js";
import { getReviewsForCollege, submitReview } from "../../services/reviewService.js";

export function initCollegeDetailModal() {
    document.addEventListener("click", (e) => {
        const btn = e.target.closest(".view-details-btn");
        if (btn) {
            const collegeCard = btn.closest("[data-college-data]");
            if (collegeCard) {
                try {
                    const data = JSON.parse(decodeURIComponent(collegeCard.dataset.collegeData));
                    openCollegeDetailModal(data);
                } catch (err) {
                    console.error("Error parsing college data:", err);
                }
            }
        }
    });

    const modal = document.getElementById("collegeDetailModal");
    const closeModalBtn = document.getElementById("closeCollegeModalBtn");
    const closeModalBtnBottom = document.getElementById("closeCollegeModalBtnBottom");

    const hideModal = () => {
        if (modal) modal.classList.add("hidden");
    };

    if (closeModalBtn) closeModalBtn.addEventListener("click", hideModal);
    if (closeModalBtnBottom) closeModalBtnBottom.addEventListener("click", hideModal);

    if (modal) {
        modal.addEventListener("click", (e) => {
            if (e.target === modal) hideModal();
        });
    }

    setupModalTabs();
}

export function openCollegeDetailModal(college) {
    const modal = document.getElementById("collegeDetailModal");
    if (!modal) return;

    const rank = college.nirfRank ?? college.Rank ?? college.rank ?? "N/A";
    const instId = college.instituteId || college["Institute ID"] || college.id || "N/A";
    const name = college.name || college.Name || "College Details";
    const city = college.city || college.City || "";
    const state = college.state || college.State || "";
    const locationStr = city && state ? `${city}, ${state}` : (city || state || "India");
    const score = college.score ?? college.Score ?? "N/A";
    const stream = college.stream || college.Stream || "Engineering";
    const description = college.description || `${name} is a premier ${stream.toLowerCase()} institution ranked #${rank} under the National Institutional Ranking Framework (NIRF).`;

    let isBookmarked = isCollegeBookmarked(instId);

    // Populate Headers & Bookmark Button
    const nameEl = document.getElementById("modalCollegeName");
    const locEl = document.getElementById("modalCollegeLocation");
    const rankEl = document.getElementById("modalCollegeRank");

    if (nameEl) nameEl.innerText = name;
    if (locEl) locEl.innerText = `📍 ${locationStr}`;
    if (rankEl) rankEl.innerText = `#${rank} NIRF Rank`;

    // Ensure Modal Header includes Bookmark Toggle Action
    let modalBookmarkBtn = document.getElementById("modalBookmarkBtn");
    if (!modalBookmarkBtn) {
        const headerContainer = nameEl ? nameEl.parentElement : modal.querySelector(".mb-5");
        if (headerContainer) {
            modalBookmarkBtn = document.createElement("button");
            modalBookmarkBtn.id = "modalBookmarkBtn";
            headerContainer.appendChild(modalBookmarkBtn);
        }
    }

    function updateModalBookmarkBtnUI() {
        if (!modalBookmarkBtn) return;
        modalBookmarkBtn.className = `w-full mt-3 py-2 px-4 rounded-xl border text-xs font-bold transition flex items-center justify-center gap-2 ${
            isBookmarked 
                ? 'bg-amber-500/20 text-amber-400 border-amber-500/40 hover:bg-amber-500/30' 
                : 'bg-slate-800 text-slate-300 border-slate-700 hover:text-white hover:border-slate-600'
        }`;
        modalBookmarkBtn.innerHTML = isBookmarked ? `<span>🔖</span> Bookmarked` : `<span>🔖</span> Bookmark College`;
    }

    updateModalBookmarkBtnUI();

    if (modalBookmarkBtn) {
        modalBookmarkBtn.onclick = () => {
            if (!auth.currentUser) {
                openAuthModal("🔒 Unlock Your Personal Experience: Register or Sign In to save shortlists and sync across devices.");
                return;
            }

            isBookmarked = toggleCollegeBookmark(instId);
            updateModalBookmarkBtnUI();
        };
    }

    // Extract metrics and report link safely
    const m = college.nirfMetrics || {};
    const tlr = m.tlr !== undefined ? m.tlr : (college.tlr || 0);
    const rpc = m.rpc !== undefined ? m.rpc : (college.rpc || 0);
    const go  = m.go  !== undefined ? m.go  : (college.go  || 0);
    const oi  = m.oi  !== undefined ? m.oi  : (college.oi  || 0);
    const pr  = m.perception !== undefined ? m.perception : (college.perception || college.pr || 0);

    const pdfUrl = college.nirfReportUrl || college.reportUrl || null;

    // Populate Tab Contents
    const overviewTab = document.getElementById("tabContentOverview");
    if (overviewTab) {
        overviewTab.innerHTML = `
            <div class="space-y-4">
                <div class="grid grid-cols-2 gap-3 text-xs">
                    <div class="bg-slate-950 p-3 rounded-xl border border-slate-800">
                        <span class="text-slate-500 block mb-1">NIRF Rank</span>
                        <span class="text-blue-400 font-bold text-base">#${rank}</span>
                    </div>
                    <div class="bg-slate-950 p-3 rounded-xl border border-slate-800">
                        <span class="text-slate-500 block mb-1">NIRF Score</span>
                        <span class="text-emerald-400 font-bold text-base">⭐ ${score}</span>
                    </div>
                    <div class="bg-slate-950 p-3 rounded-xl border border-slate-800 col-span-2">
                        <span class="text-slate-500 block mb-1">Institute ID</span>
                        <span class="text-white font-mono font-bold text-sm">${instId}</span>
                    </div>
                    <div class="bg-slate-950 p-3 rounded-xl border border-slate-800 col-span-2">
                        <span class="text-slate-500 block mb-1">Location</span>
                        <span class="text-white font-bold text-sm">📍 ${locationStr}</span>
                    </div>
                </div>

                <!-- 🏛️ Official NIRF Parameter Breakdown (MoE Audited) -->
                <div class="bg-slate-950 p-4 rounded-xl border border-blue-500/30 space-y-3">
                    <div class="flex items-center justify-between border-b border-slate-800/80 pb-2">
                        <h4 class="font-bold text-slate-200 text-xs flex items-center gap-1.5">
                            <span>🏛️</span> Official NIRF Parameter Breakdown (MoE Audited)
                        </h4>
                        <span class="text-[10px] text-slate-400 font-mono">Institute ID: ${college.instituteId || instId || 'N/A'}</span>
                    </div>

                    <div class="space-y-2.5 text-xs">
                        <div>
                            <div class="flex justify-between text-[11px] mb-1">
                                <span class="text-slate-300 font-medium">1. Teaching, Learning & Resources (TLR):</span>
                                <span class="text-blue-400 font-bold">${tlr} / 100</span>
                            </div>
                            <div class="w-full bg-slate-900 h-2 rounded-full overflow-hidden border border-slate-800">
                                <div class="bg-blue-500 h-full rounded-full transition-all duration-500" style="width: ${Math.min(100, Math.max(0, tlr))}%"></div>
                            </div>
                        </div>

                        <div>
                            <div class="flex justify-between text-[11px] mb-1">
                                <span class="text-slate-300 font-medium">2. Research & Professional Practice (RPC):</span>
                                <span class="text-emerald-400 font-bold">${rpc} / 100</span>
                            </div>
                            <div class="w-full bg-slate-900 h-2 rounded-full overflow-hidden border border-slate-800">
                                <div class="bg-emerald-500 h-full rounded-full transition-all duration-500" style="width: ${Math.min(100, Math.max(0, rpc))}%"></div>
                            </div>
                        </div>

                        <div>
                            <div class="flex justify-between text-[11px] mb-1">
                                <span class="text-slate-300 font-medium">3. Graduation Outcomes (GO):</span>
                                <span class="text-indigo-400 font-bold">${go} / 100</span>
                            </div>
                            <div class="w-full bg-slate-900 h-2 rounded-full overflow-hidden border border-slate-800">
                                <div class="bg-indigo-500 h-full rounded-full transition-all duration-500" style="width: ${Math.min(100, Math.max(0, go))}%"></div>
                            </div>
                        </div>

                        <div>
                            <div class="flex justify-between text-[11px] mb-1">
                                <span class="text-slate-300 font-medium">4. Outreach & Inclusivity (OI):</span>
                                <span class="text-amber-400 font-bold">${oi} / 100</span>
                            </div>
                            <div class="w-full bg-slate-900 h-2 rounded-full overflow-hidden border border-slate-800">
                                <div class="bg-amber-500 h-full rounded-full transition-all duration-500" style="width: ${Math.min(100, Math.max(0, oi))}%"></div>
                            </div>
                        </div>

                        <div>
                            <div class="flex justify-between text-[11px] mb-1">
                                <span class="text-slate-300 font-medium">5. Perception:</span>
                                <span class="text-purple-400 font-bold">${pr} / 100</span>
                            </div>
                            <div class="w-full bg-slate-900 h-2 rounded-full overflow-hidden border border-slate-800">
                                <div class="bg-purple-500 h-full rounded-full transition-all duration-500" style="width: ${Math.min(100, Math.max(0, pr))}%"></div>
                            </div>
                        </div>
                    </div>

                    ${college.nirfReportUrl ? `
                        <a href="${college.nirfReportUrl}" target="_blank" rel="noopener noreferrer" class="btn btn-pdf w-full py-2.5 px-4 bg-blue-600/20 hover:bg-blue-600/30 text-blue-300 border border-blue-500/40 rounded-xl font-bold text-xs transition flex items-center justify-center gap-2 mt-3 shadow-md">📄 View Official Signed NIRF Dossier (PDF)</a>
                    ` : (pdfUrl ? `
                        <a href="${pdfUrl}" target="_blank" rel="noopener noreferrer" class="btn btn-pdf w-full py-2.5 px-4 bg-blue-600/20 hover:bg-blue-600/30 text-blue-300 border border-blue-500/40 rounded-xl font-bold text-xs transition flex items-center justify-center gap-2 mt-3 shadow-md">📄 View Official Signed NIRF Dossier (PDF)</a>
                    ` : '')}
                </div>

                <p class="text-xs text-slate-300 leading-relaxed">${description}</p>
            </div>
        `;
    }

    const rawFee = college.fees || college.tuitionFee || college.btechFee;
    const formattedFee = rawFee 
        ? (String(rawFee).startsWith("≈") ? String(rawFee) : `≈ ${typeof rawFee === 'number' ? '₹' + rawFee.toLocaleString('en-IN') + '/year' : rawFee}`)
        : `≈ ₹${Number(220000).toLocaleString('en-IN')}/year`;

    const feesTab = document.getElementById("tabContentFees");
    if (feesTab) {
        feesTab.innerHTML = `
            <div class="space-y-3">
                <div class="flex justify-between items-center p-3 bg-slate-950 rounded-xl border border-slate-800 text-xs">
                    <span class="text-slate-300 font-medium">B.Tech / B.E. (Annual Fee)</span>
                    <span class="text-blue-400 font-bold">${formattedFee}</span>
                </div>
                <div class="flex justify-between items-center p-3 bg-slate-950 rounded-xl border border-slate-800 text-xs">
                    <span class="text-slate-300 font-medium">M.Tech (Annual Fee)</span>
                    <span class="text-blue-400 font-bold">${college.mtechFee || "≈ ₹1,10,000/year"}</span>
                </div>
                <div class="flex justify-between items-center p-3 bg-slate-950 rounded-xl border border-slate-800 text-xs">
                    <span class="text-slate-300 font-medium">Hostel & Mess Charges</span>
                    <span class="text-blue-400 font-bold">${college.hostelFee || "≈ ₹65,000/year"}</span>
                </div>
            </div>
        `;
    }

    const cutoffsTab = document.getElementById("tabContentCutoffs");
    if (cutoffsTab) {
        cutoffsTab.innerHTML = `
            <div class="space-y-3 text-xs">
                <p class="text-slate-400">Estimated Cutoff Ranks (General Category):</p>
                <div class="space-y-2">
                    <div class="flex justify-between p-2.5 bg-slate-950 rounded-lg border border-slate-800">
                        <span class="text-slate-300">Computer Science (CSE)</span>
                        <span class="text-emerald-400 font-bold">${college.cseCutoff || "Rank ~ 2,500"}</span>
                    </div>
                    <div class="flex justify-between p-2.5 bg-slate-950 rounded-lg border border-slate-800">
                        <span class="text-slate-300">Electronics (ECE)</span>
                        <span class="text-emerald-400 font-bold">${college.eceCutoff || "Rank ~ 5,200"}</span>
                    </div>
                    <div class="flex justify-between p-2.5 bg-slate-950 rounded-lg border border-slate-800">
                        <span class="text-slate-300">Mechanical (ME)</span>
                        <span class="text-emerald-400 font-bold">${college.meCutoff || "Rank ~ 9,800"}</span>
                    </div>
                </div>
            </div>
        `;
    }

    const placementsTab = document.getElementById("tabContentPlacements");
    if (placementsTab) {
        placementsTab.innerHTML = `
            <div class="grid grid-cols-2 gap-3 text-xs">
                <div class="bg-slate-950 p-4 rounded-xl border border-slate-800 text-center">
                    <span class="text-slate-400 block mb-1">Highest Package</span>
                    <span class="text-emerald-400 text-base font-black">${college.highestPackage || "₹52.5 LPA"}</span>
                </div>
                <div class="bg-slate-950 p-4 rounded-xl border border-slate-800 text-center">
                    <span class="text-slate-400 block mb-1">Average Package</span>
                    <span class="text-blue-400 text-base font-black">${college.avgPackage || "₹16.8 LPA"}</span>
                </div>
            </div>
        `;
    }

    // Populate Reviews Tab Content with Domain Verification Gating
    renderReviewsTab(college);

    // Reset Tabs & Show Modal
    switchTab("tabOverview");
    modal.classList.remove("hidden");
}

function renderReviewsTab(college) {
    const reviewsTab = document.getElementById("tabContentReviews");
    if (!reviewsTab) return;

    const userEmail = auth.currentUser ? auth.currentUser.email : null;
    const collegeId = college["Institute ID"] || college.instituteId || college.id || college.rank;
    const canReview = canUserReview(userEmail, college);
    const affiliation = getStudentAffiliation(userEmail);
    const existingReviews = getReviewsForCollege(collegeId);

    let reviewsHtml = `
        <div class="space-y-4 text-xs">
            <!-- Review Submission Header / Gating Card -->
            ${canReview ? `
                <div class="bg-slate-950 p-4 rounded-xl border border-emerald-500/40 space-y-3">
                    <div class="flex items-center justify-between">
                        <span class="font-bold text-slate-200 text-xs flex items-center gap-1.5">
                            <span>✍️</span> Write Student Review
                        </span>
                        <span class="bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 text-[10px] font-bold px-2.5 py-0.5 rounded-full">
                            🎓 Verified: ${affiliation.collegeName}
                        </span>
                    </div>

                    <form id="modalReviewForm" class="space-y-2">
                        <div class="flex items-center gap-2">
                            <span class="text-slate-400 font-medium text-[11px]">Rating:</span>
                            <select id="reviewRatingSelect" class="bg-slate-900 border border-slate-700 text-emerald-400 font-bold text-xs rounded-lg px-2 py-1 focus:outline-none">
                                <option value="5">⭐⭐⭐⭐⭐ (5/5)</option>
                                <option value="4">⭐⭐⭐⭐ (4/5)</option>
                                <option value="3">⭐⭐⭐ (3/5)</option>
                                <option value="2">⭐⭐ (2/5)</option>
                                <option value="1">⭐ (1/5)</option>
                            </select>
                        </div>
                        <textarea id="reviewCommentInput" required rows="2" placeholder="Share your authentic campus experience, academics, and placement insights..." class="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"></textarea>
                        <button type="submit" id="submitReviewBtn" class="w-full py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded-xl text-xs transition shadow-md shadow-emerald-600/20">
                            Post Verified Review
                        </button>
                    </form>
                    <p id="reviewFeedbackMsg" class="hidden text-emerald-400 text-center font-semibold text-[11px]"></p>
                </div>
            ` : `
                <div class="bg-slate-950 p-4 rounded-xl border border-amber-500/30 flex items-start gap-3">
                    <span class="text-xl">🔒</span>
                    <div>
                        <h4 class="font-bold text-amber-300 text-xs mb-1">Reviews Restricted to Enrolled Students</h4>
                        <p class="text-slate-400 text-[11px] leading-relaxed">
                            Reviews are restricted to enrolled students with an official university email for this campus.
                        </p>
                        ${!userEmail ? `
                            <button onclick="openAuthModal('🔒 Please sign in with your official university email to post reviews.')" class="mt-2 text-[11px] text-blue-400 hover:underline font-bold">
                                Sign in with official university email →
                            </button>
                        ` : `
                            <span class="inline-block mt-2 text-[10px] text-slate-500 font-mono">
                                Signed in as: ${userEmail}
                            </span>
                        `}
                    </div>
                </div>
            `}

            <!-- Existing Verified Student Reviews Feed -->
            <div class="pt-2">
                <h4 class="font-extrabold text-slate-300 text-xs mb-2 flex items-center justify-between">
                    <span>💬 Student Reviews (${existingReviews.length})</span>
                    <span class="text-[10px] text-slate-400 font-normal">Verified Domain Reviews</span>
                </h4>

                <div class="space-y-2.5 max-h-52 overflow-y-auto pr-1">
                    ${existingReviews.length === 0 ? `
                        <div class="p-4 bg-slate-950 rounded-xl border border-slate-800 text-center text-slate-500 text-xs">
                            No reviews posted yet for this institution.
                        </div>
                    ` : existingReviews.map(r => `
                        <div class="p-3 bg-slate-950 rounded-xl border border-slate-800/80 space-y-1.5">
                            <div class="flex items-center justify-between">
                                <div class="flex items-center gap-2">
                                    <span class="font-bold text-white text-xs">${r.authorName}</span>
                                    <span class="bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-[9px] font-bold px-2 py-0.5 rounded-full truncate max-w-[200px]" title="${r.verificationTag}">
                                        🎓 ${r.verificationTag}
                                    </span>
                                </div>
                                <span class="text-amber-400 font-bold text-xs">${'★'.repeat(r.rating)}</span>
                            </div>
                            <p class="text-slate-300 text-[11px] leading-relaxed">${r.comment}</p>
                            <span class="text-[10px] text-slate-500 block text-right">${r.date}</span>
                        </div>
                    `).join('')}
                </div>
            </div>
        </div>
    `;

    reviewsTab.innerHTML = reviewsHtml;

    // Attach Review Form Submit Event Listener if active
    const reviewForm = document.getElementById("modalReviewForm");
    if (reviewForm) {
        reviewForm.addEventListener("submit", (e) => {
            e.preventDefault();
            const ratingSelect = document.getElementById("reviewRatingSelect");
            const commentInput = document.getElementById("reviewCommentInput");
            const feedbackMsg = document.getElementById("reviewFeedbackMsg");
            const submitBtn = document.getElementById("submitReviewBtn");

            const rating = Number(ratingSelect ? ratingSelect.value : 5);
            const comment = commentInput ? commentInput.value.trim() : "";

            if (!comment) return;

            try {
                if (submitBtn) submitBtn.disabled = true;
                // Execute backend guarded review submission
                submitReview(collegeId, { rating, comment });

                if (feedbackMsg) {
                    feedbackMsg.innerText = "Review posted successfully! ✅";
                    feedbackMsg.classList.remove("hidden");
                }

                // Refresh reviews list view
                setTimeout(() => renderReviewsTab(college), 1200);
            } catch (err) {
                alert(err.message);
                if (submitBtn) submitBtn.disabled = false;
            }
        });
    }
}

export const openCollegeModal = openCollegeDetailModal;

export function setupModalTabs() {
    const tabs = ["tabOverview", "tabFees", "tabCutoffs", "tabPlacements", "tabReviews"];
    tabs.forEach(tabId => {
        const btn = document.getElementById(tabId);
        if (btn) {
            btn.onclick = () => switchTab(tabId);
        }
    });
}

function switchTab(selectedTabId) {
    const tabMap = {
        tabOverview: "tabContentOverview",
        tabFees: "tabContentFees",
        tabCutoffs: "tabContentCutoffs",
        tabPlacements: "tabContentPlacements",
        tabReviews: "tabContentReviews"
    };

    Object.keys(tabMap).forEach(tabId => {
        const btn = document.getElementById(tabId);
        const content = document.getElementById(tabMap[tabId]);

        if (tabId === selectedTabId) {
            if (btn) btn.className = "w-1/5 py-2 px-2 text-xs font-bold rounded-lg bg-blue-600 text-white transition";
            if (content) content.classList.remove("hidden");
        } else {
            if (btn) btn.className = "w-1/5 py-2 px-2 text-xs font-bold rounded-lg text-slate-400 hover:text-white transition";
            if (content) content.classList.add("hidden");
        }
    });
}