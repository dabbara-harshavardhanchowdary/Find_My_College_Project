/**
 * Return on Investment (ROI) Donut Chart Component
 * Calculates 4-year total cost, 1st-year CTC return, ROI ratio, and estimated payback period.
 * Renders an interactive SVG Donut Chart with tooltips and badges.
 */

/**
 * Helper to parse fee or salary string into Lakhs INR (float)
 */
export function parseAmountToLakhs(str, defaultLakhs = 5.0) {
    if (!str) return defaultLakhs;
    try {
        const cleaned = String(str).replace(/,/g, '');
        const match = cleaned.match(/([0-9]+(?:\.[0-9]+)?)/);
        if (!match) return defaultLakhs;
        
        const num = parseFloat(match[1]);
        if (cleaned.toLowerCase().includes("lakh") || cleaned.toLowerCase().includes("lpa")) {
            return num;
        }
        if (num > 1000) {
            return num / 100000; // e.g. 50,000 -> 0.5 Lakhs
        }
        return num;
    } catch (e) {
        return defaultLakhs;
    }
}

/**
 * Format Lakhs into readable Indian Currency string (e.g. 12.5 -> "₹12.50 Lakhs")
 */
export function formatLakhsCurrency(lakhs) {
    if (isNaN(lakhs) || lakhs <= 0) return "₹0";
    return `₹${lakhs.toFixed(2)} Lakhs`;
}

/**
 * Format Lakhs into exact INR value string (e.g. 12.5 -> "₹12,50,000")
 */
export function formatInrExact(lakhs) {
    if (isNaN(lakhs) || lakhs <= 0) return "₹0";
    const amount = Math.round(lakhs * 100000);
    return `₹${amount.toLocaleString('en-IN')}`;
}

/**
 * Calculate complete ROI metrics for a given college details object
 */
export function calculateCollegeRoi(details) {
    const annualTuitionLakhs = parseAmountToLakhs(details.btechFee, 2.0);
    const annualHostelLakhs = parseAmountToLakhs(details.hostelFee, 0.75);
    
    // Total 4-Year Investment = 4 * (Tuition + Hostel/Living)
    const total4YrCost = (annualTuitionLakhs + annualHostelLakhs) * 4;
    
    // 1st-Year CTC / Average Package
    const avgSalaryLakhs = parseAmountToLakhs(details.avgPackage, 7.5);
    
    // Calculations
    const roiMultiplier = total4YrCost > 0 ? (avgSalaryLakhs / total4YrCost) : 0;
    const roiPercent = (roiMultiplier * 100).toFixed(1);
    const paybackYears = avgSalaryLakhs > 0 ? (total4YrCost / avgSalaryLakhs).toFixed(1) : "N/A";

    return {
        annualTuitionLakhs,
        annualHostelLakhs,
        total4YrCost,
        avgSalaryLakhs,
        roiMultiplier: roiMultiplier.toFixed(2),
        roiPercent,
        paybackYears,
        total4YrCostFormatted: formatInrExact(total4YrCost),
        avgSalaryFormatted: formatInrExact(avgSalaryLakhs)
    };
}

/**
 * Render responsive, interactive SVG Donut Chart HTML
 */
export function renderRoiDonutChartHtml(details) {
    if (!details) return `<div class="text-slate-500 text-xs text-center py-4">-</div>`;

    const roiData = calculateCollegeRoi(details);
    const { total4YrCost, avgSalaryLakhs, roiMultiplier, paybackYears, total4YrCostFormatted, avgSalaryFormatted } = roiData;

    // SVG Donut geometry
    const r = 38;
    const circumference = 2 * Math.PI * r; // ~238.76
    const sum = (total4YrCost + avgSalaryLakhs) || 1;
    
    const costRatio = total4YrCost / sum;
    const salaryRatio = avgSalaryLakhs / sum;
    
    const costDash = (costRatio * circumference).toFixed(2);
    const salaryDash = (salaryRatio * circumference).toFixed(2);
    const salaryOffset = (-costRatio * circumference).toFixed(2);

    return `
        <div class="bg-slate-950 p-3 rounded-xl border border-slate-800 space-y-3">
            <!-- Chart Header & Badge -->
            <div class="flex items-center justify-between border-b border-slate-800/80 pb-2">
                <span class="text-[11px] font-bold text-slate-300 flex items-center gap-1">
                    📊 ROI Analysis
                </span>
                <span class="bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 text-[10px] font-extrabold px-2 py-0.5 rounded-full">
                    Payback: ${paybackYears} Yrs
                </span>
            </div>

            <!-- SVG Donut Chart with Central ROI Overlay -->
            <div class="relative flex items-center justify-center my-1 group">
                <svg viewBox="0 0 100 100" class="w-32 h-32 transform -rotate-90 filter drop-shadow-md">
                    <!-- Background Ring -->
                    <circle cx="50" cy="50" r="${r}" fill="none" stroke="#1E293B" stroke-width="12" />
                    
                    <!-- Cost Arc (Slice 1 - Orange/Red) -->
                    <circle cx="50" cy="50" r="${r}" fill="none" stroke="#F97316" stroke-width="12"
                        stroke-dasharray="${costDash} ${circumference}"
                        stroke-linecap="round"
                        class="transition-all duration-300 hover:stroke-amber-400 cursor-pointer">
                        <title>Total 4-Year Cost: ${total4YrCostFormatted}</title>
                    </circle>

                    <!-- Salary Return Arc (Slice 2 - Emerald Green) -->
                    <circle cx="50" cy="50" r="${r}" fill="none" stroke="#10B981" stroke-width="12"
                        stroke-dasharray="${salaryDash} ${circumference}"
                        stroke-dashoffset="${salaryOffset}"
                        stroke-linecap="round"
                        class="transition-all duration-300 hover:stroke-emerald-300 cursor-pointer">
                        <title>1st-Year Avg Salary: ${avgSalaryFormatted}</title>
                    </circle>
                </svg>

                <!-- Center ROI Multiplier Display -->
                <div class="absolute inset-0 flex flex-col items-center justify-center pointer-events-none text-center">
                    <span class="text-sm font-black text-white leading-none tracking-tight">${roiMultiplier}x</span>
                    <span class="text-[9px] font-bold text-slate-400 uppercase mt-0.5">ROI Ratio</span>
                </div>
            </div>

            <!-- Legend & Exact Amount Tooltips Container -->
            <div class="grid grid-cols-2 gap-2 text-[10px] pt-1">
                <div class="bg-slate-900/90 p-1.5 rounded-lg border border-orange-500/20 flex flex-col transition hover:border-orange-500/50" title="Total 4-Year Tuition & Living Investment">
                    <div class="flex items-center gap-1 text-slate-400">
                        <span class="w-2 h-2 rounded-full bg-orange-500 shrink-0"></span>
                        <span class="truncate">Total 4-Yr Cost</span>
                    </div>
                    <span class="font-bold text-orange-400 mt-0.5 text-[11px]">${total4YrCostFormatted}</span>
                </div>

                <div class="bg-slate-900/90 p-1.5 rounded-lg border border-emerald-500/20 flex flex-col transition hover:border-emerald-500/50" title="Average 1st-Year Package CTC">
                    <div class="flex items-center gap-1 text-slate-400">
                        <span class="w-2 h-2 rounded-full bg-emerald-500 shrink-0"></span>
                        <span class="truncate">1st-Yr Salary</span>
                    </div>
                    <span class="font-bold text-emerald-400 mt-0.5 text-[11px]">${avgSalaryFormatted}</span>
                </div>
            </div>
        </div>
    `;
}
