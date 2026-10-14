/**
 * Review Service for Web Application
 * Handles college reviews, submit authorization guards, and verified student tagging.
 */

import { auth } from "../config/firebase-config.js";
import { canUserReview, getStudentAffiliation, normalizeCollegeId } from "./verificationService.js";

// Sample initial verified student reviews
const initialReviews = [
    {
        id: "rev_1",
        collegeId: "saveetha-simats",
        authorName: "Rahul Kumar",
        authorEmail: "rahul.sse@saveetha.com",
        verificationTag: "Verified Student of Saveetha Engineering College (SIMATS)",
        rating: 5,
        comment: "Outstanding research labs, supportive faculty, and top tier placement opportunities in core CS & AI fields.",
        date: "2026-09-15"
    },
    {
        id: "rev_2",
        collegeId: "iit-madras",
        authorName: "Ananya R",
        authorEmail: "ananya@iitm.ac.in",
        verificationTag: "Verified Student of IIT Madras",
        rating: 5,
        comment: "Unmatched academic rigor, world class innovation ecosystem, and unparalleled campus life.",
        date: "2026-09-28"
    }
];

// Load local reviews from storage or defaults
function loadStoredReviews() {
    try {
        const stored = localStorage.getItem("findmycollege_reviews");
        if (stored) return JSON.parse(stored);
    } catch (e) {
        console.error("Error loading reviews from localStorage:", e);
    }
    return [...initialReviews];
}

function saveStoredReviews(reviews) {
    try {
        localStorage.setItem("findmycollege_reviews", JSON.stringify(reviews));
    } catch (e) {
        console.error("Error saving reviews to localStorage:", e);
    }
}

let reviewsList = loadStoredReviews();

/**
 * Fetch all reviews for a specific college
 * @param {string} collegeId 
 */
export function getReviewsForCollege(collegeId) {
    const normId = normalizeCollegeId(collegeId);
    return reviewsList.filter(r => normalizeCollegeId(r.collegeId) === normId);
}

/**
 * Submit a review for a college with strict backend guard authorization
 * @param {string} collegeId 
 * @param {{ rating: number, comment: string }} reviewData 
 */
export function submitReview(collegeId, reviewData) {
    const currentUser = auth.currentUser;
    const userEmail = currentUser ? currentUser.email : null;

    // Strict Backend Service Guard: Reject write if user cannot review target college
    if (!userEmail || !canUserReview(userEmail, collegeId)) {
        throw new Error("🔒 Authorization Error: Only verified students with an official university email from this institution can submit reviews.");
    }

    const affiliation = getStudentAffiliation(userEmail);
    const authorName = currentUser.displayName || (userEmail ? userEmail.split("@")[0] : "Verified Student");

    const newReview = {
        id: `rev_${Date.now()}`,
        collegeId: normalizeCollegeId(collegeId),
        authorName,
        authorEmail: userEmail,
        verificationTag: `Verified Student of ${affiliation.collegeName}`,
        rating: Math.max(1, Math.min(5, Number(reviewData.rating) || 5)),
        comment: String(reviewData.comment || "").trim(),
        date: new Date().toISOString().split("T")[0]
    };

    reviewsList.unshift(newReview);
    saveStoredReviews(reviewsList);

    // Dispatch event for UI reactivity
    window.dispatchEvent(new CustomEvent("reviewsChanged", { detail: { collegeId } }));

    return newReview;
}
