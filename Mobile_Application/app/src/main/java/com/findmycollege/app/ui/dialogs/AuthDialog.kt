package com.findmycollege.app.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.findmycollege.app.ui.theme.*
import com.findmycollege.app.ui.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

@Composable
fun AuthDialog(
    gatingMessage: String? = null,
    onDismiss: () -> Unit,
    onSuccessLogin: () -> Unit
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val authViewModel = remember { AuthViewModel() }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Sign In, 1 = Register

    // Form inputs
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // State for Verification Email Sent View
    var isVerificationSent by remember { mutableStateOf(false) }
    var sentEmailAddress by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Slate900,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Icon & Title
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Blue600.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎓", fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Find My College",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
                Text(
                    text = "Explore NIRF Ranked Colleges & Insights",
                    color = Slate400,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Optional Auth Gating Alert Banner
                gatingMessage?.let { msg ->
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = msg,
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Tab Switcher Row (Sign In / Register)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate950, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 0) Blue600 else Color.Transparent)
                            .clickable {
                                selectedTab = 0
                                isVerificationSent = false
                                errorMessage = null
                                successMessage = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sign In", color = if (selectedTab == 0) Color.White else Slate400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) Blue600 else Color.Transparent)
                            .clickable {
                                selectedTab = 1
                                isVerificationSent = false
                                errorMessage = null
                                successMessage = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Register", color = if (selectedTab == 1) Color.White else Slate400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Alert Messages (Error / Success)
                errorMessage?.let { err ->
                    Surface(
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = err,
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            if (err.contains("Email Not Verified", ignoreCase = true)) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        auth.currentUser?.sendEmailVerification()
                                            ?.addOnSuccessListener {
                                                Toast.makeText(context, "Verification email link resent!", Toast.LENGTH_SHORT).show()
                                            }
                                            ?.addOnFailureListener { e ->
                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("📧 Resend Verification Email", color = Slate950, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                successMessage?.let { msg ->
                    Surface(
                        color = Emerald400.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald400.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = msg,
                            color = Emerald400,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (selectedTab == 0) {
                    // SIGN IN TAB
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("Email Address", color = Slate400, fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate950,
                                unfocusedContainerColor = Slate950,
                                focusedBorderColor = Blue600,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Password", color = Slate400, fontSize = 12.sp) },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate950,
                                unfocusedContainerColor = Slate950,
                                focusedBorderColor = Blue600,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Forgot Password?",
                            color = Blue600,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable {
                                    if (email.isBlank()) {
                                        errorMessage = "Please enter your registered email address first."
                                    } else {
                                        isLoading = true
                                        authViewModel.sendPasswordResetEmail(
                                            email = email,
                                            onSuccess = {
                                                isLoading = false
                                                errorMessage = null
                                                successMessage = "Password reset link sent to your email inbox! Please check your spam folder as well."
                                                Toast.makeText(context, "Reset email sent to $email", Toast.LENGTH_LONG).show()
                                            },
                                            onError = { err ->
                                                isLoading = false
                                                errorMessage = err
                                            }
                                        )
                                    }
                                }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter email and password."
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                successMessage = null

                                authViewModel.signInUser(
                                    email = email,
                                    password = password,
                                    onVerifiedSuccess = {
                                        isLoading = false
                                        onSuccessLogin()
                                        onDismiss()
                                    },
                                    onUnverifiedEmail = { unverifiedUser ->
                                        isLoading = false
                                        errorMessage = "⚠️ Email Not Verified: Please verify your email link (${unverifiedUser.email}) before proceeding."
                                    },
                                    onError = { err ->
                                        isLoading = false
                                        errorMessage = err
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            else Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                } else {
                    // REGISTER TAB (FIREBASE OFFICIAL EMAIL VERIFICATION)
                    if (!isVerificationSent) {
                        // STEP 1 FORM
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                placeholder = { Text("Full Name *", color = Slate400, fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Slate950,
                                    unfocusedContainerColor = Slate950,
                                    focusedBorderColor = Blue600,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = { Text("Email Address *", color = Slate400, fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Slate950,
                                    unfocusedContainerColor = Slate950,
                                    focusedBorderColor = Blue600,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = { Text("Password (min 6 chars) *", color = Slate400, fontSize = 12.sp) },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Slate950,
                                    unfocusedContainerColor = Slate950,
                                    focusedBorderColor = Blue600,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                placeholder = { Text("Confirm Password *", color = Slate400, fontSize = 12.sp) },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Slate950,
                                    unfocusedContainerColor = Slate950,
                                    focusedBorderColor = Blue600,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    if (fullName.isBlank()) { errorMessage = "Please enter Full Name."; return@Button }
                                    if (email.isBlank()) { errorMessage = "Please enter Email Address."; return@Button }
                                    if (password.length < 6) { errorMessage = "Password must be at least 6 characters."; return@Button }
                                    if (password != confirmPassword) { errorMessage = "Passwords do not match."; return@Button }

                                    isLoading = true
                                    errorMessage = null

                                    authViewModel.registerUser(
                                        fullName = fullName,
                                        email = email,
                                        password = password,
                                        onVerificationSent = { targetEmail ->
                                            isLoading = false
                                            sentEmailAddress = targetEmail
                                            isVerificationSent = true
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                else Text("Create Account & Send Verification Link", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                    } else {
                        // VERIFICATION EMAIL SENT CONFIRMATION SCREEN
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = Slate950,
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Blue600.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("📧", fontSize = 36.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Verification Email Sent!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "We have sent a verification link to $sentEmailAddress. Please click the link in your inbox (or spam folder) to activate your account.",
                                        color = Slate300,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    authViewModel.resendVerificationEmail(
                                        user = auth.currentUser,
                                        onSuccess = {
                                            Toast.makeText(context, "Verification email link resent to $sentEmailAddress!", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📧 Resend Verification Link", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            TextButton(onClick = {
                                isVerificationSent = false
                                selectedTab = 0
                            }) {
                                Text("← Back to Sign In", color = Slate400, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("Continue as Guest / Skip for Now →", color = Slate400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
