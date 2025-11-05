package com.example.secureshe.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import android.content.Intent
import com.example.secureshe.legal.AIToolsActivity
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.res.painterResource

// Controller exposed to screens
class DrawerController(
    val open: () -> Unit,
    val close: () -> Unit
)

val LocalDrawerController = staticCompositionLocalOf<DrawerController> {
    DrawerController(open = {}, close = {})
}

@Composable
fun AppDrawerHost(
    navController: NavController,
    openInitially: Boolean = false,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Get current route to conditionally disable drawer on auth screen
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val isAuthScreen = currentRoute == "auth"

    LaunchedEffect(openInitially) {
        if (openInitially && !isAuthScreen) drawerState.open()
    }

    val controller = remember {
        DrawerController(
            open = { 
                if (!isAuthScreen) {
                    scope.launch { drawerState.open() }
                }
            },
            close = { scope.launch { drawerState.close() } }
        )
    }

    CompositionLocalProvider(LocalDrawerController provides controller) {
        if (isAuthScreen) {
            // On auth screen, render content without drawer
            content()
        } else {
            // On other screens, render with drawer
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = Color.White
                    ) {
                        AppDrawerContent(navController = navController, onClose = { controller.close() })
                    }
                }
            ) {
                content()
            }
        }
    }
}

@Composable
private fun AppDrawerContent(navController: NavController, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val authViewModel: com.example.secureshe.ui.viewmodels.AuthViewModel = hiltViewModel()

    fun navigateAndClose(route: String, intent: Intent? = null) {
        scope.launch { onClose() }
        if (intent != null) {
            context.startActivity(intent)
        } else {
            navController.navigate(route)
        }
    }

    Column(modifier = Modifier.fillMaxHeight()) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
        ) {
            Text(
                "Naari Shakti AI",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black
            )
            Text(
                "Your safety, our priority",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black.copy(alpha = 0.7f)
            )
        }
        Divider(modifier = Modifier.padding(horizontal = 12.dp))
        Text(
            "Menu",
            style = MaterialTheme.typography.labelLarge,
            color = Color.Black.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        NavigationDrawerItem(
        label = { Text("Home") },
        selected = false,
        icon = { Icon(Icons.Default.Home, contentDescription = null) },
        onClick = { navigateAndClose("dashboard") },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = Color.Black,
            unselectedIconColor = Color.Black,
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            selectedTextColor = MaterialTheme.colorScheme.primary,
            selectedIconColor = MaterialTheme.colorScheme.primary
        )
        )

        NavigationDrawerItem(
        label = { Text("AI Legal Tools") },
        selected = false,
        icon = { Icon(Icons.Default.Lightbulb, contentDescription = null) },
        onClick = {
            navigateAndClose(route = "dashboard", intent = Intent(context, AIToolsActivity::class.java))
        },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = Color.Black,
            unselectedIconColor = Color.Black,
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            selectedTextColor = MaterialTheme.colorScheme.primary,
            selectedIconColor = MaterialTheme.colorScheme.primary
        )
        )

        NavigationDrawerItem(
        label = { Text("Pro Bono") },
        selected = false,
        icon = { Icon(painterResource(id = com.example.secureshe.R.drawable.ic_pro_bono), contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.Unspecified) },
        onClick = { navigateAndClose("userTypeSelection") },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = Color.Black,
            unselectedIconColor = Color.Black,
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            selectedTextColor = MaterialTheme.colorScheme.primary,
            selectedIconColor = MaterialTheme.colorScheme.primary
        )
        )

        NavigationDrawerItem(
        label = { Text("Safe Path Navigation") },
        selected = false,
        icon = { Icon(painterResource(id = com.example.secureshe.R.drawable.ic_safe_path), contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.Unspecified) },
        onClick = { navigateAndClose("safe_path") },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = Color.Black,
            unselectedIconColor = Color.Black,
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            selectedTextColor = MaterialTheme.colorScheme.primary,
            selectedIconColor = MaterialTheme.colorScheme.primary
        )
        )

        NavigationDrawerItem(
        label = { Text("Profile") },
        selected = false,
        icon = { Icon(Icons.Default.Person, contentDescription = null) },
        onClick = { navigateAndClose("profile") },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = Color.Black,
            unselectedIconColor = Color.Black,
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            selectedTextColor = MaterialTheme.colorScheme.primary,
            selectedIconColor = MaterialTheme.colorScheme.primary
        )
        )

        NavigationDrawerItem(
        label = { Text("Help") },
        selected = false,
        icon = { Icon(Icons.Default.Help, contentDescription = null) },
        onClick = { navigateAndClose("help") },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = Color.Black,
            unselectedIconColor = Color.Black,
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            selectedTextColor = MaterialTheme.colorScheme.primary,
            selectedIconColor = MaterialTheme.colorScheme.primary
        )
        )

        

        Spacer(modifier = Modifier.weight(1f))
        Divider(modifier = Modifier.padding(horizontal = 12.dp))
        NavigationDrawerItem(
        label = { Text("Log out", color = MaterialTheme.colorScheme.error) },
        selected = false,
        icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
            unselectedTextColor = MaterialTheme.colorScheme.error,
            unselectedIconColor = MaterialTheme.colorScheme.error,
            selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
            selectedTextColor = MaterialTheme.colorScheme.onError,
            selectedIconColor = MaterialTheme.colorScheme.onError
        ),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = {
            authViewModel.signOut()
            scope.launch { onClose() }
            navController.navigate("auth") {
                popUpTo("dashboard") { inclusive = true }
            }
        }
        )
    }
}
