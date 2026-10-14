// Filter colleges by Rank, Name, City, State, Institute ID, or Branch Acronyms matching search query (1+ chars)
export function filterColleges(colleges, query) {
    if (!query || !query.trim()) return colleges;

    const term = query.toLowerCase().trim();

    return colleges.filter((col) => {
        const name = String(col.Name || col.name || "").toLowerCase();
        const city = String(col.City || col.city || "").toLowerCase();
        const state = String(col.State || col.state || "").toLowerCase();
        const rank = String(col.Rank ?? col.rank ?? "").toLowerCase();
        const instId = String(col["Institute ID"] || col.instituteId || "").toLowerCase();
        const branches = Array.isArray(col.branches) ? col.branches.map(b => String(b).toLowerCase()).join(" ") : "";

        return name.includes(term) || 
               city.includes(term) || 
               state.includes(term) || 
               rank.includes(term) ||
               instId.includes(term) ||
               branches.includes(term);
    });
}