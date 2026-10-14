import { db } from "../config/firebase-config.js";
import { collection, getDocs } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

export const STREAM_COLLECTIONS = {
    Dental: "dental_colleges",
    Engineering: "engineering_colleges",
    Law: "law_colleges",
    Management: "management_colleges"
};

export function getCollectionNameForStream(stream) {
    if (!stream || stream.trim().toLowerCase() === "all") return null;
    const match = Object.keys(STREAM_COLLECTIONS).find(
        k => k.toLowerCase() === stream.trim().toLowerCase()
    );
    return match ? STREAM_COLLECTIONS[match] : null;
}

export function getStreamNameForCollection(collectionName) {
    const match = Object.entries(STREAM_COLLECTIONS).find(
        ([, col]) => col.toLowerCase() === collectionName.trim().toLowerCase()
    );
    return match ? match[0] : "Engineering";
}

const cacheByStream = new Map();

// Fetch colleges from the active Firestore collection or across all 4 collections
export async function fetchColleges(stream = "All", bypassCache = false) {
    const streamNorm = (stream || "All").trim();
    if (!bypassCache && cacheByStream.has(streamNorm)) {
        const cached = cacheByStream.get(streamNorm);
        if (cached && cached.length > 0) {
            return cached;
        }
    }

    const list = [];
    try {
        const targetCollection = getCollectionNameForStream(streamNorm);

        if (targetCollection) {
            // Specific stream collection
            const streamKey = Object.keys(STREAM_COLLECTIONS).find(k => STREAM_COLLECTIONS[k] === targetCollection) || streamNorm;
            const querySnapshot = await getDocs(collection(db, targetCollection));
            querySnapshot.forEach((doc) => {
                const data = doc.data();
                list.push({
                    id: doc.id,
                    ...data,
                    stream: data.stream || streamKey
                });
            });
        } else {
            // "All" - Fetch from all 4 stream collections concurrently
            const entries = Object.entries(STREAM_COLLECTIONS);
            const snapshots = await Promise.all(
                entries.map(async ([streamName, colPath]) => {
                    try {
                        const snap = await getDocs(collection(db, colPath));
                        return { streamName, snap };
                    } catch (e) {
                        console.warn(`Failed fetching from ${colPath}:`, e);
                        return { streamName, snap: null };
                    }
                })
            );

            snapshots.forEach(({ streamName, snap }) => {
                if (snap && snap.forEach) {
                    snap.forEach((doc) => {
                        const data = doc.data();
                        list.push({
                            id: doc.id,
                            ...data,
                            stream: data.stream || streamName
                        });
                    });
                }
            });
        }

        // Sort ascending using the rank field
        const sorted = list.sort((a, b) => {
            const rA = Number(String(a.nirfRank ?? a.Rank ?? a.rank ?? "999").replace(/[^0-9]/g, '')) || 999;
            const rB = Number(String(b.nirfRank ?? b.Rank ?? b.rank ?? "999").replace(/[^0-9]/g, '')) || 999;
            return rA - rB;
        });

        cacheByStream.set(streamNorm, sorted);

        // Add console.log showing how many documents were retrieved to make debugging easy
        console.log(`Retrieved ${sorted.length} documents for stream: ${streamNorm}`);

        // Keep window._allCollegesCache updated with merged set of all colleges
        if (!window._allCollegesCache) window._allCollegesCache = [];
        const existingMap = new Map(window._allCollegesCache.map(c => [c.id || c.instituteId, c]));
        sorted.forEach(c => existingMap.set(c.id || c.instituteId, c));
        window._allCollegesCache = Array.from(existingMap.values());

        return sorted;
    } catch (err) {
        console.error("Firebase fetch failed for stream", streamNorm, err);
        return [];
    }
}