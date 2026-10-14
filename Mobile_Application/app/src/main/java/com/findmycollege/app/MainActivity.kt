package com.findmycollege.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.findmycollege.app.ui.dialogs.AuthDialog
import com.findmycollege.app.ui.screens.AboutScreen
import com.findmycollege.app.ui.screens.BookmarksScreen
import com.findmycollege.app.ui.screens.ChatScreen
import com.findmycollege.app.ui.screens.CollegesScreen
import com.findmycollege.app.ui.screens.CompareScreen
import com.findmycollege.app.ui.screens.HomeScreen
import com.findmycollege.app.ui.screens.ProfileScreen
import com.findmycollege.app.ui.theme.Blue400
import com.findmycollege.app.ui.theme.Blue600
import com.findmycollege.app.ui.theme.FindMyCollegeTheme
import com.findmycollege.app.ui.theme.Slate800
import com.findmycollege.app.ui.theme.Slate900
import com.findmycollege.app.ui.theme.Slate950
import com.findmycollege.app.ui.viewmodel.CollegeViewModel
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Colleges : Screen("colleges", "Colleges", Icons.Default.School)
    object Compare : Screen("compare", "Compare", Icons.Default.CompareArrows)
    object Saved : Screen("saved", "Saved", Icons.Default.Bookmark)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object About : Screen("about", "About", Icons.Default.Info)
    object Chat : Screen("chat", "AI Chat", Icons.Default.AutoAwesome)
}

class MainActivity : ComponentActivity() {
    private val viewModel: CollegeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FindMyCollegeTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: CollegeViewModel) {
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.Colleges, Screen.Compare, Screen.Saved, Screen.About)
    var showSavedGatingDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = Slate900,
        bottomBar = {
            NavigationBar(
                containerColor = Slate950,
                tonalElevation = 8.dp
            ) {
                items.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, fontSize = 10.sp) },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Blue400,
                            selectedTextColor = Blue400,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            indicatorColor = Slate800
                        ),
                        onClick = {
                            if (screen == Screen.Saved && FirebaseAuth.getInstance().currentUser == null) {
                                showSavedGatingDialog = true
                                return@NavigationBarItem
                            }

                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentRoute != Screen.Chat.route) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.Chat.route) },
                    containerColor = Blue600,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.border(1.dp, Blue400.copy(alpha = 0.4f), CircleShape)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🤖 ", fontSize = 16.sp)
                        Text("Ask AI", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToColleges = { navController.navigate(Screen.Colleges.route) },
                        onNavigateToCompare = { navController.navigate(Screen.Compare.route) },
                        onNavigateToSaved = {
                            if (FirebaseAuth.getInstance().currentUser == null) {
                                showSavedGatingDialog = true
                            } else {
                                navController.navigate(Screen.Saved.route)
                            }
                        },
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                        onNavigateToChat = { navController.navigate(Screen.Chat.route) }
                    )
                }
                composable(Screen.Colleges.route) {
                    CollegesScreen(viewModel = viewModel)
                }
                composable(Screen.Compare.route) {
                    CompareScreen(viewModel = viewModel)
                }
                composable(Screen.Saved.route) {
                    BookmarksScreen(viewModel = viewModel)
                }
                composable(Screen.About.route) {
                    AboutScreen()
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        viewModel = viewModel,
                        onNavigateToAuth = { showSavedGatingDialog = true },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Chat.route) {
                    ChatScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    if (showSavedGatingDialog) {
        AuthDialog(
            gatingMessage = "🔒 Unlock Your Personal Experience: Register or Sign In to save shortlists and sync across devices.",
            onDismiss = { showSavedGatingDialog = false },
            onSuccessLogin = {
                showSavedGatingDialog = false
                navController.navigate(Screen.Saved.route)
            }
        )
    }
}
