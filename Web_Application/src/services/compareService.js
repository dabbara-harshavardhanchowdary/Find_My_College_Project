import { getInferredStream } from "../screens/CollegesScreen.js";

// Centralized Compare State Service for Web Application
let compareCollegeIds = [];

const listeners = new Set();

export function getCompareCollegeIds() {
    return [...compareCollegeIds];
}

export function isCollegeInCompare(instId) {
    if (!instId) return false;
    return compareCollegeIds.includes(String(instId));
}

export function toggleCollegeInCompare(instId) {
    if (!instId) return false;
    const strId = String(instId);
    const index = compareCollegeIds.indexOf(strId);
    
    if (index >= 0) {
        compareCollegeIds.splice(index, 1);
        notifyListeners();
        return false;
    } else {
        if (compareCollegeIds.length >= 5) {
            alert("Maximum 5 colleges can be compared at once. Please remove a college to add a new one.");
            return false;
        }

        // Stream-matching validation: check against first selected college
        if (compareCollegeIds.length > 0) {
            const allColleges = window._allCollegesCache || [];
            const college1 = allColleges.find(c => String(c.instituteId || c["Institute ID"] || c.id || c.rank || c.Rank) === String(compareCollegeIds[0]));
            const college2 = allColleges.find(c => String(c.instituteId || c["Institute ID"] || c.id || c.rank || c.Rank) === strId);
            if (college1 && college2) {
                const stream1 = college1.stream || getInferredStream(college1);
                const stream2 = college2.stream || getInferredStream(college2);
                if (stream1.trim().toLowerCase() !== stream2.trim().toLowerCase()) {
                    const streamDisplay = college1.stream || stream1;
                    const errorMsg = `Please select a college from the same stream (${streamDisplay}) as your previously selected college.`;
                    if (typeof window.showCompareStreamToast === "function") {
                        window.showCompareStreamToast(errorMsg);
                    }
                    alert(errorMsg);
                    return false;
                }
            }
        }

        compareCollegeIds.push(strId);
        notifyListeners();
        return true;
    }
}

export function addCollegeToCompare(instId) {
    if (!instId) return false;
    const strId = String(instId);
    if (!compareCollegeIds.includes(strId)) {
        if (compareCollegeIds.length >= 5) {
            alert("Maximum 5 colleges can be compared at once.");
            return false;
        }

        // Stream-matching validation: check against first selected college
        if (compareCollegeIds.length > 0) {
            const allColleges = window._allCollegesCache || [];
            const college1 = allColleges.find(c => String(c.instituteId || c["Institute ID"] || c.id || c.rank || c.Rank) === String(compareCollegeIds[0]));
            const college2 = allColleges.find(c => String(c.instituteId || c["Institute ID"] || c.id || c.rank || c.Rank) === strId);
            if (college1 && college2) {
                const stream1 = college1.stream || getInferredStream(college1);
                const stream2 = college2.stream || getInferredStream(college2);
                if (stream1.trim().toLowerCase() !== stream2.trim().toLowerCase()) {
                    const streamDisplay = college1.stream || stream1;
                    const errorMsg = `Please select a college from the same stream (${streamDisplay}) as your previously selected college.`;
                    if (typeof window.showCompareStreamToast === "function") {
                        window.showCompareStreamToast(errorMsg);
                    }
                    alert(errorMsg);
                    return false;
                }
            }
        }

        compareCollegeIds.push(strId);
        notifyListeners();
    }
    return true;
}

export function removeCollegeFromCompare(instId) {
    if (!instId) return;
    const strId = String(instId);
    const index = compareCollegeIds.indexOf(strId);
    if (index >= 0) {
        compareCollegeIds.splice(index, 1);
        notifyListeners();
    }
}

export function setCompareCollegeIds(ids) {
    compareCollegeIds = ids.filter(Boolean).map(String);
    notifyListeners();
}

export function subscribeCompareChange(listener) {
    listeners.add(listener);
    return () => listeners.delete(listener);
}

function notifyListeners() {
    listeners.forEach(fn => {
        try { fn(getCompareCollegeIds()); } catch (err) { console.error(err); }
    });
    window.dispatchEvent(new CustomEvent("compareChanged", { detail: getCompareCollegeIds() }));
}
