import { auth } from "../../config/firebase-config.js";
import { 
    logoutUser, 
    getUserProfile, 
    saveProfileDetails, 
    saveProfilePhoto,
    sendUserPasswordResetEmail, 
    deleteUserAccountWithPassword 
} from "../../services/authService.js?v=2";
import { getBookmarkedIds } from "../../services/bookmarkService.js";
import { openAuthModal } from "../modals/AuthModal.js";
import { getStudentAffiliation } from "../../services/verificationService.js";

let cachedProfileData = null;

export function initProfileDrawer() {
    const openProfileBtn = document.getElementById("openProfileBtn");
    const closeProfileBtn = document.getElementById("closeProfileBtn");
    const profileDrawer = document.getElementById("profileDrawer");
    const logoutBtn = document.getElementById("logoutBtn");
    const topLogoutBtn = document.getElementById("topLogoutBtn");
    const profileForm = document.getElementById("profileForm");
    
    const editProfileBtn = document.getElementById("editProfileBtn");
    const saveProfileBtn = document.getElementById("saveProfileBtn");
    const cancelProfileBtn = document.getElementById("cancelProfileBtn");

    const profileAvatarBadge = document.getElementById("profileAvatarBadge");
    const profileImageInput = document.getElementById("profileImageInput");

    const sendPasswordResetBtn = document.getElementById("sendPasswordResetBtn");
    const resetEmailMsg = document.getElementById("resetEmailMsg");

    const deleteAccountBtn = document.getElementById("deleteAccountBtn");
    const deleteAccountModal = document.getElementById("deleteAccountModal");
    const cancelDeleteAccountBtn = document.getElementById("cancelDeleteAccountBtn");
    const confirmDeleteAccountBtn = document.getElementById("confirmDeleteAccountBtn");
    const deleteAccountError = document.getElementById("deleteAccountError");

    // Locked vs Edit Mode Helpers
    function lockProfileFields() {
        const inputs = ["profileFullName", "profilePhone", "profileBio", "profileProgram"];
        inputs.forEach(id => {
            const el = document.getElementById(id);
            if (el) el.disabled = true;
        });

        if (editProfileBtn) editProfileBtn.classList.remove("hidden");
        if (saveProfileBtn) saveProfileBtn.classList.add("hidden");
        if (cancelProfileBtn) cancelProfileBtn.classList.add("hidden");
    }

    function unlockProfileFields() {
        const inputs = ["profileFullName", "profilePhone", "profileBio", "profileProgram"];
        inputs.forEach(id => {
            const el = document.getElementById(id);
            if (el) el.disabled = false;
        });

        if (editProfileBtn) editProfileBtn.classList.add("hidden");
        if (saveProfileBtn) saveProfileBtn.classList.remove("hidden");
        if (cancelProfileBtn) cancelProfileBtn.classList.remove("hidden");
    }

    // Edit Button Click
    if (editProfileBtn) {
        editProfileBtn.addEventListener("click", () => {
            unlockProfileFields();
        });
    }

    // Cancel Button Click
    if (cancelProfileBtn) {
        cancelProfileBtn.addEventListener("click", () => {
            if (cachedProfileData) {
                const profileFullName = document.getElementById("profileFullName");
                const profilePhone = document.getElementById("profilePhone");
                const profileBio = document.getElementById("profileBio");
                const profileProgram = document.getElementById("profileProgram");

                if (profileFullName) profileFullName.value = cachedProfileData.fullName || "";
                if (profilePhone) profilePhone.value = cachedProfileData.phone || "";
                if (profileBio) profileBio.value = cachedProfileData.bio || "";
                if (profileProgram) profileProgram.value = cachedProfileData.programNeeded || "B.Tech - Computer Science (CSE)";
            }
            lockProfileFields();
            const msg = document.getElementById("profileSaveMsg");
            if (msg) msg.classList.add("hidden");
        });
    }

    // Avatar Image Click -> Trigger Input
    if (profileAvatarBadge && profileImageInput) {
        profileAvatarBadge.addEventListener("click", () => {
            profileImageInput.click();
        });

        profileImageInput.addEventListener("change", (e) => {
            const file = e.target.files && e.target.files[0];
            if (!file) return;

            if (!file.type.startsWith("image/")) {
                alert("Please select a valid image file.");
                return;
            }

            const reader = new FileReader();
            reader.onload = async (event) => {
                const base64Data = event.target.result;
                updateAvatarDisplay(base64Data);

                if (auth.currentUser) {
                    try {
                        await saveProfilePhoto(base64Data);
                        console.log("Avatar saved successfully!");
                    } catch (err) {
                        console.error("Failed to save avatar photo:", err);
                        alert("Could not save avatar: " + err.message);
                    }
                }
            };
            reader.readAsDataURL(file);
        });
    }

    // Open Drawer
    if (openProfileBtn && profileDrawer) {
        openProfileBtn.addEventListener("click", () => {
            profileDrawer.classList.remove("hidden");
            if (auth.currentUser) {
                updateProfileUI(auth.currentUser);
            }
        });
    }

    // Close Drawer
    if (closeProfileBtn && profileDrawer) {
        closeProfileBtn.addEventListener("click", () => {
            profileDrawer.classList.add("hidden");
            lockProfileFields();
        });
    }

    // Handle Sign Out
    const handleLogout = async (e) => {
        if (e) e.preventDefault();
        if (profileDrawer) profileDrawer.classList.add("hidden");
        const navDrawer = document.getElementById("navDrawer");
        if (navDrawer) navDrawer.classList.add("hidden");

        try {
            await logoutUser();
            console.log("User signed out from Profile Drawer");
            window.location.hash = "#signin";
            openAuthModal("🔒 You have signed out. Please sign in to access your account.", "signin");
        } catch (err) {
            console.error("Logout error:", err);
            alert("Failed to log out: " + err.message);
        }
    };

    if (logoutBtn) logoutBtn.addEventListener("click", handleLogout);
    if (topLogoutBtn) topLogoutBtn.addEventListener("click", handleLogout);

    // Save Profile Form Submission
    if (profileForm) {
        profileForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const currentUser = auth.currentUser;
            if (!currentUser || !currentUser.uid) {
                alert("Please log in to save your profile details.");
                return;
            }

            const nameInput = document.getElementById("profileFullName");
            const phoneInput = document.getElementById("profilePhone");
            const bioInput = document.getElementById("profileBio");
            const programInput = document.getElementById("profileProgram");

            const fullName = nameInput ? nameInput.value.trim() : "";
            const phone = phoneInput ? phoneInput.value.trim() : "";
            const bio = bioInput ? bioInput.value.trim() : "";
            const programNeeded = programInput ? programInput.value : "";

            if (saveProfileBtn) {
                saveProfileBtn.innerText = "Saving Profile...";
                saveProfileBtn.disabled = true;
            }

            try {
                // Call saveProfileDetails service (uses setDoc with merge: true)
                await saveProfileDetails(fullName, phone, bio, programNeeded);

                cachedProfileData = { fullName, phone, bio, programNeeded };

                // Refresh UI & display success message
                const displayNameEl = document.getElementById("profileDisplayName");
                if (displayNameEl) displayNameEl.innerText = fullName || "User Account";

                const msg = document.getElementById("profileSaveMsg");
                if (msg) {
                    msg.innerText = "Profile details saved successfully! ✅";
                    msg.classList.remove("hidden");
                    setTimeout(() => msg.classList.add("hidden"), 3500);
                }

                // Re-lock fields on successful write
                lockProfileFields();
            } catch (error) {
                console.error("Firestore write failed:", error);
                alert("Failed to save profile: " + (error.message || "Firestore write failed"));
            } finally {
                if (saveProfileBtn) {
                    saveProfileBtn.innerText = "💾 Save Profile Details";
                    saveProfileBtn.disabled = false;
                }
            }
        });
    }

    // Trigger Password Reset Email Button
    if (sendPasswordResetBtn) {
        sendPasswordResetBtn.addEventListener("click", async () => {
            const user = auth.currentUser;
            if (!user || !user.email) {
                alert("Please log in with a valid account to reset your password.");
                return;
            }

            sendPasswordResetBtn.disabled = true;
            sendPasswordResetBtn.innerText = "Sending Reset Email...";

            try {
                await sendUserPasswordResetEmail(user.email);
                if (resetEmailMsg) {
                    resetEmailMsg.innerText = `Password reset email sent to ${user.email}! ✅`;
                    resetEmailMsg.className = "text-xs text-center font-semibold text-emerald-400 pt-0.5";
                    resetEmailMsg.classList.remove("hidden");
                }
            } catch (err) {
                console.error("Password reset error:", err);
                if (resetEmailMsg) {
                    resetEmailMsg.innerText = `Failed: ${err.message}`;
                    resetEmailMsg.className = "text-xs text-center font-semibold text-red-400 pt-0.5";
                    resetEmailMsg.classList.remove("hidden");
                }
            } finally {
                sendPasswordResetBtn.disabled = false;
                sendPasswordResetBtn.innerText = "🔑 Send Password Reset Email";
            }
        });
    }

    // Delete Account Modal Controls
    if (deleteAccountBtn && deleteAccountModal) {
        deleteAccountBtn.addEventListener("click", () => {
            if (deleteAccountError) deleteAccountError.classList.add("hidden");
            const pwdInput = document.getElementById("deleteAccountPassword");
            if (pwdInput) pwdInput.value = "";
            deleteAccountModal.classList.remove("hidden");
        });
    }

    if (cancelDeleteAccountBtn && deleteAccountModal) {
        cancelDeleteAccountBtn.addEventListener("click", () => {
            deleteAccountModal.classList.add("hidden");
        });
    }

    if (confirmDeleteAccountBtn) {
        confirmDeleteAccountBtn.addEventListener("click", async () => {
            const user = auth.currentUser;
            if (!user) return;

            const pwdInput = document.getElementById("deleteAccountPassword");
            const enteredPassword = pwdInput ? pwdInput.value.trim() : "";

            confirmDeleteAccountBtn.disabled = true;
            confirmDeleteAccountBtn.innerText = "Deleting Account...";

            try {
                await deleteUserAccountWithPassword(enteredPassword);
                deleteAccountModal.classList.add("hidden");
                if (profileDrawer) profileDrawer.classList.add("hidden");

                // Clear local storage and session
                sessionStorage.clear();
                localStorage.removeItem("findmycollege_bookmarks");

                // Redirect to Auth Screen with success message
                window.location.hash = "#signin";
                openAuthModal("✅ Account deleted successfully.", "signin");
            } catch (err) {
                console.error("Account deletion error:", err);
                if (deleteAccountError) {
                    deleteAccountError.innerText = err.message || "Incorrect password. Please try again.";
                    deleteAccountError.classList.remove("hidden");
                }
            } finally {
                confirmDeleteAccountBtn.disabled = false;
                confirmDeleteAccountBtn.innerText = "Confirm Delete";
            }
        });
    }

    // Listen for bookmarks changes to update profile badge in real time
    window.addEventListener("bookmarksChanged", () => {
        if (auth.currentUser) {
            updateProfileBookmarksCount();
        }
    });
}

export async function updateProfileUI(user) {
    const profileEmail = document.getElementById("profileEmail");
    const profileDisplayName = document.getElementById("profileDisplayName");
    const profileStatus = document.getElementById("profileStatus");
    const profileFullName = document.getElementById("profileFullName");
    const profilePhone = document.getElementById("profilePhone");
    const profileBio = document.getElementById("profileBio");
    const profileProgram = document.getElementById("profileProgram");

    // Lock fields by default when drawer opens or updates
    const editProfileBtn = document.getElementById("editProfileBtn");
    const saveProfileBtn = document.getElementById("saveProfileBtn");
    const cancelProfileBtn = document.getElementById("cancelProfileBtn");

    if (profileFullName) profileFullName.disabled = true;
    if (profilePhone) profilePhone.disabled = true;
    if (profileBio) profileBio.disabled = true;
    if (profileProgram) profileProgram.disabled = true;

    if (editProfileBtn) editProfileBtn.classList.remove("hidden");
    if (saveProfileBtn) saveProfileBtn.classList.add("hidden");
    if (cancelProfileBtn) cancelProfileBtn.classList.add("hidden");

    if (user && user.uid) {
        if (profileEmail) profileEmail.innerText = user.email || "User Account";
        
        const affiliation = getStudentAffiliation(user.email);
        if (profileStatus) {
            if (affiliation.isVerified) {
                profileStatus.innerHTML = `<span class="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 font-bold text-xs shadow-sm">${affiliation.badgeText}</span>`;
            } else {
                profileStatus.innerHTML = `<span class="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-slate-800 text-slate-400 border border-slate-700 font-semibold text-xs">Standard Account</span>`;
            }
        }

        // Update real-time bookmarks counter
        updateProfileBookmarksCount();

        try {
            // Fetch student profile from Firebase Firestore 'users/{uid}'
            const firestoreData = await getUserProfile(user.uid);

            const name = firestoreData?.fullName || firestoreData?.displayName || firestoreData?.studentName || user.displayName || (user.email ? user.email.split("@")[0] : "Student");
            const phone = firestoreData?.phone || "";
            const bio = firestoreData?.bio || "";
            const program = firestoreData?.programNeeded || firestoreData?.program || "B.Tech - Computer Science (CSE)";
            const photoURL = firestoreData?.photoURL || user.photoURL || null;

            cachedProfileData = { fullName: name, phone, bio, programNeeded: program, photoURL };

            if (profileDisplayName) profileDisplayName.innerText = name;
            if (profileFullName) profileFullName.value = name;
            if (profilePhone) profilePhone.value = phone;
            if (profileBio) profileBio.value = bio;
            if (profileProgram) profileProgram.value = program;

            if (photoURL) {
                updateAvatarDisplay(photoURL);
            } else {
                updateAvatarInitials(name);
            }
        } catch (err) {
            console.error("Error loading profile from Firestore:", err);
        }
    } else {
        if (profileEmail) profileEmail.innerText = "Guest Mode";
        if (profileDisplayName) profileDisplayName.innerText = "Guest User";
        if (profileStatus) profileStatus.innerText = "Guest Session";
        updateAvatarInitials("Guest");
    }
}

function updateProfileBookmarksCount() {
    const badge = document.getElementById("profileBookmarksBadge");
    if (badge) {
        const count = getBookmarkedIds().length;
        badge.innerText = `${count} Saved`;
    }
}

function updateAvatarDisplay(photoURL) {
    const profileAvatarImg = document.getElementById("profileAvatarImg");
    const profileAvatarText = document.getElementById("profileAvatarText");

    if (profileAvatarImg && photoURL) {
        profileAvatarImg.src = photoURL;
        profileAvatarImg.classList.remove("hidden");
        if (profileAvatarText) profileAvatarText.classList.add("hidden");
    }
}

function updateAvatarInitials(name) {
    const firstLetter = name && name.length > 0 ? name.charAt(0).toUpperCase() : "S";
    const headerUserInitial = document.getElementById("headerUserInitial");
    const profileAvatarText = document.getElementById("profileAvatarText");
    const profileAvatarImg = document.getElementById("profileAvatarImg");

    if (headerUserInitial) headerUserInitial.innerText = firstLetter;
    if (profileAvatarText) {
        profileAvatarText.innerText = firstLetter;
        profileAvatarText.classList.remove("hidden");
    }
    if (profileAvatarImg && (!profileAvatarImg.src || profileAvatarImg.src === window.location.href)) {
        profileAvatarImg.classList.add("hidden");
    }
}