import { fetchColleges } from "../services/collegeService.js";
import { getCollegeDetails } from "../components/cards/CollegeCard.js";
import { isCollegeBookmarked, toggleCollegeBookmark } from "../services/bookmarkService.js";
import { getCompareCollegeIds, setCompareCollegeIds, removeCollegeFromCompare } from "../services/compareService.js";
import { auth } from "../config/firebase-config.js";
import { openAuthModal } from "../components/modals/AuthModal.js";
import { getInferredStream } from "./CollegesScreen.js";

let allColleges = [];
let selectedCollegeIds = [null, null, null, null, null]; // Up to 5 slots
let searchQueries = ["", "", "", "", ""];
let activeDropdownIndex = null;

export async function initCompareScreen() {
    const compareSection = document.getElementById("compareSection");
    if (!compareSection) return;

    try {
        if (allColleges.length === 0) {
            allColleges = await fetchColleges("All");
        }
        window._allCollegesCache = allColleges;

        if (allColleges.length === 0) {
            compareSection.innerHTML = `<p class="text-red-400 py-8 text-center font-semibold">Unable to load colleges for comparison.</p>`;
            return;
        }

        // Sync initial slots from compareService (up to 5 slots)
        const serviceIds = getCompareCollegeIds();
        for (let i = 0; i < 5; i++) {
            selectedCollegeIds[i] = serviceIds[i] || null;
            if (selectedCollegeIds[i]) {
                const c = allColleges.find(col => String(col.instituteId || col["Institute ID"] || col.id || col.rank || col.Rank) === String(selectedCollegeIds[i]));
                if (c) searchQueries[i] = c.Name || c.name;
            } else {
                searchQueries[i] = "";
            }
        }

        renderCompareUI();
    } catch (err) {
        console.error("Error initializing CompareScreen:", err);
    }
}

function updateDropdownContent(index, query) {
    const compareSection = document.getElementById("compareSection");
    if (!compareSection) return;
    const drop = compareSection.querySelector(`#compareDropdown_${index}`);
    if (!drop) return;

    const currentId = selectedCollegeIds[index];
    const q = (query || "").toLowerCase().trim();
    const otherSelectedIds = selectedCollegeIds.filter((id, i) => i !== index && id).map(String);

    const referenceCollege = otherSelectedIds.length > 0
        ? allColleges.find(c => String(c.instituteId || c["Institute ID"] || c.id || c.rank || c.Rank) === String(otherSelectedIds[0]))
        : null;
    const requiredStream = referenceCollege ? (referenceCollege.stream || getInferredStream(referenceCollege)) : null;

    const filteredOptions = allColleges.filter(c => {
        const colId = String(c.instituteId || c["Institute ID"] || c.id || c.rank || c.Rank);
        if (otherSelectedIds.includes(colId)) return false;

        // Stream matching rule: restrict to same academic stream as reference college
        if (requiredStream) {
            const stream = c.stream || getInferredStream(c);
            if (stream.trim().toLowerCase() !== requiredStream.trim().toLowerCase()) return false;
        }

        if (!q) return true;
        const name = String(c.Name || c.name || "").toLowerCase();
        const city = String(c.City || c.city || "").toLowerCase();
        const state = String(c.State || c.state || "").toLowerCase();
        const rank = String(c.Rank || c.rank || "").toLowerCase();
        const instId = String(c.instituteId || c["Institute ID"] || "").toLowerCase();
        return name.includes(q) || city.includes(q) || state.includes(q) || rank.includes(q) || instId.includes(q);
    });

    const streamBanner = requiredStream ? `
        <div class="px-2 py-1.5 mb-1 bg-blue-950/70 border border-blue-800/40 rounded-lg text-blue-300 text-[11px] font-semibold flex items-center justify-between">
            <span>🔒 Showing only <strong>${requiredStream}</strong> colleges</span>
        </div>
    ` : '';

    if (filteredOptions.length === 0) {
        drop.innerHTML = streamBanner + `<div class="p-2 text-center text-xs text-slate-400">No matching ${requiredStream || ''} colleges</div>`;
    } else {
        drop.innerHTML = streamBanner + filteredOptions.map(col => {
            const colId = String(col.instituteId || col["Institute ID"] || col.id || col.rank || col.Rank);
            const isSelected = String(colId) === String(currentId);
            return `
                <div data-slot-index="${index}" data-col-id="${colId}" class="compare-dropdown-item p-2 hover:bg-slate-800 ${isSelected ? 'bg-blue-600/20 border-l-2 border-blue-500' : ''} rounded-lg cursor-pointer flex items-center justify-between gap-2 transition">
                    <div class="flex-1 min-w-0 text-left">
                        <div class="font-bold text-slate-100 text-xs leading-snug truncate">${col.Name || col.name}</div>
                        <div class="text-[10px] text-slate-400">📍 ${col.City || col.city}, ${col.State || col.state}</div>
                    </div>
                    <span class="bg-blue-600/20 text-blue-400 font-extrabold text-[10px] px-1.5 py-0.5 rounded-full shrink-0">
                        #${col.nirfRank || col.Rank || col.rank || 'Unranked'}
                    </span>
                </div>
            `;
        }).join('');
    }

    drop.querySelectorAll(".compare-dropdown-item").forEach(item => {
        item.addEventListener("click", (e) => {
            e.stopPropagation();
            const slotIdx = parseInt(item.dataset.slotIndex);
            const colId = item.dataset.colId;
            const targetCol = allColleges.find(c => String(c.instituteId || c["Institute ID"] || c.id || c.rank || c.Rank) === String(colId));

            // Stream mismatch validation check
            if (referenceCollege && targetCol) {
                const stream1 = referenceCollege.stream || getInferredStream(referenceCollege);
                const stream2 = targetCol.stream || getInferredStream(targetCol);
                if (stream1.trim().toLowerCase() !== stream2.trim().toLowerCase()) {
                    const streamDisplay = referenceCollege.stream || stream1;
                    const errorMsg = `Please select a college from the same stream (${streamDisplay}) as your previously selected college.`;
                    if (typeof window.showCompareStreamToast === 'function') {
                        window.showCompareStreamToast(errorMsg);
                    }
                    alert(errorMsg);
                    return;
                }
            }

            selectedCollegeIds[slotIdx] = colId;
            if (targetCol) searchQueries[slotIdx] = targetCol.Name || targetCol.name;
            activeDropdownIndex = null;
            syncGlobalCompareState();
            renderCompareUI();
        });
    });
}

function syncGlobalCompareState() {
    setCompareCollegeIds(selectedCollegeIds.filter(Boolean));
}

function renderCompareUI() {
    const compareSection = document.getElementById("compareSection");
    if (!compareSection) return;

    const selectedCols = [0, 1, 2, 3, 4].map(i => {
        const id = selectedCollegeIds[i];
        return id ? allColleges.find(c => String(c["Institute ID"] || c.id || c.rank || c.Rank) === String(id)) : null;
    });

    const detailsArr = selectedCols.map(c => c ? getCollegeDetails(c) : null);

    const winsPerSlot = [0, 0, 0, 0, 0];
    const activeIndices = selectedCols.map((c, i) => c ? i : null).filter(i => i !== null);
    const activeCount = activeIndices.length;

    // Evaluator across active slots
    let evalResults = null;
    if (activeCount >= 2) {
        const getVal = (col, details, key) => {
            if (!col || !details) return null;
            const m = col.nirfMetrics || {};
            if (key === 'rank') return Number(col.nirfRank ?? col.Rank ?? col.rank) || 999;
            if (key === 'score') return Number(col.Score ?? col.score) || 0;
            if (key === 'tlr') return m.tlr !== undefined ? m.tlr : (col.tlr || 0);
            if (key === 'rpc') return m.rpc !== undefined ? m.rpc : (col.rpc || 0);
            if (key === 'go') return m.go !== undefined ? m.go : (col.go || 0);
            if (key === 'oi') return m.oi !== undefined ? m.oi : (col.oi || 0);
            if (key === 'pr') return m.perception !== undefined ? m.perception : (col.perception || col.pr || 0);
            if (key === 'pkg') return parseFloat(details.avgPackage.replace(/[^0-9.]/g, '')) || 0;
            return 0;
        };

        const compareParam = (key, lowerIsBetter = false) => {
            const vals = [0, 1, 2, 3, 4].map(i => detailsArr[i] ? getVal(selectedCols[i], detailsArr[i], key) : null);
            const valid = vals.filter(v => v !== null && !isNaN(v));
            if (valid.length < 2) return [0, 1, 2, 3, 4].map(() => null);

            const bestVal = lowerIsBetter ? Math.min(...valid) : Math.max(...valid);
            const allSame = valid.every(v => v === valid[0]);

            return vals.map((v, idx) => {
                if (v === null || isNaN(v)) return null;
                if (allSame) return 'Equal';
                if (v === bestVal) {
                    winsPerSlot[idx]++;
                    return 'Best';
                }
                return 'OK';
            });
        };

        evalResults = {
            rank: compareParam('rank', true),
            score: compareParam('score', false),
            tlr: compareParam('tlr', false),
            rpc: compareParam('rpc', false),
            go: compareParam('go', false),
            oi: compareParam('oi', false),
            pr: compareParam('pr', false),
            pkg: compareParam('pkg', false)
        };
    }

    const renderBadge = (badgeType) => {
        if (badgeType === 'Best') return `<span class="badge badge-best font-bold text-xs bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 px-2 py-0.5 rounded-full ml-1">Best</span>`;
        if (badgeType === 'OK') return `<span class="badge badge-ok font-semibold text-xs bg-slate-800 text-slate-400 border border-slate-700 px-2 py-0.5 rounded-full ml-1">OK</span>`;
        if (badgeType === 'Equal') return `<span class="badge font-semibold text-xs bg-blue-600/20 text-blue-400 border border-blue-500/30 px-2 py-0.5 rounded-full ml-1">Equal</span>`;
        return '';
    };

    let html = `
        <div class="mb-6">
            <h2 class="text-2xl font-black text-slate-100 flex items-center gap-2">
                <span>⚖️</span> 5-Slot College Side-by-Side Comparison Matrix
            </h2>
            <p class="text-xs text-slate-400 mt-1">
                Select up to 5 institutions below to evaluate NIRF metrics, fees, cutoffs, and audited parameter verdicts side by side.
            </p>
        </div>

        <!-- 5-SLOT MATRIX TABLE WRAPPER -->
        <div class="overflow-x-auto pb-6">
            <table class="w-full border-collapse min-w-[1000px] table-fixed">
                <thead>
                    <tr>
                        <th class="w-44 p-3 bg-slate-950/90 text-left text-xs font-extrabold text-slate-400 border border-slate-800 uppercase tracking-wider rounded-tl-2xl">
                            Audited Parameter
                        </th>
                        ${[0, 1, 2, 3, 4].map(index => {
                            const selectedCollege = selectedCols[index];
                            const query = (searchQueries[index] || "").toLowerCase().trim();

                            const otherSelectedIds = selectedCollegeIds.filter((id, i) => i !== index && id).map(String);
                            const referenceCollege = otherSelectedIds.length > 0
                                ? allColleges.find(c => String(c.instituteId || c["Institute ID"] || c.id || c.rank || c.Rank) === String(otherSelectedIds[0]))
                                : null;
                            const requiredStream = referenceCollege ? (referenceCollege.stream || getInferredStream(referenceCollege)) : null;

                            const filteredOptions = allColleges.filter(c => {
                                const colId = String(c.instituteId || c["Institute ID"] || c.id || c.rank || c.Rank);
                                if (otherSelectedIds.includes(colId)) return false;

                                // Filter to only list colleges belonging to requiredStream
                                if (requiredStream) {
                                    const stream = c.stream || getInferredStream(c);
                                    if (stream.trim().toLowerCase() !== requiredStream.trim().toLowerCase()) return false;
                                }

                                if (!query) return true;
                                const name = String(c.Name || c.name || "").toLowerCase();
                                const city = String(c.City || c.city || "").toLowerCase();
                                const state = String(c.State || c.state || "").toLowerCase();
                                const rank = String(c.Rank || c.rank || "").toLowerCase();
                                const instId = String(c.instituteId || c["Institute ID"] || "").toLowerCase();
                                return name.includes(query) || city.includes(query) || state.includes(query) || rank.includes(query) || instId.includes(query);
                            });

                            const streamBanner = requiredStream ? `
                                <div class="px-2 py-1.5 mb-1 bg-blue-950/70 border border-blue-800/40 rounded-lg text-blue-300 text-[11px] font-semibold flex items-center justify-between">
                                    <span>🔒 Showing only <strong>${requiredStream}</strong> colleges</span>
                                </div>
                            ` : '';

                            return `
                                <th class="p-3 bg-slate-900 border border-slate-800 text-left align-top min-w-[190px] ${index === 4 ? 'rounded-tr-2xl' : ''}">
                                    <div class="space-y-2">
                                        <div class="flex justify-between items-center">
                                            <div class="flex items-center gap-1.5 flex-wrap">
                                                <span class="text-xs font-bold text-blue-400 uppercase tracking-wider">Slot ${index + 1}</span>
                                                ${selectedCollege ? `
                                                    <span class="text-[10px] bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 px-1.5 py-0.5 rounded-full font-semibold">
                                                        ${getInferredStream(selectedCollege)}
                                                    </span>
                                                ` : (requiredStream ? `
                                                    <span class="text-[10px] bg-blue-500/20 text-blue-400 border border-blue-500/30 px-1.5 py-0.5 rounded-full font-semibold">
                                                        🔒 ${requiredStream}
                                                    </span>
                                                ` : '')}
                                            </div>
                                            ${selectedCollege ? `
                                                <button data-clear-index="${index}" class="clear-selection-btn text-[10px] text-amber-400 hover:text-amber-300 font-semibold px-2 py-0.5 rounded bg-slate-950 border border-amber-500/30 transition">
                                                    ✕ Clear
                                                </button>
                                            ` : ''}
                                        </div>

                                        <!-- Search & Dropdown Box -->
                                        <div class="relative">
                                            <input type="text" 
                                                id="compareSearchInput_${index}"
                                                data-search-index="${index}" 
                                                value="${searchQueries[index] !== undefined && searchQueries[index] !== null ? searchQueries[index] : (selectedCollege ? (selectedCollege.Name || selectedCollege.name) : '')}" 
                                                placeholder="🔍 Search college..." 
                                                autocomplete="off"
                                                class="college-search-input w-full bg-slate-950 border border-slate-700 rounded-xl pl-3 pr-7 py-2 text-xs text-slate-100 font-medium placeholder-slate-500 focus:outline-none focus:border-blue-500 transition">
                                            ${(searchQueries[index] || selectedCollege) ? `
                                                <button type="button" data-clear-search-index="${index}" class="clear-compare-search-btn absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white text-xs font-bold p-0.5 z-10 cursor-pointer" title="Clear search">✕</button>
                                            ` : ''}
                                            
                                            <div id="compareDropdown_${index}" class="college-search-dropdown absolute left-0 right-0 top-full mt-1 z-50 bg-slate-950 border border-slate-700 rounded-xl shadow-2xl max-h-56 overflow-y-auto p-1 divide-y divide-slate-800/80 ${activeDropdownIndex === index ? '' : 'hidden'}">
                                                ${streamBanner}
                                                ${filteredOptions.length === 0 ? `
                                                    <div class="p-2 text-center text-xs text-slate-400">No matching ${requiredStream || ''} colleges</div>
                                                ` : filteredOptions.map(col => {
                                                    const colId = String(col["Institute ID"] || col.id || col.rank || col.Rank);
                                                    const isSelected = selectedCollege && String(colId) === String(selectedCollege["Institute ID"] || selectedCollege.id || selectedCollege.rank || selectedCollege.Rank);
                                                    return `
                                                        <div data-slot-index="${index}" data-col-id="${colId}" class="compare-dropdown-item p-2 hover:bg-slate-800 ${isSelected ? 'bg-blue-600/20 border-l-2 border-blue-500' : ''} rounded-lg cursor-pointer flex items-center justify-between gap-2 transition">
                                                            <div class="flex-1 min-w-0 text-left">
                                                                <div class="font-bold text-slate-100 text-xs leading-snug truncate">${col.Name || col.name}</div>
                                                                <div class="text-[10px] text-slate-400">📍 ${col.City || col.city}, ${col.State || col.state}</div>
                                                            </div>
                                                            <span class="bg-blue-600/20 text-blue-400 font-extrabold text-[10px] px-1.5 py-0.5 rounded-full shrink-0">
                                                                #${col.nirfRank || col.Rank || col.rank || 'Unranked'}
                                                            </span>
                                                        </div>
                                                    `;
                                                }).join('')}
                                            </div>
                                        </div>
                                    </div>
                                </th>
                            `;
                        }).join('')}
                    </tr>
                </thead>
                <tbody class="divide-y divide-slate-800 text-xs">
                    
                    <!-- Row 1: Institution Header -->
                    <tr class="bg-slate-900/60">
                        <td class="p-3 font-extrabold text-slate-300 bg-slate-950/80 border border-slate-800">
                            🏛️ Institution Header
                        </td>
                        ${detailsArr.map((details, i) => {
                            if (!details) return `<td class="p-4 border border-slate-800 text-center text-slate-500 bg-slate-950/30">Select College for Slot ${i + 1}</td>`;
                            return `
                                <td class="p-4 border border-slate-800 bg-slate-900/40 align-top">
                                    <div class="space-y-1.5">
                                        <h3 class="font-extrabold text-white text-sm leading-snug">${details.name}</h3>
                                        <p class="text-xs text-slate-400">📍 ${details.locationStr}</p>
                                        <span class="inline-block bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-[11px] font-bold px-2 py-0.5 rounded-full mt-1">${details.accreditation}</span>
                                    </div>
                                </td>
                            `;
                        }).join('')}
                    </tr>

                    <!-- Row 2: NIRF Rank (Parameter 1/8) -->
                    <tr class="bg-slate-900/30">
                        <td class="p-3 font-extrabold text-slate-300 bg-slate-950/80 border border-slate-800">
                            🏆 1. NIRF Rank
                        </td>
                        ${detailsArr.map((details, i) => `
                            <td class="p-3 border border-slate-800 align-middle">
                                ${details ? `<span class="font-bold text-white">${details.rankDisplay}</span> ${evalResults && evalResults.rank[i] ? renderBadge(evalResults.rank[i]) : ''}` : '-'}
                            </td>
                        `).join('')}
                    </tr>

                    <!-- Row 3: Overall Score (Parameter 2/8) -->
                    <tr class="bg-slate-900/50">
                        <td class="p-3 font-extrabold text-slate-300 bg-slate-950/80 border border-slate-800">
                            ⭐ 2. Overall NIRF Score
                        </td>
                        ${detailsArr.map((details, i) => `
                            <td class="p-3 border border-slate-800 align-middle">
                                ${details ? `<span class="font-bold text-white">⭐ ${details.score}</span> ${evalResults && evalResults.score[i] ? renderBadge(evalResults.score[i]) : ''}` : '-'}
                            </td>
                        `).join('')}
                    </tr>

                    <!-- Row 4: TLR Score (Parameter 3/8) -->
                    <tr class="bg-slate-900/30">
                        <td class="p-3 font-extrabold text-slate-300 bg-slate-950/80 border border-slate-800">
                            📚 3. TLR (Teaching & Resources)
                        </td>
                        ${detailsArr.map((details, i) => {
                            const col = selectedCols[i];
                            const m = col?.nirfMetrics || {};
                            const val = m.tlr !== undefined ? m.tlr : (col?.tlr || 0);
                            return `
                                <td class="p-3 border border-slate-800 align-middle">
                                    ${details ? `<span class="font-bold text-blue-400">${val} / 100</span> ${evalResults && evalResults.tlr[i] ? renderBadge(evalResults.tlr[i]) : ''}` : '-'}
                                </td>
                            `;
                        }).join('')}
                    </tr>

                    <!-- Row 5: RPC Score (Parameter 4/8) -->
                    <tr class="bg-slate-900/50">
                        <td class="p-3 font-extrabold text-slate-300 bg-slate-950/80 border border-slate-800">
                            🔬 4. RPC (Research Practice)
                        </td>
                        ${detailsArr.map((details, i) => {
                            const col = selectedCols[i];
                            const m = col?.nirfMetrics || {};
                            const val = m.rpc !== undefined ? m.rpc : (col?.rpc || 0);
                            return `
                                <td class="p-3 border border-slate-800 align-middle">
                                    ${details ? `<span class="font-bold text-emerald-400">${val} / 100</span> ${evalResults && evalResults.rpc[i] ? renderBadge(evalResults.rpc[i]) : ''}` : '-'}
                                </td>
                            `;
                        }).join('')}
                    </tr>

                    <!-- Row 6: GO Score (Parameter 5/8) -->
                    <tr class="bg-slate-900/30">
                        <td class="p-3 font-extrabold text-slate-300 bg-slate-950/80 border border-slate-800">
                            🎓 5. GO (Graduation Outcomes)
                        </td>
                        ${detailsArr.map((details, i) => {
                            const col = selectedCols[i];
                            const m = col?.nirfMetrics || {};
                            const val = m.go !== undefined ? m.go : (col?.go || 0);
                            return `
                                <td class="p-3 border border-slate-800 align-middle">
                                    ${details ? `<span class="font-bold text-indigo-400">${val} / 100</span> ${evalResults && evalResults.go[i] ? renderBadge(evalResults.go[i]) : ''}` : '-'}
                                </td>
                            `;
                        }).join('')}
                    </tr>

                    <!-- Row 7: OI Score (Parameter 6/8) -->
                    <tr class="bg-slate-900/50">
                        <td class="p-3 font-extrabold text-slate-300 bg-slate-950/80 border border-slate-800">
                            🌐 6. OI (Outreach & Inclusivity)
                        </td>
                        ${detailsArr.map((details, i) => {
                            const col = selectedCols[i];
                            const m = col?.nirfMetrics || {};
                            const val = m.oi !== undefined ? m.oi : (col?.oi || 0);
                            return `
                                <td class="p-3 border border-slate-800 align-middle">
                                    ${details ? `<span class="font-bold text-amber-400">${val} / 100</span> ${evalResults && evalResults.oi[i] ? renderBadge(evalResults.oi[i]) : ''}` : '-'}
                                </td>
                            `;
                        }).join('')}
                    </tr>

                    <!-- Row 8: PR Score (Parameter 7/8) -->
                    <tr class="bg-slate-900/30">
                        <td class="p-3 font-extrabold text-slate-300 bg-slate-950/80 border border-slate-800">
                            👥 7. Perception Score
                        </td>
                        ${detailsArr.map((details, i) => {
                            const col = selectedCols[i];
                            const m = col?.nirfMetrics || {};
                            const val = m.perception !== undefined ? m.perception : (col?.perception || col?.pr || 0);
                            return `
                                <td class="p-3 border border-slate-800 align-middle">
                                    ${details ? `<span class="font-bold text-purple-400">${val} / 100</span> ${evalResults && evalResults.pr[i] ? renderBadge(evalResults.pr[i]) : ''}` : '-'}
                                </td>
                            `;
                        }).join('')}
                    </tr>

                    <!-- Row 9: Average CTC / Package (Parameter 8/8) -->
                    <tr class="bg-slate-900/50">
                        <td class="p-3 font-extrabold text-slate-300 bg-slate-950/80 border border-slate-800">
                            📈 8. Placement (Avg Package)
                        </td>
                        ${detailsArr.map((details, i) => `
                            <td class="p-3 border border-slate-800 align-middle">
                                ${details ? `<span class="font-bold text-blue-400">${details.avgPackage}</span> ${evalResults && evalResults.pkg[i] ? renderBadge(evalResults.pkg[i]) : ''}` : '-'}
                            </td>
                        `).join('')}
                    </tr>

                    <!-- Row 10: Fees & Cutoff Details -->
                    <tr class="bg-slate-900/30">
                        <td class="p-3 font-extrabold text-slate-300 bg-slate-950/80 border border-slate-800">
                            💰 Fee & Cutoff Overview
                        </td>
                        ${detailsArr.map((details) => `
                            <td class="p-3 border border-slate-800 align-top space-y-1">
                                ${details ? `
                                    <div><strong class="text-slate-400">Fee:</strong> <span class="text-blue-400 font-bold">${details.btechFee}</span></div>
                                    <div><strong class="text-slate-400">Cutoff:</strong> <span class="text-emerald-400 font-semibold">${details.eligibilityCutoff}</span></div>
                                ` : '-'}
                            </td>
                        `).join('')}
                    </tr>

                    <!-- Row 11: Actions -->
                    <tr class="bg-slate-900/90">
                        <td class="p-3 font-extrabold text-slate-300 bg-slate-950/90 border border-slate-800 rounded-bl-2xl">
                            ⚙️ Bookmark Action
                        </td>
                        ${detailsArr.map((details, index) => {
                            if (!details) return `<td class="p-3 border border-slate-800 text-center text-slate-600 ${index === 4 ? 'rounded-br-2xl' : ''}">-</td>`;
                            const isBookmarked = isCollegeBookmarked(details.instId);

                            return `
                                <td class="p-3 border border-slate-800 align-middle ${index === 4 ? 'rounded-br-2xl' : ''}">
                                    <button data-bookmark-col-id="${details.instId}" class="compare-row-bookmark-btn w-full py-2 px-3 rounded-xl border font-bold text-xs transition flex items-center justify-center gap-1.5 ${
                                        isBookmarked 
                                            ? 'bg-amber-500/20 text-amber-400 border-amber-500/50 hover:bg-amber-500/30' 
                                            : 'bg-slate-800 text-slate-300 border-slate-700 hover:text-amber-400'
                                    }">
                                        <span>🔖</span>
                                        <span>${isBookmarked ? 'Saved' : 'Bookmark'}</span>
                                    </button>
                                </td>
                            `;
                        }).join('')}
                    </tr>

                </tbody>
            </table>
        </div>
    `;

    // BOTTOM OVERALL VERDICT CARD
    if (activeCount >= 2 && evalResults) {
        const maxWins = Math.max(...winsPerSlot);
        const winningIndices = activeIndices.filter(i => winsPerSlot[i] === maxWins);

        let verdictSummary = "";
        if (winningIndices.length === 1) {
            const winnerName = detailsArr[winningIndices[0]].name;
            verdictSummary = `${winnerName} wins in ${maxWins} out of 8 audited parameters (including TLR, RPC, and Placement metrics). Recommended based on NIRF audit.`;
        } else {
            const tiedNames = winningIndices.map(i => detailsArr[i].name).join(" and ");
            verdictSummary = `${tiedNames} are tied with ${maxWins} wins out of 8 audited parameters. Equally recommended based on NIRF audit.`;
        }

        html += `
            <div class="mt-4 bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 border border-emerald-500/40 rounded-2xl p-6 shadow-2xl space-y-3">
                <div class="flex items-center gap-2">
                    <span class="text-2xl">🏆</span>
                    <h3 class="text-lg font-black text-white">Overall NIRF Parameter Audit Verdict</h3>
                </div>
                <div class="flex flex-wrap items-center gap-3 text-xs font-bold text-slate-300 bg-slate-950/60 p-3 rounded-xl border border-slate-800">
                    ${activeIndices.map(i => `
                        <div class="px-2">Slot ${i + 1} (${detailsArr[i].name}): <span class="text-emerald-400 font-extrabold text-sm">${winsPerSlot[i]} Wins</span></div>
                    `).join('<div class="text-slate-700">|</div>')}
                </div>
                <p class="text-sm text-emerald-300 font-semibold leading-relaxed bg-emerald-950/40 p-4 rounded-xl border border-emerald-500/30">
                    ${verdictSummary}
                </p>
            </div>
        `;
    } else {
        html += `
            <div class="mt-4 bg-slate-800/60 border border-slate-700/80 rounded-2xl p-6 text-center text-slate-400 text-xs">
                💡 Select two or more colleges in the slots above (up to 5) to view the automated 8-parameter NIRF audit verdict.
            </div>
        `;
    }

    compareSection.innerHTML = html;

    // Attach Event Listeners
    // 1. Search Inputs
    compareSection.querySelectorAll(".college-search-input").forEach(input => {
        const idx = parseInt(input.dataset.searchIndex);

        input.addEventListener("focus", () => {
            activeDropdownIndex = idx;
            updateDropdownContent(idx, input.value);
            const drop = compareSection.querySelector(`#compareDropdown_${idx}`);
            if (drop) drop.classList.remove("hidden");
        });

        input.addEventListener("input", (e) => {
            activeDropdownIndex = idx;
            searchQueries[idx] = e.target.value;
            updateDropdownContent(idx, e.target.value);
            const drop = compareSection.querySelector(`#compareDropdown_${idx}`);
            if (drop) drop.classList.remove("hidden");
        });
    });

    // 2. Initial Dropdown Items Attachment
    [0, 1, 2, 3, 4].forEach((idx) => {
        updateDropdownContent(idx, searchQueries[idx] || "");
    });

    // Close active dropdown if clicked outside
    if (!window._compareClickBound) {
        document.addEventListener("click", (e) => {
            if (!e.target.closest(".relative")) {
                activeDropdownIndex = null;
                const section = document.getElementById("compareSection");
                if (section) {
                    section.querySelectorAll(".college-search-dropdown").forEach(d => d.classList.add("hidden"));
                }
            }
        });
        window._compareClickBound = true;
    }

    // 3. Clear Selection Buttons
    compareSection.querySelectorAll(".clear-selection-btn").forEach(btn => {
        btn.addEventListener("click", (e) => {
            const idx = parseInt(e.currentTarget.dataset.clearIndex);
            const removedId = selectedCollegeIds[idx];
            selectedCollegeIds[idx] = null;
            searchQueries[idx] = "";
            activeDropdownIndex = null;
            if (removedId) removeCollegeFromCompare(removedId);
            syncGlobalCompareState();
            renderCompareUI();
        });
    });

    // 4. Clear Search Query Button (✕)
    compareSection.querySelectorAll(".clear-compare-search-btn").forEach(btn => {
        btn.addEventListener("click", (e) => {
            e.stopPropagation();
            const idx = parseInt(btn.dataset.clearSearchIndex);
            const removedId = selectedCollegeIds[idx];
            selectedCollegeIds[idx] = null;
            searchQueries[idx] = "";
            activeDropdownIndex = idx;
            if (removedId) removeCollegeFromCompare(removedId);
            syncGlobalCompareState();
            renderCompareUI();

            // Auto-focus input and show dropdown menu
            setTimeout(() => {
                const inp = document.getElementById(`compareSearchInput_${idx}`);
                if (inp) {
                    inp.focus();
                    updateDropdownContent(idx, "");
                    const drop = compareSection.querySelector(`#compareDropdown_${idx}`);
                    if (drop) drop.classList.remove("hidden");
                }
            }, 50);
        });
    });

    // 5. Bookmark Action Buttons
    compareSection.querySelectorAll(".compare-row-bookmark-btn").forEach(btn => {
        btn.addEventListener("click", (e) => {
            e.stopPropagation();
            if (!auth.currentUser) {
                openAuthModal("🔒 Unlock Your Personal Experience: Register or Sign In to save shortlists and sync across devices.");
                return;
            }
            const colId = e.currentTarget.dataset.bookmarkColId;
            if (colId) {
                toggleCollegeBookmark(colId);
                renderCompareUI();
            }
        });
    });
}
