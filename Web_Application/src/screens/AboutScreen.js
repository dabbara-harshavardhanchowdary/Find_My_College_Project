export function initAboutScreen() {
    const aboutSection = document.getElementById("aboutSection");
    if (!aboutSection) return;

    // Ensure official NIRF data note exists
    let noteContainer = aboutSection.querySelector(".about-nirf-note");
    if (!noteContainer) {
        const wrapper = aboutSection.querySelector(".bg-slate-800\\/60") || aboutSection;
        const noteHtml = `
            <div class="about-nirf-note pt-4 border-t border-slate-700/80 mt-4">
                <div class="bg-blue-950/40 border border-blue-500/30 rounded-2xl p-4 flex items-start gap-3">
                    <span class="text-xl shrink-0">📌</span>
                    <div>
                        <h5 class="text-xs font-bold text-blue-400 uppercase tracking-wider mb-1">Official Data Note</h5>
                        <p class="text-xs text-slate-300 font-medium leading-relaxed">
                            <strong>Note:</strong> All ranking data, metrics, and institutional reports on this platform are subject to and sourced directly from NIRF, Ministry of Education, Government of India.
                        </p>
                    </div>
                </div>
            </div>
        `;
        wrapper.insertAdjacentHTML("beforeend", noteHtml);
    }
}
