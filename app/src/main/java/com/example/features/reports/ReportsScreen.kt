package com.example.features.reports

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.Category
import com.example.core.database.TransactionEntity
import com.example.features.wallet.TransactionRowItem
import com.example.features.wallet.WalletViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Helper functions for the Transaction Calendar
fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
           cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
}

fun getTransactionsForDay(txs: List<TransactionEntity>, dateInMillis: Long): List<TransactionEntity> {
    val targetCal = Calendar.getInstance().apply { timeInMillis = dateInMillis }
    return txs.filter { tx ->
        val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
        isSameDay(targetCal, txCal)
    }
}

fun getMonthNameArabic(month: Int): String {
    return when (month) {
        Calendar.JANUARY -> "يناير"
        Calendar.FEBRUARY -> "فبراير"
        Calendar.MARCH -> "مارس"
        Calendar.APRIL -> "أبريل"
        Calendar.MAY -> "مايو"
        Calendar.JUNE -> "يونيو"
        Calendar.JULY -> "يوليو"
        Calendar.AUGUST -> "أغسطس"
        Calendar.SEPTEMBER -> "سبتمبر"
        Calendar.OCTOBER -> "أكتوبر"
        Calendar.NOVEMBER -> "نوفمبر"
        Calendar.DECEMBER -> "ديسمبر"
        else -> ""
    }
}

fun getDayOfWeekArabic(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        Calendar.SUNDAY -> "الأحد"
        Calendar.MONDAY -> "الاثنين"
        Calendar.TUESDAY -> "الثلاثاء"
        Calendar.WEDNESDAY -> "الأربعاء"
        Calendar.THURSDAY -> "الخميس"
        Calendar.FRIDAY -> "الجمعة"
        Calendar.SATURDAY -> "السبت"
        else -> ""
    }
}

fun getFormattedDateArabic(calendar: Calendar): String {
    val dayName = getDayOfWeekArabic(calendar.get(Calendar.DAY_OF_WEEK))
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = calendar.get(Calendar.MONTH) + 1
    val year = calendar.get(Calendar.YEAR)
    return "$dayName %02d/%02d/%d".format(Locale.ENGLISH, day, month, year)
}

data class CalendarDay(
    val dayOfMonth: Int,
    val dateInMillis: Long,
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

@Composable
fun ReportsScreen(viewModel: WalletViewModel) {
    val txs by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var selectedIntervalDays by remember { mutableStateOf(30) } // 7, 30, 90, 365
    val context = LocalContext.current

    // Navigation Tab state: 0 = Calendar, 1 = Charts/Stats
    var selectedReportTab by remember { mutableStateOf(0) }
    
    // Day Selection State for Calendar View
    var selectedCalendarDay by remember { mutableStateOf(Calendar.getInstance()) }

    // OPTIMIZATIONS FOR SMOOTH RENDERING AND PREVENTING CRASHES/ANRs:
    val selectedDayTxs = remember(txs, selectedCalendarDay) {
        getTransactionsForDay(txs, selectedCalendarDay.timeInMillis)
    }
    val selectedDayIncome = remember(selectedDayTxs) {
        selectedDayTxs.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
    }
    val selectedDayExpense = remember(selectedDayTxs) {
        selectedDayTxs.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }
    }

    val highestInDayAndExDay = remember(txs) {
        val calendarInstance = Calendar.getInstance()
        val currentYear = calendarInstance.get(Calendar.YEAR)
        val currentMonth = calendarInstance.get(Calendar.MONTH)
        
        val txCal = Calendar.getInstance()
        val currentMonthTxs = txs.filter { tx ->
            txCal.timeInMillis = tx.date
            txCal.get(Calendar.YEAR) == currentYear &&
            txCal.get(Calendar.MONTH) == currentMonth
        }

        val dayGroupedTxs = currentMonthTxs.groupBy { tx ->
            val c = Calendar.getInstance().apply { timeInMillis = tx.date }
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis
        }

        val inDay = dayGroupedTxs.mapValues { entry ->
            entry.value.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
        }.filter { it.value > 0.0 }.maxByOrNull { it.value }

        val exDay = dayGroupedTxs.mapValues { entry ->
            entry.value.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }
        }.filter { it.value > 0.0 }.maxByOrNull { it.value }

        Pair(inDay, exDay)
    }
    val highestInDay = highestInDayAndExDay.first
    val highestExDay = highestInDayAndExDay.second

    val filteredTransactions = remember(txs, selectedIntervalDays) {
        val boundaryTime = System.currentTimeMillis() - (selectedIntervalDays * 24L * 60L * 60L * 1000L)
        txs.filter { it.date >= boundaryTime }
    }

    val totalIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
    }

    val totalExpense = remember(filteredTransactions) {
        filteredTransactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }
    }

    val netSavings = totalIncome - totalExpense

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Appbar header row
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تقارير وتحليلات الميزانية",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary
                )
            }
        }

        // Tab Selector for Calendar vs. Charts
        item {
            val isDark = isSystemInDarkTheme()
            val surfaceColor = MaterialTheme.colorScheme.surface
            val activeTabBg = if (isDark) Color(0xFF153333) else MintBgSoft
            val defaultBorder = if (isDark) Color(0xFF353C3C) else BorderLight

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF191C1C) else GrayLight)
                    .padding(4.dp)
            ) {
                listOf("تقويم المعاملات 📅", "الرسوم والإحصائيات 📊").forEachIndexed { index, title ->
                    val isSelected = selectedReportTab == index
                    val tabBg = if (isSelected) activeTabBg else Color.Transparent
                    val textColor = if (isSelected) TealPrimary else TextGray

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tabBg)
                            .clickable { selectedReportTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = textColor
                        )
                    }
                }
            }
        }

        if (selectedReportTab == 0) {
            // ================== TAB 0: TRANSACTION CALENDAR ==================
            item {
                MonthlyTransactionCalendar(
                    transactions = txs,
                    selectedDay = selectedCalendarDay,
                    onDaySelected = { day ->
                        selectedCalendarDay = day
                    }
                )
            }

            item {
                val isDark = isSystemInDarkTheme()
                val cardBg = if (isDark) SlateDark else SurfaceWhite
                val borderColor = if (isDark) Color(0xFF353C3C) else BorderLight

                // Calculate highest income and expense days of the current visible month
                val calendarInstance = Calendar.getInstance()
                val currentMonthTxs = txs.filter { tx ->
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                    txCal.get(Calendar.YEAR) == calendarInstance.get(Calendar.YEAR) &&
                    txCal.get(Calendar.MONTH) == calendarInstance.get(Calendar.MONTH)
                }

                val dayGroupedTxs = currentMonthTxs.groupBy {
                    val c = Calendar.getInstance().apply { timeInMillis = it.date }
                    c.set(Calendar.HOUR_OF_DAY, 0)
                    c.set(Calendar.MINUTE, 0)
                    c.set(Calendar.SECOND, 0)
                    c.set(Calendar.MILLISECOND, 0)
                    c.timeInMillis
                }

                val highestInDay = dayGroupedTxs.mapValues { entry ->
                    entry.value.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
                }.filter { it.value > 0.0 }.maxByOrNull { it.value }

                val highestExDay = dayGroupedTxs.mapValues { entry ->
                    entry.value.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }
                }.filter { it.value > 0.0 }.maxByOrNull { it.value }

                if (highestInDay != null || highestExDay != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "أبرز الصرف والوارد للشهر الحالي 📈",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (highestInDay != null) {
                                    val inDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(highestInDay.key))
                                    Card(
                                        modifier = Modifier.weight(1f).clickable {
                                            selectedCalendarDay = Calendar.getInstance().apply { timeInMillis = highestInDay.key }
                                        },
                                        colors = CardDefaults.cardColors(containerColor = (if (isDark) Color(0xFF0F261E) else Color(0xFFE8F8F5))),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("أبرز يوم دخل 💸", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00A86B))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(String.format(Locale.ENGLISH, "%.1f ₪", highestInDay.value), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00A86B))
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(inDate, fontSize = 9.sp, color = TextGray)
                                        }
                                    }
                                }

                                if (highestExDay != null) {
                                    val exDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(highestExDay.key))
                                    Card(
                                        modifier = Modifier.weight(1f).clickable {
                                            selectedCalendarDay = Calendar.getInstance().apply { timeInMillis = highestExDay.key }
                                        },
                                        colors = CardDefaults.cardColors(containerColor = (if (isDark) Color(0xFF2B1414) else Color(0xFFFDECEB))),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("أبرز يوم صرف 📤", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(String.format(Locale.ENGLISH, "%.1f ₪", highestExDay.value), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE53935))
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(exDate, fontSize = 9.sp, color = TextGray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Daily Summary details display (like Image 2 bottom pane)
            item {
                val isDark = isSystemInDarkTheme()
                val cellColor = if (isDark) SlateDark else SurfaceWhite
                val borderColor = if (isDark) Color(0xFF353C3C) else BorderLight

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = cellColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تفاصيل يوم ${getFormattedDateArabic(selectedCalendarDay)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "معاملات",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }

                        Divider(color = borderColor.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Income summary line
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xFF00A86B))
                                )
                                Text("إيرادات", fontSize = 12.sp, color = TextGray, fontWeight = FontWeight.Medium)
                                Text(
                                    text = String.format(Locale.ENGLISH, "%.1f ₪", selectedDayIncome),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00A86B)
                                )
                            }

                            // Expense summary line
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xFFE53935))
                                )
                                Text("مصاريف", fontSize = 12.sp, color = TextGray, fontWeight = FontWeight.Medium)
                                Text(
                                    text = String.format(Locale.ENGLISH, "%.1f ₪", selectedDayExpense),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE53935)
                                )
                            }
                        }
                    }
                }
            }

            // List of actually registered movements for the selected day
            if (selectedDayTxs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد حركات مالية مسجلة في هذا اليوم.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                items(selectedDayTxs) { tx ->
                    TransactionRowItem(tx = tx)
                }
            }

        } else {
            // ================== TAB 1: ORIGINAL GRAPHS & REPORT ==================
            // Interval selector
            item {
                val isDark = isSystemInDarkTheme()
                val surfaceColor = MaterialTheme.colorScheme.surface
                val dynamicBorder = if (isDark) Color(0xFF353C3C) else BorderLight

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        7 to "٧ أيام",
                        30 to "٣٠ يوم",
                        90 to "٩٠ يوم",
                        365 to "عام كامل"
                    ).forEach { pair ->
                        val isSelected = selectedIntervalDays == pair.first
                        val optionBg = if (isSelected) (if (isDark) Color(0xFF153333) else MintBgSoft) else surfaceColor
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(optionBg)
                                .border(
                                    BorderStroke(1.dp, if (isSelected) TealPrimary else dynamicBorder),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedIntervalDays = pair.first }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pair.second,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isSelected) TealPrimary else TextGray
                            )
                        }
                    }
                }
            }

            // Stats boxes cards
            item {
                val isDark = isSystemInDarkTheme()
                val surfaceColor = MaterialTheme.colorScheme.surface
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                val dynamicBorder = if (isDark) Color(0xFF353C3C) else BorderLight

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, dynamicBorder), RoundedCornerShape(20.dp))
                        .background(surfaceColor)
                        .padding(16.dp)
                ) {
                    Column {
                        Text("إحصائيات المدة المحددة", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = onSurfaceColor)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("مدخولات واردة 📥", fontSize = 10.sp, color = TextGray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(String.format(Locale.ENGLISH, "₪ %,.1f", totalIncome), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("مصروفات خارجة 📤", fontSize = 10.sp, color = TextGray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(String.format(Locale.ENGLISH, "₪ %,.1f", totalExpense), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = dynamicBorder.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("صافي المتبقي والادخار", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = onSurfaceColor)
                            Text(
                                text = String.format(Locale.ENGLISH, "₪ %,.1f", netSavings),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (netSavings >= 0) TealPrimary else ExpenseRed
                            )
                        }
                    }
                }
            }

            // Geometric custom donut category charts
            item {
                val isDark = isSystemInDarkTheme()
                val surfaceColor = MaterialTheme.colorScheme.surface
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                val dynamicBorder = if (isDark) Color(0xFF353C3C) else BorderLight

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, dynamicBorder), RoundedCornerShape(20.dp))
                        .background(surfaceColor)
                        .padding(16.dp)
                ) {
                    Column {
                        Text("توزيع المصروفات حسب الفئات", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = onSurfaceColor)
                        Spacer(modifier = Modifier.height(16.dp))

                        val expenseTransactions = filteredTransactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }
                        val expensesByCategory = remember(expenseTransactions, categories) {
                            categories.map { cat ->
                                cat to expenseTransactions.filter { it.categoryId == cat.id }.sumOf { it.amount }
                            }.filter { it.second > 0.0 }
                        }

                        if (expensesByCategory.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("لا توجد مصاريف مضافة للرسم البياني", color = TextGray, fontSize = 11.sp)
                            }
                        } else {
                            val colorList = listOf(
                                TealPrimary,
                                BluePrimary,
                                Color(0xFFD48237),
                                ExpenseRed,
                                Color(0xFF8E90B2),
                                Color(0xFF9C27B0),
                                Color(0xFF138D75)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Graph
                                Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val totalToDraw = expensesByCategory.sumOf { it.second }.toFloat()
                                        var currentAngle = 0f

                                        expensesByCategory.forEachIndexed { index, pair ->
                                            val sweep = (pair.second.toFloat() / totalToDraw) * 360f
                                            drawArc(
                                                color = colorList[index % colorList.size],
                                                startAngle = currentAngle,
                                                sweepAngle = sweep,
                                                useCenter = false,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 24f)
                                            )
                                            currentAngle += sweep
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("المصروف", fontSize = 9.sp, color = TextGray)
                                        Text(String.format(Locale.ENGLISH, "%,.0f ₪", totalExpense), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                                    }
                                }

                                // Legend
                                Column(
                                    modifier = Modifier.padding(start = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    expensesByCategory.take(5).forEachIndexed { index, pair ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(colorList[index % colorList.size])
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${pair.first.name} (${String.format(Locale.ENGLISH, "%.1f", pair.second)} ₪)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = onSurfaceColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "حركات التقرير المالي الحالي",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد حركات مالية في المدة المحددة", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                items(filteredTransactions) { tx ->
                    TransactionRowItem(tx = tx)
                }
            }
        }
    }
}

@Composable
fun MonthlyTransactionCalendar(
    transactions: List<TransactionEntity>,
    selectedDay: Calendar,
    onDaySelected: (Calendar) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().apply { timeInMillis = selectedDay.timeInMillis }) }

    val calendarDays = remember(currentMonth) {
        val list = mutableListOf<CalendarDay>()
        val tempCal = Calendar.getInstance().apply {
            timeInMillis = currentMonth.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Saturday = 7
        val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Previous month filler days
        val prevCal = (tempCal.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
        val prevDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val prefixCount = firstDayOfWeek - 1 // distance from Sunday
        for (i in (prevDays - prefixCount + 1)..prevDays) {
            val buildCal = Calendar.getInstance().apply {
                timeInMillis = prevCal.timeInMillis
                set(Calendar.DAY_OF_MONTH, i)
            }
            list.add(
                CalendarDay(
                    dayOfMonth = i,
                    dateInMillis = buildCal.timeInMillis,
                    isCurrentMonth = false,
                    isToday = isSameDay(buildCal, Calendar.getInstance())
                )
            )
        }

        // Current month days
        for (i in 1..daysInMonth) {
            val buildCal = Calendar.getInstance().apply {
                timeInMillis = tempCal.timeInMillis
                set(Calendar.DAY_OF_MONTH, i)
            }
            list.add(
                CalendarDay(
                    dayOfMonth = i,
                    dateInMillis = buildCal.timeInMillis,
                    isCurrentMonth = true,
                    isToday = isSameDay(buildCal, Calendar.getInstance())
                )
            )
        }

        // Next month filler days (fill up to grid of 42 cells)
        val remaining = 42 - list.size
        val nextCal = (tempCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        for (i in 1..remaining) {
            val buildCal = Calendar.getInstance().apply {
                timeInMillis = nextCal.timeInMillis
                set(Calendar.DAY_OF_MONTH, i)
            }
            list.add(
                CalendarDay(
                    dayOfMonth = i,
                    dateInMillis = buildCal.timeInMillis,
                    isCurrentMonth = false,
                    isToday = isSameDay(buildCal, Calendar.getInstance())
                )
            )
        }
        list
    }

    // HIGHLY OPTIMIZED LOOKUPS:
    val dayFormatter = remember { SimpleDateFormat("yyyyMMdd", Locale.US) }
    val groupedTransactions = remember(transactions) {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        transactions.groupBy { tx ->
            sdf.format(Date(tx.date))
        }
    }
    val selectedDayKey = remember(selectedDay) {
        dayFormatter.format(selectedDay.time)
    }

    val isDark = isSystemInDarkTheme()
    val calendarCardBg = if (isDark) SlateDark else SurfaceWhite
    val borderColor = if (isDark) Color(0xFF353C3C) else BorderLight

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = calendarCardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Month Switcher Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val prev = (currentMonth.clone() as Calendar).apply {
                            add(Calendar.MONTH, -1)
                        }
                        currentMonth = prev
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "الشهر السابق",
                        tint = TealPrimary
                    )
                }

                Text(
                    text = "${getMonthNameArabic(currentMonth.get(Calendar.MONTH))} ${currentMonth.get(Calendar.YEAR)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = {
                        val next = (currentMonth.clone() as Calendar).apply {
                            add(Calendar.MONTH, 1)
                        }
                        currentMonth = next
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "الشهر التالي",
                        tint = TealPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Days of the Week Header
            val dayHeaders = listOf("أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayHeaders.forEach { header ->
                    Text(
                        text = header,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Divider(color = borderColor.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // Calendar Day Grid
            val rows = calendarDays.chunked(7)
            rows.forEach { weekDays ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekDays.forEach { day ->
                        val dayKey = dayFormatter.format(Date(day.dateInMillis))
                        val isSelected = dayKey == selectedDayKey

                        // Calculate stats for this day using direct map lookup
                        val dayTxs = groupedTransactions[dayKey] ?: emptyList()
                        val dayIncome = dayTxs.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
                        val dayExpense = dayTxs.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }

                        // Style for date cells
                        val cellBg = when {
                            isSelected -> if (isDark) TealPrimary.copy(alpha = 0.35f) else MintBgSoft
                            day.isToday -> if (isDark) Color(0xFF1E2F2F) else GrayLight
                            else -> Color.Transparent
                        }
                        val cellBorder = if (isSelected) {
                            BorderStroke(1.5.dp, TealPrimary)
                        } else if (day.isToday) {
                            BorderStroke(1.dp, TealPrimary.copy(alpha = 0.4f))
                        } else {
                            null
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.72f) // slightly taller for info texts as in Image 2
                                .padding(1.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cellBg)
                                .then(if (cellBorder != null) Modifier.border(cellBorder, RoundedCornerShape(8.dp)) else Modifier)
                                .clickable {
                                    val dayCal = Calendar.getInstance().apply { timeInMillis = day.dateInMillis }
                                    onDaySelected(dayCal)
                                }
                                .padding(vertical = 4.dp, horizontal = 1.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Indicators (Dots) row at the top of the box
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.height(6.dp)
                                ) {
                                    if (dayIncome > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(0xFF00A86B))
                                        )
                                    }
                                    if (dayExpense > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(0xFFE53935))
                                        )
                                    }
                                }

                                // Day number text
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    fontWeight = if (day.isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = when {
                                        isSelected -> TealPrimary
                                        day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                                        else -> TextGray.copy(alpha = 0.4f)
                                    }
                                )

                                // Income / Expense values at bottom
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(1.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (dayIncome > 0 && day.isCurrentMonth) {
                                        Text(
                                            text = "+${dayIncome.toInt()}",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF00A86B),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    if (dayExpense > 0 && day.isCurrentMonth) {
                                        Text(
                                            text = "-${dayExpense.toInt()}",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFE53935),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
