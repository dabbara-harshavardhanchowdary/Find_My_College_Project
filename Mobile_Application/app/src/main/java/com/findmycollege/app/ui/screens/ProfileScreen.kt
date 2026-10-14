package com.findmycollege.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.findmycollege.app.data.VerificationRepository
import com.findmycollege.app.ui.theme.*
import com.findmycollege.app.ui.viewmodel.CollegeViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: CollegeViewModel,
    onNavigateToAuth: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val currentUser = auth.currentUser

    // Locked vs Edit Mode State
    var isEditing by remember { mutableStateOf(false) }

    // User Profile Input States
    var fullName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var phone by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var program by remember { mutableStateOf("B.Tech - Computer Science (CSE)") }
    var photoUrl by remember { mutableStateOf<String?>(currentUser?.photoUrl?.toString()) }
    var bookmarkedCount by remember { mutableStateOf(0) }

    // Cached Initial States for Cancel restoration
    var initialFullName by remember { mutableStateOf("") }
    var initialPhone by remember { mutableStateOf("") }
    var initialBio by remember { mutableStateOf("") }
    var initialProgram by remember { mutableStateOf("B.Tech - Computer Science (CSE)") }

    var isLoading by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var deleteErrorMsg by remember { mutableStateOf<String?>(null) }
    var isDeletingAccount by remember { mutableStateOf(false) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    val base64Str = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                    photoUrl = base64Str

                    currentUser?.uid?.let { uid ->
                        val photoUpdate = mapOf(
                            "photoURL" to base64Str,
                            "photoUrl" to base64Str,
                            "updatedAt" to System.currentTimeMillis().toString()
                        )
                        firestore.collection("users").document(uid)
                            .set(photoUpdate, SetOptions.merge())
                            .addOnSuccessListener {
                                Toast.makeText(context, "Profile picture updated! 📷", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fetch Profile & Bookmarks count from Firestore
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val fn = doc.getString("fullName") ?: doc.getString("displayName") ?: currentUser.displayName ?: ""
                        val ph = doc.getString("phone") ?: ""
                        val bi = doc.getString("bio") ?: ""
                        val pr = doc.getString("programNeeded") ?: doc.getString("program") ?: "B.Tech - Computer Science (CSE)"
                        val img = doc.getString("photoURL") ?: doc.getString("photoUrl")

                        fullName = fn
                        phone = ph
                        bio = bi
                        program = pr
                        if (!img.isNullOrBlank()) photoUrl = img

                        initialFullName = fn
                        initialPhone = ph
                        initialBio = bi
                        initialProgram = pr

                        val bookmarks = doc.get("bookmarkedCollegeIds") as? List<*>
                        bookmarkedCount = bookmarks?.size ?: 0
                    }
                }
        }
    }

    if (currentUser == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Guest Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
                )
            },
            containerColor = Slate900
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = Slate950.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Slate800)
                                .border(2.dp, Slate700, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", fontSize = 36.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Guest User Mode",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Sign in to save colleges, bookmark shortlists, and edit your personalized student profile.",
                            color = Slate400,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onNavigateToAuth,
                            colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🔑 Sign In / Register Account", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        },
        containerColor = Slate900
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Profile Header Card
            Surface(
                color = Slate950.copy(alpha = 0.8f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Bookmarks Counter Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "🔖 $bookmarkedCount Saved",
                                color = Color(0xFFFBBF24),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Avatar Circle with Camera Overlay
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Blue600, Color(0xFF4F46E5))))
                            .border(2.dp, Blue400.copy(alpha = 0.5f), CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        val parsedBitmap = photoUrl?.let { decodeBase64ToBitmap(it) }
                        if (parsedBitmap != null) {
                            Image(
                                bitmap = parsedBitmap.asImageBitmap(),
                                contentDescription = "Profile Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val initial = if (fullName.isNotBlank()) fullName.first().uppercase() else "S"
                            Text(
                                text = initial,
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        // Camera Overlay Icon
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Upload Photo",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (fullName.isNotBlank()) fullName else (currentUser?.displayName ?: "User Account"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Text(
                        text = currentUser?.email ?: "user@example.com",
                        color = Slate400,
                        fontSize = 12.sp
                    )

                    val affiliation = remember(currentUser?.email) {
                        VerificationRepository.getAffiliation(currentUser?.email)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = if (affiliation.isVerified) Emerald400.copy(alpha = 0.15f) else Slate800,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (affiliation.isVerified) Emerald400.copy(alpha = 0.3f) else Slate700
                        )
                    ) {
                        Text(
                            text = affiliation.badgeText,
                            color = if (affiliation.isVerified) Emerald400 else Slate400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Editable Profile Form (Locked vs Edit Mode)
            Surface(
                color = Slate950.copy(alpha = 0.8f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Student Details",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    // Full Name
                    Column {
                        Text("Full Name *", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            placeholder = { Text("e.g. Rahul Sharma", color = Slate400, fontSize = 12.sp) },
                            enabled = isEditing,
                            readOnly = !isEditing,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate900,
                                unfocusedContainerColor = Slate900,
                                disabledContainerColor = Slate950,
                                focusedBorderColor = Blue600,
                                unfocusedBorderColor = Slate800,
                                disabledBorderColor = Slate800.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Slate300
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Phone Number
                    Column {
                        Text("Phone Number", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            placeholder = { Text("e.g. 9876543210", color = Slate400, fontSize = 12.sp) },
                            enabled = isEditing,
                            readOnly = !isEditing,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate900,
                                unfocusedContainerColor = Slate900,
                                disabledContainerColor = Slate950,
                                focusedBorderColor = Blue600,
                                unfocusedBorderColor = Slate800,
                                disabledBorderColor = Slate800.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Slate300
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Bio / Academic Goal
                    Column {
                        Text("Bio / Academic Goal", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            placeholder = { Text("e.g. Aspiring CSE student targeting Top 10 NIRF IITs/NITs...", color = Slate400, fontSize = 12.sp) },
                            enabled = isEditing,
                            readOnly = !isEditing,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate900,
                                unfocusedContainerColor = Slate900,
                                disabledContainerColor = Slate950,
                                focusedBorderColor = Blue600,
                                unfocusedBorderColor = Slate800,
                                disabledBorderColor = Slate800.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Slate300
                            ),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Program Needed
                    Column {
                        Text("Program Needed", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = program,
                            onValueChange = { program = it },
                            placeholder = { Text("e.g. B.Tech - Computer Science (CSE)", color = Slate400, fontSize = 12.sp) },
                            enabled = isEditing,
                            readOnly = !isEditing,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate900,
                                unfocusedContainerColor = Slate900,
                                disabledContainerColor = Slate950,
                                focusedBorderColor = Blue600,
                                unfocusedBorderColor = Slate800,
                                disabledBorderColor = Slate800.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Slate300
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Locked Mode vs Edit Mode Action Buttons
                    if (!isEditing) {
                        Button(
                            onClick = { isEditing = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("✏️ Edit Profile", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (currentUser == null) return@Button
                                    isLoading = true

                                    // Update Firebase Auth displayName
                                    if (fullName.isNotBlank()) {
                                        val profileUpdates = UserProfileChangeRequest.Builder()
                                            .setDisplayName(fullName.trim())
                                            .build()
                                        currentUser.updateProfile(profileUpdates)
                                    }

                                    // Save to Firestore
                                    val profileMap = mapOf(
                                        "fullName" to fullName.trim(),
                                        "displayName" to fullName.trim(),
                                        "phone" to phone.trim(),
                                        "bio" to bio.trim(),
                                        "programNeeded" to program.trim(),
                                        "updatedAt" to System.currentTimeMillis().toString()
                                    )

                                    firestore.collection("users").document(currentUser.uid)
                                        .set(profileMap, SetOptions.merge())
                                        .addOnSuccessListener {
                                            isLoading = false
                                            initialFullName = fullName
                                            initialPhone = phone
                                            initialBio = bio
                                            initialProgram = program
                                            isEditing = false
                                            Toast.makeText(context, "Profile details saved successfully! ✅", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            Toast.makeText(context, "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald400),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Slate950, modifier = Modifier.size(18.dp))
                                } else {
                                    Text("💾 Save Profile Details", color = Slate950, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    fullName = initialFullName
                                    phone = initialPhone
                                    bio = initialBio
                                    program = initialProgram
                                    isEditing = false
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Account Quick Actions (Password Reset, Sign Out, Delete Account)
            Surface(
                color = Slate950.copy(alpha = 0.8f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Account Actions",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    // Send Password Reset Email Button
                    OutlinedButton(
                        onClick = {
                            val email = currentUser?.email
                            if (email.isNullOrBlank()) {
                                Toast.makeText(context, "No email address found for this user.", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            auth.sendPasswordResetEmail(email)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Password reset email sent to $email! 🔑", Toast.LENGTH_LONG).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔑 Send Password Reset Email", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Sign Out Button
                    Button(
                        onClick = {
                            auth.signOut()
                            Toast.makeText(context, "Signed out successfully 👋", Toast.LENGTH_SHORT).show()
                            onNavigateToAuth()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🚪 Sign Out", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Delete Account Button
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🗑️ Delete Account", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Delete Account Confirmation Dialog with In-Place Re-authentication
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deletePassword = ""
                deleteErrorMsg = null
            },
            title = { Text("Delete Account?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Are you sure you want to delete your account? This action is permanent and cannot be undone.",
                        color = Slate300,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = {
                            deletePassword = it
                            deleteErrorMsg = null
                        },
                        placeholder = { Text("Confirm Your Password *", color = Slate400, fontSize = 11.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Slate950,
                            unfocusedContainerColor = Slate950,
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    deleteErrorMsg?.let { err ->
                        Text(
                            text = err,
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val user = currentUser
                        if (user == null) return@Button
                        val email = user.email ?: ""

                        if (deletePassword.isBlank()) {
                            deleteErrorMsg = "Incorrect password. Please try again."
                            return@Button
                        }

                        isDeletingAccount = true
                        deleteErrorMsg = null

                        try {
                            val credential = EmailAuthProvider.getCredential(email, deletePassword.trim())
                            user.reauthenticate(credential)
                                .addOnSuccessListener {
                                    val uid = user.uid
                                    // 1. Delete user document from Firestore
                                    firestore.collection("users").document(uid).delete()

                                    // 2. Delete user account from Auth
                                    user.delete()
                                        .addOnSuccessListener {
                                            isDeletingAccount = false
                                            showDeleteDialog = false
                                            deletePassword = ""
                                            Toast.makeText(context, "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                                            onNavigateToAuth()
                                        }
                                        .addOnFailureListener { e ->
                                            isDeletingAccount = false
                                            deleteErrorMsg = "Account deletion error: ${e.message}"
                                        }
                                }
                                .addOnFailureListener { err ->
                                    isDeletingAccount = false
                                    deleteErrorMsg = "Incorrect password. Please try again."
                                }
                        } catch (e: Exception) {
                            isDeletingAccount = false
                            deleteErrorMsg = "Incorrect password. Please try again."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    enabled = !isDeletingAccount
                ) {
                    if (isDeletingAccount) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Confirm Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        deletePassword = ""
                        deleteErrorMsg = null
                    }
                ) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// Base64 Image Decoder Helper
private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val pureBase64 = if (base64Str.contains(",")) base64Str.split(",")[1] else base64Str
        val bytes = Base64.decode(pureBase64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}
