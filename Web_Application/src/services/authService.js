import { auth, db } from "../config/firebase-config.js";
import { 
    signInWithEmailAndPassword, 
    createUserWithEmailAndPassword, 
    signOut, 
    onAuthStateChanged,
    GoogleAuthProvider,
    signInWithPopup,
    updateProfile,
    sendPasswordResetEmail,
    sendEmailVerification,
    EmailAuthProvider,
    reauthenticateWithCredential,
    deleteUser
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-auth.js";
import { doc, setDoc, getDoc, deleteDoc } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";
import { initFirestoreBookmarksListener } from "./bookmarkService.js";

// Listen to Auth State
export function initAuthListener(callback) {
    onAuthStateChanged(auth, async (user) => {
        if (user) {
            console.log("Logged in user:", user.email);
            const userRef = doc(db, "users", user.uid);
            const snap = await getDoc(userRef);
            if (!snap.exists()) {
                await setDoc(userRef, {
                    uid: user.uid,
                    email: user.email,
                    fullName: user.displayName || "Student",
                    emailVerified: user.emailVerified || false,
                    createdAt: new Date().toISOString()
                }, { merge: true });
            }
        }
        
        // Initialize Firestore real-time bookmarks listener for logged in / logged out state
        initFirestoreBookmarksListener(user);

        if (typeof callback === "function") callback(user);
    });
}

// Email Login
export async function loginUser(email, password) {
    return await signInWithEmailAndPassword(auth, email, password);
}

// Email Signup with Official Firebase Email Verification
export async function registerUser(email, password, fullName = "Student") {
    const cred = await createUserWithEmailAndPassword(auth, email, password);
    const user = cred.user;
    if (fullName) {
        try {
            await updateProfile(user, { displayName: fullName });
        } catch (e) {
            console.warn("Could not update auth displayName:", e);
        }
    }

    // Immediately send official Firebase verification link
    try {
        await sendEmailVerification(user);
        console.log("Official Firebase verification email sent to:", user.email);
    } catch (e) {
        console.warn("Could not send email verification link:", e);
    }

    await setDoc(doc(db, "users", user.uid), {
        uid: user.uid,
        email: user.email,
        fullName: fullName,
        emailVerified: false,
        createdAt: new Date().toISOString()
    }, { merge: true });

    return user;
}

// Send or resend official verification email link
export async function sendUserVerificationEmail() {
    if (!auth.currentUser) throw new Error("No user currently logged in.");
    return await sendEmailVerification(auth.currentUser);
}

// Google Sign In
export async function loginWithGoogle() {
    const provider = new GoogleAuthProvider();
    return await signInWithPopup(auth, provider);
}

// Logout
export async function logoutUser() {
    return await signOut(auth);
}

// Save User Profile to Firestore
export async function saveUserProfile(uid, data) {
    const userRef = doc(db, "users", uid);
    return await setDoc(userRef, data, { merge: true });
}

// Comprehensive profile update helper (Auth displayName + photoURL + Firestore record)
export async function saveProfileDetails(name, phone, bio, programNeeded = "", photoURL = null) {
    const user = auth.currentUser;
    if (!user || !user.uid) {
        const authErr = new Error("No active authenticated user session found.");
        console.error("Firestore write failed:", authErr);
        throw authErr;
    }

    // Update Firebase Auth displayName & photoURL
    const profileUpdates = {};
    if (name) profileUpdates.displayName = name;
    if (photoURL !== null) profileUpdates.photoURL = photoURL;

    if (Object.keys(profileUpdates).length > 0) {
        try {
            await updateProfile(user, profileUpdates);
        } catch (err) {
            console.warn("Failed to update Firebase Auth profile:", err);
        }
    }

    // Exact schema payload
    const payload = {
        fullName: name,
        displayName: name,
        phone: phone || "",
        bio: bio || "",
        programNeeded: programNeeded || "B.Tech - Computer Science (CSE)",
        updatedAt: new Date().toISOString()
    };

    if (photoURL !== null) {
        payload.photoURL = photoURL;
    }

    const userRef = doc(db, "users", user.uid);

    try {
        await setDoc(userRef, payload, { merge: true });
        console.log("Firestore write succeeded for user:", user.uid, payload);
        return payload;
    } catch (error) {
        console.error("Firestore write failed:", error);
        throw error;
    }
}

// Save avatar photo URL/Base64 to Firestore and Auth
export async function saveProfilePhoto(photoURL) {
    const user = auth.currentUser;
    if (!user || !user.uid) throw new Error("No active authenticated user session found.");

    try {
        await updateProfile(user, { photoURL });
    } catch (err) {
        console.warn("Could not update Auth photoURL:", err);
    }

    const userRef = doc(db, "users", user.uid);
    await setDoc(userRef, { photoURL, updatedAt: new Date().toISOString() }, { merge: true });
    return photoURL;
}

// Trigger password reset email via Firebase Auth
export async function sendUserPasswordResetEmail(email) {
    const targetEmail = email || (auth.currentUser ? auth.currentUser.email : null);
    if (!targetEmail) throw new Error("No valid user email address provided.");
    await sendPasswordResetEmail(auth, targetEmail);
}

// Delete user account with in-place credential re-authentication
export async function deleteUserAccountWithPassword(password = "") {
    const user = auth.currentUser;
    if (!user) throw new Error("No active user session found.");

    const email = user.email;
    const isGoogleUser = user.providerData.some(p => p.providerId === "google.com");

    if (!isGoogleUser) {
        if (!password || !password.trim()) {
            throw new Error("Incorrect password. Please try again.");
        }
        try {
            const credential = EmailAuthProvider.credential(email, password.trim());
            await reauthenticateWithCredential(user, credential);
        } catch (err) {
            console.error("Re-authentication failed:", err);
            throw new Error("Incorrect password. Please try again.");
        }
    }

    const uid = user.uid;
    const userRef = doc(db, "users", uid);

    // 1. Delete user document from Firestore
    try {
        await deleteDoc(userRef);
    } catch (err) {
        console.warn("Error deleting Firestore user document:", err);
    }

    // 2. Delete Firebase Auth user
    await deleteUser(user);
}

export const deleteUserAccount = deleteUserAccountWithPassword;

// Get User Profile from Firestore
export async function getUserProfile(uid) {
    const userRef = doc(db, "users", uid);
    const snap = await getDoc(userRef);
    return snap.exists() ? snap.data() : null;
}