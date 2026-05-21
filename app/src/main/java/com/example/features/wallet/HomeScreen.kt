package com.example.features.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.ExchangeRateEntity
import com.example.core.database.TransactionEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: WalletViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    val balances by viewModel.walletBalances.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val exchangeRateOpt by viewModel.exchangeRate.collectAsState()
    val rates = exchangeRateOpt ?: ExchangeRateEntity(1, 3.7, 5.2)

    val ils = balances["ILS"] ?: 0.0
    val usd = balances["USD"] ?: 0.0
    val jod = balances["JOD"] ?: 0.0

    val totalIls = ils + (usd * rates.usdToIls) + (jod * rates.jodToIls)

    val savingGoals by viewModel.savingGoals.collectAsState()
    val budgetLimitOpt by viewModel.budgetLimit.collectAsState()
    val limitAmount = budgetLimitOpt?.monthlyLimit ?: 0.0

    // Dialog state controllers
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInputStr by remember { mutableStateOf("") }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalNameInput by remember { mutableStateOf("") }
    var goalTargetInput by remember { mutableStateOf("") }
    var goalCurrencyInput by remember { mutableStateOf("ILS") }
    var showDepositDialogForGoal by remember { mutableStateOf<com.example.core.database.SavingGoal?>(null) }
    var depositAmountStr by remember { mutableStateOf("") }
    var isDepositMode by remember { mutableStateOf(true) }

    // Calculate total income and expense from list of transactions
    val totalIncome = transactions.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }

    val isDark = isSystemInDarkTheme()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val cardSurface = MaterialTheme.colorScheme.surface

    val dynamicBorder = if (isDark) Color(0xFF353C3C) else BorderLight
    val dynamicBorderDetail = if (isDark) Color(0xFF3F4646) else BorderDetail
    val dynamicMintBg = if (isDark) Color(0xFF153333) else MintBgSoft
    val dynamicRedLightBg = if (isDark) Color(0xFF451919) else RedLight
    val dynamicBlueLightBg = if (isDark) Color(0xFF1E2847) else BlueLight
    val dynamicMintLight = if (isDark) Color(0xFF1C4D4D) else MintLight

    // Calculate total expenses for the current calendar month
    val currentMonthExpenses = remember(transactions) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startOfMonth = cal.timeInMillis
        transactions
            .filter { it.type.equals("EXPENSE", ignoreCase = true) && it.date >= startOfMonth }
            .sumOf { it.amount }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Clean Minimalism App Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TealPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = "محفظتي",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = onBgColor
                        )
                        Text(
                            text = SimpleDateFormat("EEEE, d MMMM", Locale("ar")).format(Date()),
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                }

                // Notification bell icon styled matching HTML class "bg-[#CCE8E8] rounded-full text-[#002020]"
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(dynamicMintLight)
                        .clickable { /* Handle click if desired */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "التنبيهات",
                        tint = TealPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 2. Summary Wallet Card containing totals & the glassmorphic side-by-side sub-panels
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(TealPrimary, TealPrimary.copy(alpha = 0.9f))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "الرصيد الإجمالي الفعلي (ILS)",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format(Locale.ENGLISH, "%,.2f", totalIls),
                                    color = Color.White,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-1).sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "₪",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Symmetric transparent balance cards (HTML class backdrop-blur-md)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Expenses Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "المصروفات",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format(Locale.ENGLISH, "%,.1f ₪", totalExpense),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Income Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "الدخل الكلي",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format(Locale.ENGLISH, "%,.1f ₪", totalIncome),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Grid of three premium action cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Add Quick button (Mint theme)
                Card(
                    onClick = onNavigateToTransactions,
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = dynamicMintBg),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(TealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "إضافة حركات",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                    }
                }

                // Debts Quick button (Red theme)
                Card(
                    onClick = onNavigateToDebts,
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = dynamicRedLightBg),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RedPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "دفتر الديون",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedPrimary
                        )
                    }
                }

                // Reports Quick button (Blue theme)
                Card(
                    onClick = onNavigateToReports,
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = dynamicBlueLightBg),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BluePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "التقارير",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                    }
                }
            }
        }

        // Budget & Warning Alerts
        item {
            val isDark = isSystemInDarkTheme()
            val hasLimit = limitAmount > 0.0
            val progress = if (hasLimit) (currentMonthExpenses / limitAmount).coerceIn(0.0, 1.0) else 0.0
            val isExceeded = hasLimit && currentMonthExpenses > limitAmount
            val cardBg = if (isExceeded) {
                if (isDark) Color(0xFF451C24) else Color(0xFFFFEBEE)
            } else {
                if (isDark) Color(0xFF1D292E) else Color(0xFFF2F9F9)
            }
            val borderCol = if (isExceeded) Color(0xFFE53935) else (if (isDark) Color(0xFF354449) else Color(0xFFD0EBEB))

            Card(
                onClick = {
                    budgetInputStr = if (hasLimit) limitAmount.toString() else ""
                    showBudgetDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.2.dp, borderCol)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (isExceeded) "🚨" else "📊", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = "حد الميزانية والإنذارات المبكرة",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isExceeded) Color(0xFFD32F2F) else TealPrimary
                                )
                                Text(
                                    text = "تتبع مصروفاتك وضبط الاستهلاك المالي",
                                    fontSize = 10.sp,
                                    color = TextGray
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isExceeded) Color(0xFFD32F2F).copy(alpha = 0.15f) else TealPrimary.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (hasLimit) "تعديل ⚙️" else "تحديد الحد+",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExceeded) Color(0xFFD32F2F) else TealPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!hasLimit) {
                        Text(
                            text = "اضغط لتحديد سقف مالي شهري للمصروفات، وسنقوم بتنبيهك تلقائياً إذا أوشكت على تجاوزه لتفادي الإنفاق المفرط.",
                            fontSize = 11.sp,
                            color = TextGray,
                            lineHeight = 16.sp
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text("المصروف هذا الشهر", fontSize = 10.sp, color = TextGray)
                                Text(
                                    text = String.format(Locale.ENGLISH, "%,.1f ₪", currentMonthExpenses),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isExceeded) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("السقف المالي المسموح", fontSize = 10.sp, color = TextGray)
                                Text(
                                    text = String.format(Locale.ENGLISH, "%,.0f ₪", limitAmount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TealPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Gauge bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isExceeded) Color(0xFFE53935).copy(alpha = 0.2f) else TealPrimary.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress.toFloat())
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isExceeded) Color(0xFFE53935) else TealPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format(Locale.ENGLISH, "المستهلك %d%%", (progress * 100).toInt()),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExceeded) Color(0xFFD32F2F) else TealPrimary
                            )
                            if (isExceeded) {
                                Text(
                                    text = "⚠️ تجاوزت حد الميزانية بـ ${String.format(Locale.ENGLISH, "%,.1f ₪", currentMonthExpenses - limitAmount)}!",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFD32F2F)
                                )
                            } else {
                                val remaining = limitAmount - currentMonthExpenses
                                Text(
                                    text = "متاح لك إنفاق ${String.format(Locale.ENGLISH, "%,.1f ₪", remaining)} بأمان",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Savings Goals Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎯", fontSize = 18.sp)
                    Text(
                        text = "صناديق وحصّالات الادخار",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = onBgColor
                    )
                }
                TextButton(onClick = { showAddGoalDialog = true }) {
                    Text("+ صندوق جديد", color = TealPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Saving Goals List (vertical cards)
        if (savingGoals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, dynamicBorder), RoundedCornerShape(20.dp))
                        .background(cardSurface)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🍯", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "حصّالات الادخار فارغة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        )
                        Text(
                            text = "أنشئ صندوقاً لتوفير المال لشراء سيارة، سفر أو أهداف شخصية",
                            fontSize = 10.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(savingGoals) { goal ->
                val goalProgress = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).coerceIn(0.0, 1.0) else 0.0
                val percent = (goalProgress * 100).toInt()
                val symbol = when (goal.currency) {
                    "ILS" -> "₪"
                    "USD" -> "$"
                    "JOD" -> "JD"
                    else -> goal.currency
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, dynamicBorder), RoundedCornerShape(22.dp))
                        .background(cardSurface)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(TealPrimary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎯", fontSize = 20.sp)
                                }
                                Column {
                                    Text(
                                        text = goal.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = onSurfaceColor
                                    )
                                    Text(
                                        text = "المستهدف: ${String.format(Locale.ENGLISH, "%,.0f %s", goal.targetAmount, symbol)}",
                                        fontSize = 11.sp,
                                        color = TextGray
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    onClick = {
                                        showDepositDialogForGoal = goal
                                        isDepositMode = true
                                        depositAmountStr = ""
                                    },
                                    modifier = Modifier
                                        .background(dynamicMintLight, RoundedCornerShape(10.dp))
                                        .height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("توفير +", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TealPrimary)
                                }

                                TextButton(
                                    onClick = {
                                        showDepositDialogForGoal = goal
                                        isDepositMode = false
                                        depositAmountStr = ""
                                    },
                                    modifier = Modifier
                                        .border(BorderStroke(1.dp, dynamicBorder), RoundedCornerShape(10.dp))
                                        .height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("سحب -", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextGray)
                                }

                                IconButton(
                                    onClick = { viewModel.deleteSavingGoal(goal) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = ExpenseRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مجموع الحصّالة: ${String.format(Locale.ENGLISH, "%,.1f %s", goal.savedAmount, symbol)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                            Text(
                                text = "$percent%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGray
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Progress Gauge bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(dynamicBorderDetail)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(goalProgress.toFloat())
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(TealPrimary)
                            )
                        }
                    }
                }
            }
        }

        // 4. Currency rates & wallets section
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الحسابات والعملات",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceColor
                )
                Text(
                    text = "أسعار صرف العملات المدمجة",
                    fontSize = 11.sp,
                    color = TextGray
                )
            }
        }

        // Horizontal Row of outline curved wallets
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val wallets = listOf(
                    Triple("الشيكل (ILS)", ils, "₪"),
                    Triple("الدولار (USD)", usd, "$"),
                    Triple("الدينار (JOD)", jod, "JD")
                )

                wallets.forEach { triple ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(BorderStroke(1.dp, dynamicBorder), RoundedCornerShape(20.dp))
                            .background(cardSurface)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.SpaceBetween) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(dynamicBorderDetail),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = triple.third,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextGray
                                    )
                                }
                                Text(
                                    text = triple.first.substringBefore(" "),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = String.format(Locale.ENGLISH, "%,.1f", triple.second),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = onSurfaceColor
                            )
                        }
                    }
                }
            }
        }

        // 5. Recent Transactions
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المعاملات الأخيرة",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceColor
                )
                TextButton(onClick = onNavigateToTransactions) {
                    Text(text = "عرض الكل", color = TealPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, dynamicBorder), RoundedCornerShape(20.dp))
                        .background(cardSurface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💸", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد حركات مالية مسجلة بعد",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        )
                        Text(
                            text = "بإمكانك إضافة مصروف أو دخل من زر الإضافة",
                            fontSize = 11.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(transactions.take(5)) { tx ->
                TransactionRowItem(tx = tx)
            }
        }
    }

    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("ضبط الميزانية الشهرية", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TealPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("أدخل سقف المصروفات الشهري المفضل بعملة الشيكل (₪) وسنرسل إنذارات حمراء وتنبيهات مستمرة في حال تجاوزها:", fontSize = 12.sp, color = TextGray)
                    OutlinedTextField(
                        value = budgetInputStr,
                        onValueChange = { budgetInputStr = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("مثال: 3000") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limitVal = budgetInputStr.toDoubleOrNull() ?: 0.0
                        viewModel.updateBudgetLimit(limitVal, "ILS")
                        showBudgetDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("حفظ وتفعيل 💾", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showAddGoalDialog) {
        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("إنشاء صندوق ادخار جديد", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TealPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = goalNameInput,
                        onValueChange = { goalNameInput = it },
                        label = { Text("اسم الهدف الادخاري (مثال: شراء لابتوب جديد 💻)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = goalTargetInput,
                        onValueChange = { goalTargetInput = it },
                        label = { Text("المبلغ المستهدف (أرقام)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Currency Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ILS", "USD", "JOD").forEach { cur ->
                            val selected = goalCurrencyInput == cur
                            Card(
                                modifier = Modifier.weight(1f).clickable { goalCurrencyInput = cur },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = when (cur) {
                                        "ILS" -> "شيكل"
                                        "USD" -> "دولار"
                                        "JOD" -> "دينار"
                                        else -> cur
                                    },
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetVal = goalTargetInput.toDoubleOrNull() ?: 0.0
                        if (goalNameInput.isNotEmpty() && targetVal > 0.0) {
                            viewModel.addSavingGoal(goalNameInput, targetVal, goalCurrencyInput)
                            goalNameInput = ""
                            goalTargetInput = ""
                            showAddGoalDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إنشاء الحصّالة 🎉", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showDepositDialogForGoal != null) {
        val activeGoal = showDepositDialogForGoal!!
        val symbol = when (activeGoal.currency) {
            "ILS" -> "₪"
            "USD" -> "$"
            "JOD" -> "JD"
            else -> activeGoal.currency
        }
        AlertDialog(
            onDismissRequest = { showDepositDialogForGoal = null },
            title = {
                Text(
                    text = if (isDepositMode) "توفير مالي في: ${activeGoal.name}" else "سحب مالي من: ${activeGoal.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TealPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isDepositMode) 
                            "المبلغ المراد توفيره ونقله من الحساب الفعلي إلى حصّالة التوفير:" 
                            else "المبلغ المراد سحبه من حصّالة التوفير وإرجاعه لحسابك الفعلي:",
                        fontSize = 12.sp, color = TextGray
                    )
                    OutlinedTextField(
                        value = depositAmountStr,
                        onValueChange = { depositAmountStr = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("المبلغ بـ $symbol") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = depositAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            if (isDepositMode) {
                                // 1. Log an Expense Transaction to deduct from main account balance
                                viewModel.addTransaction(
                                    title = "[ادخار 🎯] ${activeGoal.name}",
                                    amount = amt,
                                    currency = activeGoal.currency,
                                    type = "EXPENSE",
                                    categoryId = 8, // Category 'فواتير والتزامات'
                                    subCategoryId = null,
                                    incomeSourceId = null,
                                    date = System.currentTimeMillis()
                                )
                                // 2. Increase saved Amount in goals
                                viewModel.updateSavingGoal(activeGoal.copy(savedAmount = activeGoal.savedAmount + amt))
                                showDepositDialogForGoal = null
                            } else {
                                if (amt <= activeGoal.savedAmount) {
                                    // 1. Log an Income Transaction to replenish main account balance
                                    viewModel.addTransaction(
                                        title = "[سحب من حصالة 🎯] ${activeGoal.name}",
                                        amount = amt,
                                        currency = activeGoal.currency,
                                        type = "INCOME",
                                        categoryId = 8,
                                        subCategoryId = null,
                                        incomeSourceId = null,
                                        date = System.currentTimeMillis()
                                    )
                                    // 2. Decrease saved Amount in goals
                                    viewModel.updateSavingGoal(activeGoal.copy(savedAmount = activeGoal.savedAmount - amt))
                                    showDepositDialogForGoal = null
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تأكيد العملية ✅", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDepositDialogForGoal = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun TransactionRowItem(tx: TransactionEntity) {
    val df = SimpleDateFormat("yyyy/MM/dd • hh:mm a", Locale("ar"))
    val isIncome = tx.type.equals("INCOME", ignoreCase = true)
    val isDark = isSystemInDarkTheme()

    val dynamicBorder = if (isDark) Color(0xFF353C3C) else BorderLight
    val dynamicMintBg = if (isDark) Color(0xFF153333) else MintBgSoft
    val dynamicRedLightBg = if (isDark) Color(0xFF451919) else RedLight
    val textColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .border(BorderStroke(1.dp, dynamicBorder), RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isIncome) dynamicMintBg
                            else dynamicRedLightBg
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (isIncome) TealPrimary else ExpenseRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = tx.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = df.format(Date(tx.date)),
                        fontSize = 10.sp,
                        color = TextGray
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val symbol = when (tx.currency) {
                    "ILS" -> "₪"
                    "USD" -> "$"
                    "JOD" -> "JD"
                    else -> tx.currency
                }
                Text(
                    text = String.format(Locale.ENGLISH, "%s%,.1f %s", if (isIncome) "+" else "-", tx.amount, symbol),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = if (isIncome) TealPrimary else ExpenseRed
                )
                Text(
                    text = if (isIncome) "دخل" else "مصروف",
                    fontSize = 9.sp,
                    color = TextGray
                )
            }
        }
    }
}
