/**
 * Domain Verification Service for Web Application
 * Maps official institutional email domains to college identities and enforces review gating.
 */

// Official University Email Domain Mapping Table
const DOMAIN_COLLEGE_MAP = [
    {
        domains: ["saveetha.com", "sse.saveetha.com", "simats.com"],
        collegeId: "saveetha-simats",
        aliases: ["saveetha-school-of-engineering", "saveetha-sse", "sse-simats", "saveetha-simats", "simats", "sse", "ir-e-u-0474", "ir-e-c-26911", "saveetha"],
        collegeName: "Saveetha School of Engineering (SIMATS)"
    },
    {
        domains: ["saveetha.ac.in"],
        collegeId: "saveetha-sec",
        aliases: ["saveetha-engineering-college", "saveetha-sec", "sec"],
        collegeName: "Saveetha Engineering College (SEC)"
    },
    {
        domains: ["iitm.ac.in", "student.iitm.ac.in"],
        collegeId: "iit-madras",
        aliases: ["iit-madras", "iitm"],
        collegeName: "IIT Madras"
    },
    {
        domains: ["vitstudent.ac.in", "vit.ac.in"],
        collegeId: "vit-vellore",
        aliases: ["vit-vellore", "vit"],
        collegeName: "VIT Vellore"
    },
    {
        domains: ["srmist.edu.in"],
        collegeId: "srm-ktr",
        aliases: ["srm-ktr", "srm"],
        collegeName: "SRM Institute of Science and Technology"
    }
];

/**
 * Normalizes input college identifiers (ID, name, institute ID, rank, alias) into standard collegeId
 */
export function normalizeCollegeId(targetIdOrName) {
    if (!targetIdOrName) return "";
    const str = String(targetIdOrName).toLowerCase().trim();

    for (const item of DOMAIN_COLLEGE_MAP) {
        if (item.collegeId === str) return item.collegeId;
        if (item.collegeName.toLowerCase().includes(str) || str.includes(item.collegeName.toLowerCase())) {
            return item.collegeId;
        }
        for (const alias of item.aliases) {
            if (str === alias || str.includes(alias)) return item.collegeId;
        }
    }
    return str;
}

/**
 * Derives student verification status and affiliation from an email address
 * @param {string} email 
 * @returns {{ isVerified: boolean, collegeId: string|null, collegeName: string|null, badgeText: string }}
 */
export function getStudentAffiliation(email) {
    if (!email || typeof email !== "string" || !email.includes("@")) {
        return {
            isVerified: false,
            collegeId: null,
            collegeName: null,
            badgeText: "Standard Account"
        };
    }

    const cleanEmail = email.toLowerCase().trim();
    const domain = cleanEmail.split("@")[1] || "";

    // 1. Saveetha School of Engineering (SIMATS Deemed University)
    if (cleanEmail.includes(".sse@saveetha.com") || 
        cleanEmail.includes("@saveetha.com") || 
        cleanEmail.includes("@sse.saveetha.com") || 
        cleanEmail.includes("@simats.com") ||
        domain === "saveetha.com" ||
        domain === "sse.saveetha.com" ||
        domain === "simats.com") {
        return {
            isVerified: true,
            collegeId: "saveetha-simats",
            collegeName: "Saveetha School of Engineering (SIMATS)",
            badgeText: "🎓 Verified Student of Saveetha School of Engineering (SIMATS)"
        };
    }

    // 2. Saveetha Engineering College (SEC - Autonomous / Anna University)
    if (cleanEmail.includes("@saveetha.ac.in") || domain === "saveetha.ac.in") {
        return {
            isVerified: true,
            collegeId: "saveetha-sec",
            collegeName: "Saveetha Engineering College (SEC)",
            badgeText: "🎓 Verified Student of Saveetha Engineering College (SEC)"
        };
    }

    // 3. General domain map check
    for (const entry of DOMAIN_COLLEGE_MAP) {
        const isMatch = entry.domains.some(d => domain === d || domain.endsWith("." + d));
        if (isMatch) {
            return {
                isVerified: true,
                collegeId: entry.collegeId,
                collegeName: entry.collegeName,
                badgeText: `🎓 Verified Student of ${entry.collegeName}`
            };
        }
    }

    return {
        isVerified: false,
        collegeId: null,
        collegeName: null,
        badgeText: "Standard Account"
    };
}

/**
 * Checks whether a user with the given email is permitted to post a review for a specific college
 * @param {string} userEmail 
 * @param {string} targetCollegeId 
 * @returns {boolean}
 */
export function canUserReview(userEmail, collegeOrId) {
    if (!userEmail || !collegeOrId) return false;
    const email = String(userEmail).toLowerCase().trim();
    const isSaveethaUser = email.includes('@saveetha.com') || email.includes('.sse@saveetha.com') || email.includes('@simats.com') || email.includes('@saveetha.ac.in') || email.includes('saveetha') || email.includes('simats') || email.includes('sse');

    if (isSaveethaUser) {
        const targetStr = (typeof collegeOrId === 'string'
            ? collegeOrId
            : ((collegeOrId?.id || collegeOrId?.collegeId || collegeOrId?.["Institute ID"] || '') + ' ' + (collegeOrId?.name || collegeOrId?.Name || ''))
        ).toLowerCase();

        if (targetStr.includes('saveetha') || targetStr.includes('simats') || targetStr.includes('sse') || targetStr.includes('ir-e-u-0474') || targetStr.includes('ir-e-c-26911')) {
            return true;
        }
    }

    const affiliation = getStudentAffiliation(userEmail);
    if (!affiliation.isVerified) return false;

    if (typeof collegeOrId === 'object' && collegeOrId !== null) {
        const domain = email.split('@')[1];
        const collegeDomain = collegeOrId?.emailDomain || collegeOrId?.domain || '';
        if (domain && collegeDomain && domain.toLowerCase() === collegeDomain.toLowerCase()) {
            return true;
        }
    }

    const targetStr = typeof collegeOrId === 'string'
        ? collegeOrId
        : (collegeOrId?.id || collegeOrId?.collegeId || collegeOrId?.["Institute ID"] || collegeOrId?.name || collegeOrId?.Name || '');

    const targetNormalized = normalizeCollegeId(targetStr);
    return affiliation.collegeId === targetNormalized;
}
