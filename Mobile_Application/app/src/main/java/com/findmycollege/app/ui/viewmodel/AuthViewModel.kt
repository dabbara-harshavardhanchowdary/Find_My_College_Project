package com.findmycollege.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Authentication ViewModel for handling official Firebase Email Verification & Login flow.
 */
class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Register a new user and immediately dispatch official Firebase Email Verification link
     */
    fun registerUser(
        fullName: String,
        email: String,
        password: String,
        onVerificationSent: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), password.trim())
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    // Update Auth Display Name
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName.trim())
                        .build()
                    user.updateProfile(profileUpdates)

                    // Immediately send official Firebase verification link
                    user.sendEmailVerification()
                        .addOnSuccessListener {
                            // Update Firestore record
                            val userData = mapOf(
                                "uid" to user.uid,
                                "email" to user.email,
                                "fullName" to fullName.trim(),
                                "emailVerified" to false,
                                "createdAt" to System.currentTimeMillis().toString()
                            )
                            firestore.collection("users").document(user.uid)
                                .set(userData, SetOptions.merge())

                            onVerificationSent(email.trim())
                        }
                        .addOnFailureListener { e ->
                            onError("Account created, but failed to send verification email: ${e.message}")
                        }
                } else {
                    onError("Registration failed: User creation null")
                }
            }
            .addOnFailureListener { e ->
                onError("Registration Error: ${e.message}")
            }
    }

    /**
     * Sign in user and verify that email address has been verified via the link
     */
    fun signInUser(
        email: String,
        password: String,
        onVerifiedSuccess: (FirebaseUser) -> Unit,
        onUnverifiedEmail: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email.trim(), password.trim())
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    // Reload user token to ensure fresh emailVerified status
                    user.reload().addOnCompleteListener {
                        if (user.isEmailVerified) {
                            onVerifiedSuccess(user)
                        } else {
                            onUnverifiedEmail(user)
                        }
                    }
                } else {
                    onError("Sign in failed")
                }
            }
            .addOnFailureListener { e ->
                onError("Sign In Error: ${e.message}")
            }
    }

    /**
     * Resend verification email link to active user
     */
    fun resendVerificationEmail(
        user: FirebaseUser?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val targetUser = user ?: auth.currentUser
        if (targetUser != null) {
            targetUser.sendEmailVerification()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onError("Resend Link Error: ${e.message}") }
        } else {
            onError("No active user session found to resend verification link.")
        }
    }

    /**
     * Send official Firebase Password Reset Email
     */
    fun sendPasswordResetEmail(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val targetEmail = email.trim()
        if (targetEmail.isEmpty()) {
            onError("Please enter your registered email address.")
            return
        }
        auth.sendPasswordResetEmail(targetEmail)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError("Failed to send reset email: ${e.message}") }
    }
}
