package com.fubon.fxmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.fubon.fxmonitor.data.local.UserPreferencesRepository
import com.fubon.fxmonitor.ui.alert.AlertScreen
import com.fubon.fxmonitor.ui.alert.AlertViewModel
import com.fubon.fxmonitor.ui.calculator.CalculatorScreen
import com.fubon.fxmonitor.ui.calculator.CalculatorViewModel
import com.fubon.fxmonitor.ui.detail.DetailScreen
import com.fubon.fxmonitor.ui.detail.DetailViewModel
import com.fubon.fxmonitor.ui.home.HomeScreen
import com.fubon.fxmonitor.ui.home.HomeViewModel
import com.fubon.fxmonitor.ui.settings.SettingsScreen
import com.fubon.fxmonitor.ui.settings.SettingsViewModel
import com.fubon.fxmonitor.ui.theme.FubonFXMonitorTheme
import com.fubon.fxmonitor.worker.ExchangeMonitorWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPrefs: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read background interval from preferences and schedule worker
        val targetCurrency = intent?.getStringExtra("target_currency")

        setContent {
            val darkMode by userPrefs.darkMode.collectAsState(initial = isSystemInDarkTheme())
            val intervalMins by userPrefs.backgroundIntervalMins.collectAsState(initial = 15)

            LaunchedEffect(intervalMins) {
                ExchangeMonitorWorker.schedule(this@MainActivity, intervalMins.toLong())
            }

            FubonFXMonitorTheme(darkTheme = darkMode) {
                AppNavigation(startTargetCurrency = targetCurrency)
            }
        }
    }
}

@Composable
fun AppNavigation(startTargetCurrency: String?) {
    val navController = rememberNavController()

    val startDestination = if (startTargetCurrency != null) {
        "detail/$startTargetCurrency"
    } else {
        "home"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                onCurrencyClick = { code -> navController.navigate("detail/$code") },
                onNavigateToAlerts = { navController.navigate("alerts") },
                onNavigateToCalculator = { navController.navigate("calculator") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }

        composable(
            route = "detail/{currency}",
            arguments = listOf(navArgument("currency") { type = NavType.StringType })
        ) {
            val viewModel: DetailViewModel = hiltViewModel()
            DetailScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onAddAlertClick = { code -> navController.navigate("alerts_add/$code") }
            )
        }

        composable("alerts") {
            val viewModel: AlertViewModel = hiltViewModel()
            AlertScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = "alerts_add/{currency}",
            arguments = listOf(navArgument("currency") { type = NavType.StringType })
        ) { backStackEntry ->
            val currencyCode = backStackEntry.arguments?.getString("currency")
            val viewModel: AlertViewModel = hiltViewModel()
            LaunchedEffect(currencyCode) {
                viewModel.openAddDialog(currencyCode)
            }
            AlertScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("calculator") {
            val viewModel: CalculatorViewModel = hiltViewModel()
            CalculatorScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("settings") {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
