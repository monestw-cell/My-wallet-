package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.features.auth.LockScreen
import com.example.features.auth.SecurityPrefs
import com.example.features.debts.DebtsScreen
import com.example.features.reports.ReportsScreen
import com.example.features.settings.SettingsScreen
import com.example.features.transactions.TransactionsScreen
import com.example.features.wallet.HomeScreen
import com.example.features.wallet.WalletViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = SecurityPrefs(applicationContext)

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                // Force RTL orientation layout for Arabic app alignment
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    var isLocked by remember { mutableStateOf(prefs.isLockEnabled()) }

                    if (isLocked) {
                        LockScreen(
                            prefs = prefs,
                            onUnlocked = { isLocked = false }
                        )
                    } else {
                        MainNavigationContainer(
                            prefs = prefs,
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = { isDarkTheme = it }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationContainer(
    prefs: SecurityPrefs,
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit
) {
    val walletViewModel: WalletViewModel = viewModel()
    var currentTab by remember { mutableStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                tonalElevation = 8.dp
            ) {
                listOf(
                    Triple("الرئيسية", Icons.Default.Home, 0),
                    Triple("المعاملات", Icons.Default.ReceiptLong, 1),
                    Triple("الديون", Icons.Default.People, 2),
                    Triple("التقارير", Icons.Default.Leaderboard, 3),
                    Triple("الإعدادات", Icons.Default.Settings, 4)
                ).forEach { item ->
                    NavigationBarItem(
                        selected = currentTab == item.third,
                        onClick = { currentTab = item.third },
                        icon = { Icon(imageVector = item.second, contentDescription = item.first) },
                        label = { Text(text = item.first, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> HomeScreen(
                    viewModel = walletViewModel,
                    onNavigateToTransactions = { currentTab = 1 },
                    onNavigateToDebts = { currentTab = 2 },
                    onNavigateToReports = { currentTab = 3 }
                )
                1 -> TransactionsScreen(
                    viewModel = walletViewModel
                )
                2 -> DebtsScreen(
                    viewModel = walletViewModel
                )
                3 -> ReportsScreen(
                    viewModel = walletViewModel
                )
                4 -> SettingsScreen(
                    viewModel = walletViewModel,
                    prefs = prefs,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle
                )
            }
        }
    }
}
