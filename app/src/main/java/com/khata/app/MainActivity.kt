package com.khata.app

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    private val vm: KhataVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val theme by vm.theme.collectAsState()
            val t = theme
            if (t != null) {
                val dark = when (t) { "DARK" -> true; "LIGHT" -> false; else -> isSystemInDarkTheme() }
                LaunchedEffect(dark) {
                    val style = if (dark) SystemBarStyle.dark(AColor.TRANSPARENT) else SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT)
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
                KKhataRoot(dark)
            }
        }
    }

    @Composable
    private fun KKhataRoot(dark: Boolean) {
        KhataTheme(dark) { Surface { KhataApp(vm) } }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun KhataApp(vm: KhataVM) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val tabs = listOf(
        Tab("customers", "Customers", Icons.Default.AccountBox),
        Tab("calc", "Calculator", Icons.Default.Build),
        Tab("more", "More", Icons.Default.MoreVert),
        Tab("profile", "Profile", Icons.Default.Person)
    )
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (route in tabs.map { it.route }) NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo("customers") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "customers", modifier = Modifier.padding(pad)) {
            composable("customers") { CustomersScreen(vm, nav) }
            composable("calc") { CalculatorScreen() }
            composable("more") { MoreScreen(nav) }
            composable("profile") { ProfileScreen(vm) }
            composable("customer_form?id={id}", arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })) {
                CustomerFormScreen(vm, nav, it.arguments?.getLong("id") ?: -1L)
            }
            composable("ledger/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                LedgerScreen(vm, nav, it.arguments!!.getLong("id"))
            }
            composable("all_txns") { AllTxnsScreen(vm, nav) }
            composable("settings") { SettingsScreen(vm, nav) }
            composable("about") { AboutScreen(nav) }
        }
    }
}
