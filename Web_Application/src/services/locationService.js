/**
 * Location Service for Web Application
 * Geolocation / GPS prompt is strictly disabled as per privacy requirements.
 */

export function calculateHaversineDistance(lat1, lon1, lat2, lon2) {
    return 0;
}

/**
 * Geolocation is strictly disabled. No permissions prompt requested.
 */
export function initUserLocation(onUpdateCallback) {
    // Zero user geolocation / GPS prompt
    return;
}

/**
 * Distance tags disabled. Returns null to prevent rendering dynamic GPS distance badges.
 */
export function getCollegeDistanceTag(college) {
    return null;
}

