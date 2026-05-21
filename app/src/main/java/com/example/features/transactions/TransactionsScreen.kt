package com.example.features.transactions

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.widget.Toast
import com.example.ui.theme.*
import com.example.core.database.*
import com.example.features.wallet.TransactionRowItem
import com.example.features.wallet.WalletViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(viewModel: WalletViewModel) {
    val categories by viewModel.categories.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()
    val incomeSources by viewModel.incomeSources.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showCategoryCrudDialog by remember { mutableStateOf(false) }
    var selectedSourceProfile by remember { mutableStateOf<IncomeSource?>(null) }

    // Navigation sub-tabs
    var activeSubTab by remember { mutableStateOf(0) } // 0: الحركات المالية, 1: مصادر الدخل

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Upper Switch Tab
        TabRow(selectedTabIndex = activeSubTab, modifier = Modifier.clip(RoundedCornerShape(8.dp))) {
            Tab(selected = activeSubTab == 0, onClick = { activeSubTab = 0 }) {
                Text("المعاملات المالية", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Tab(selected = activeSubTab == 1, onClick = { activeSubTab = 1 }) {
                Text("مصادر الدخل", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeSubTab == 0) {
            // Transaction list and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة حركة مالية")
                }

                FilledTonalButton(
                    onClick = { showCategoryCrudDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Category, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إدارة الفئات")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💸", fontSize = 48.sp)
                        Text("لا توجد معاملات بعد", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("قم بإضافة حركة مالية جديدة للبدء بالتتبع", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(transactions) { tx ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            TransactionRowWithDelete(tx = tx, onDelete = { viewModel.deleteTransaction(tx) })
                        }
                    }
                }
            }
        } else {
            // Income Sources profiles
            IncomeSourcesProfileSection(
                incomeSources = incomeSources,
                transactions = transactions,
                onAddSource = { name, icon -> viewModel.addIncomeSource(name, icon) },
                onDeleteSource = { source -> viewModel.deleteIncomeSource(source) },
                selectedSource = selectedSourceProfile,
                onSelectSource = { selectedSourceProfile = it }
            )
        }
    }

    val walletBalances by viewModel.walletBalances.collectAsState()
    val context = LocalContext.current

    // Modal Create Transaction Dialog
    if (showAddDialog) {
        AddTransactionModal(
            categories = categories,
            subCategories = subCategories,
            incomeSources = incomeSources,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, amount, currency, type, catId, subCatId, incSourceId, date ->
                val currentBal = walletBalances[currency] ?: 0.0
                if (type == "EXPENSE" && currentBal < amount) {
                    Toast.makeText(context, "⚠️ عذراً! الرصيد غير كافٍ في المحفظة لإجراء هذا المصروف. (الرصيد المتاح: $currentBal $currency)", Toast.LENGTH_LONG).show()
                } else {
                    viewModel.addTransaction(title, amount, currency, type, catId, subCatId, incSourceId, date)
                    showAddDialog = false
                }
            }
        )
    }

    // Modal Category Management Dialog
    if (showCategoryCrudDialog) {
        CategoryCrudModal(
            categories = categories,
            subCategories = subCategories,
            allTransactions = transactions,
            onDismiss = { showCategoryCrudDialog = false },
            onAddCategory = { name, icon -> viewModel.addCategory(name, icon) },
            onUpdateCategory = { category -> viewModel.updateCategory(category) },
            onDeleteCategory = { category -> viewModel.deleteCategory(category) },
            onAddSubCategory = { categoryId, name -> viewModel.addSubCategory(categoryId, name) },
            onUpdateSubCategory = { sub -> viewModel.updateSubCategory(sub) },
            onDeleteSubCategory = { sub -> viewModel.deleteSubCategory(sub) }
        )
    }
}

@Composable
fun TransactionRowWithDelete(tx: TransactionEntity, onDelete: () -> Unit) {
    var expandedMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable {
                expandedMenu = true
            },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box {
                TransactionRowItem(tx = tx)
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(12.dp).clickable { expandedMenu = true },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
            DropdownMenuItem(
                text = { Text("حذف الحركة المالية", color = Color.Red) },
                onClick = {
                    onDelete()
                    expandedMenu = false
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IncomeSourcesProfileSection(
    incomeSources: List<IncomeSource>,
    transactions: List<TransactionEntity>,
    onAddSource: (String, String?) -> Unit,
    onDeleteSource: (IncomeSource) -> Unit,
    selectedSource: IncomeSource?,
    onSelectSource: (IncomeSource?) -> Unit
) {
    var showAddSourceDialog by remember { mutableStateOf(false) }
    var sourceName by remember { mutableStateOf("") }
    var sourceIcon by remember { mutableStateOf("💼") }

    if (selectedSource == null) {
        // List sources
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ملفات الإيرادات الاستثمارية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = { showAddSourceDialog = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (incomeSources.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("لا توجد ملامح دخل معرفة، اضغط لإضافة الراتب أو عمل مستقل", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                FlowRow(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    incomeSources.forEach { src ->
                        val matchingTx = transactions.filter { it.incomeSourceId == src.id && it.type.equals("INCOME", ignoreCase = true) }
                        val totalIncomeForSource = matchingTx.sumOf { it.amount }

                        Card(
                            modifier = Modifier
                                .width(160.dp)
                                .height(160.dp)
                                .clickable { onSelectSource(src) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(src.icon ?: "💰", fontSize = 24.sp)
                                    IconButton(
                                        onClick = { onDeleteSource(src) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(src.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format(Locale.ENGLISH, "₪ %,.1f", totalIncomeForSource),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text("إجمالي الإيرادات", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Source profile details with graph of growth
        val matchingTx = transactions.filter { it.incomeSourceId == selectedSource.id && it.type.equals("INCOME", ignoreCase = true) }.sortedBy { it.date }
        val totalIncomeForSource = matchingTx.sumOf { it.amount }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onSelectSource(null) }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                }
                Text(selectedSource.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(selectedSource.icon ?: "💼", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stat card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("إجمالي التدفق الوارد من هذا المصدر", fontSize = 12.sp)
                    Text(
                        text = String.format(Locale.ENGLISH, "₪ %,.2f", totalIncomeForSource),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("عدد الدفعات الواردة: ${matchingTx.size}", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("رسم بياني لنمو الإيرادات", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            // Custom canvas growth line graph!
            Card(
                modifier = Modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                if (matchingTx.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد بيانات لتمثيل الرسم البياني", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 6f
                            val numPoints = matchingTx.size
                            val maxVal = matchingTx.maxOfOrNull { it.amount }?.toFloat() ?: 1f
                            val width = size.width
                            val height = size.height

                            val path = androidx.compose.ui.graphics.Path()
                            var cumulativeSum = 0.0

                            val cumulativePoints = matchingTx.map {
                                cumulativeSum += it.amount
                                cumulativeSum
                            }
                            val maxCumulative = cumulativePoints.lastOrNull() ?: 1.0

                            cumulativePoints.forEachIndexed { idx, value ->
                                val x = if (numPoints > 1) (idx.toFloat() / (numPoints - 1)) * width else width / 2
                                val y = height - ((value.toFloat() / maxCumulative.toFloat()) * height)

                                if (idx == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }

                                drawCircle(
                                    color = Color(0xFF00A86B),
                                    radius = 10f,
                                    center = androidx.compose.ui.geometry.Offset(x, y)
                                )
                            }

                            drawPath(
                                path = path,
                                color = Color(0xFF00A86B),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("سجل الدفعات المستلمة", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            matchingTx.reversed().forEach { tx ->
                TransactionRowItem(tx = tx)
            }
        }
    }

    // Add Source dialog
    if (showAddSourceDialog) {
        AlertDialog(
            onDismissRequest = { showAddSourceDialog = false },
            title = { Text("إضافة مصدر دخل جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = sourceName,
                        onValueChange = { sourceName = it },
                        label = { Text("اسم المصدر (الراتب، الاستثمارات...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sourceIcon,
                        onValueChange = { sourceIcon = it },
                        label = { Text("أيقونة تعبيرية (Emoji)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (sourceName.isNotEmpty()) {
                        onAddSource(sourceName, sourceIcon)
                        sourceName = ""
                        showAddSourceDialog = false
                    }
                }) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSourceDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionModal(
    categories: List<Category>,
    subCategories: List<SubCategory>,
    incomeSources: List<IncomeSource>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, currency: String, type: String, catId: Int, subCatId: Int?, incSourceId: Int?, date: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("ILS") }
    var type by remember { mutableStateOf("EXPENSE") } // EXPENSE, INCOME

    var categoryId by remember { mutableStateOf(0) }
    var subCategoryId by remember { mutableStateOf<Int?>(null) }
    var incomeSourceId by remember { mutableStateOf<Int?>(null) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val filteredSubCategories = remember(categoryId, subCategories) {
        subCategories.filter { it.categoryId == categoryId }
    }

    var catMenuExpanded by remember { mutableStateOf(false) }
    var subMenuExpanded by remember { mutableStateOf(false) }
    var sourceMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة حركة مالية جديدة", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Type choice (Tab/Toggler)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { type = "EXPENSE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "EXPENSE") Color(0xFFE53935) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (type == "EXPENSE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("مصروف")
                    }
                    Button(
                        onClick = { type = "INCOME" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "INCOME") Color(0xFF00A86B) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (type == "INCOME") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("وارد")
                    }
                }

                // 2. Classification Section (Placed above بيان الحركة)
                if (type == "EXPENSE") {
                    Text("تصنيف المصروف:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                    // Category Selection dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val activeCategory = categories.find { it.id == categoryId }
                        OutlinedButton(
                            onClick = { catMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(activeCategory?.let { "${it.icon} ${it.name}" } ?: "اختر الفئة الرئيسية للمصروف")
                        }
                        DropdownMenu(expanded = catMenuExpanded, onDismissRequest = { catMenuExpanded = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${cat.icon ?: "📂"} ${cat.name}") },
                                    onClick = {
                                        categoryId = cat.id
                                        subCategoryId = null
                                        catMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Sub-category Selection dropdown
                    if (categoryId != 0) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val activeSub = subCategories.find { it.id == subCategoryId }
                            OutlinedButton(
                                onClick = { subMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(activeSub?.name ?: "اختر الفئة الفرعية لمزيد من الدقة (اختياري)")
                            }
                            DropdownMenu(expanded = subMenuExpanded, onDismissRequest = { subMenuExpanded = false }) {
                                filteredSubCategories.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub.name) },
                                        onClick = {
                                            subCategoryId = sub.id
                                            subMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text("تصنيف ومصدر الوارد:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                    // INCOME: Source Profile premium grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val chunkedSources = incomeSources.chunked(2)
                        chunkedSources.forEach { rowSources ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowSources.forEach { src ->
                                    val isSelected = incomeSourceId == src.id
                                    val bg = if (isSelected) TealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                                    val borderCol = if (isSelected) TealPrimary else MaterialTheme.colorScheme.outlineVariant
                                    val contentColor = if (isSelected) TealPrimary else MaterialTheme.colorScheme.onSurface
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(bg)
                                            .border(1.5.dp, borderCol, RoundedCornerShape(14.dp))
                                            .clickable { 
                                                incomeSourceId = src.id
                                                // Pre-fill corresponding title if currently empty or a placeholder
                                                if (title.isEmpty() || incomeSources.any { it.name == title }) {
                                                    title = src.name
                                                }
                                            }
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(src.icon ?: "💸", fontSize = 18.sp)
                                            Text(
                                                text = src.name,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = contentColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (rowSources.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Category Selection dropdown for INCOME
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val activeCategory = categories.find { it.id == categoryId }
                        OutlinedButton(
                            onClick = { catMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(activeCategory?.let { "${it.icon} ${it.name}" } ?: "اختر الفئة الرئيسية (التصنيف الكبير)")
                        }
                        DropdownMenu(expanded = catMenuExpanded, onDismissRequest = { catMenuExpanded = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${cat.icon ?: "📂"} ${cat.name}") },
                                    onClick = {
                                        categoryId = cat.id
                                        subCategoryId = null
                                        catMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Sub-category Selection dropdown for INCOME
                    if (categoryId != 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val activeSub = subCategories.find { it.id == subCategoryId }
                            OutlinedButton(
                                onClick = { subMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(activeSub?.name ?: "اختر الفئة الفرعية (اختياري)")
                            }
                            DropdownMenu(expanded = subMenuExpanded, onDismissRequest = { subMenuExpanded = false }) {
                                filteredSubCategories.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub.name) },
                                        onClick = {
                                            subCategoryId = sub.id
                                            subMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 3. بيان الحركة Input Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("بيان الحركة (مثال: راتب الشهر أو مستلزمات بقالة)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // 4. المبلغ Input Field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("المبلغ (أرقام إنجليزية)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // 5. Currency Row labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ILS", "USD", "JOD").forEach { cur ->
                        val selected = currency == cur
                        Card(
                            modifier = Modifier.weight(1f).clickable { currency = cur },
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
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 6. Date Selector
                val mContext = LocalContext.current
                val mCalendar = Calendar.getInstance()
                mCalendar.timeInMillis = selectedDate
                val mDatePickerDialog = android.app.DatePickerDialog(
                    mContext,
                    { _, year, month, dayOfMonth ->
                        val newCal = Calendar.getInstance()
                        newCal.set(Calendar.YEAR, year)
                        newCal.set(Calendar.MONTH, month)
                        newCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        selectedDate = newCal.timeInMillis
                    },
                    mCalendar.get(Calendar.YEAR),
                    mCalendar.get(Calendar.MONTH),
                    mCalendar.get(Calendar.DAY_OF_MONTH)
                )

                OutlinedButton(
                    onClick = { mDatePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تاريخ المعاملة: ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(selectedDate))}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotEmpty() && amt > 0.0) {
                        onConfirm(title, amt, currency, type, categoryId, subCategoryId, incomeSourceId, selectedDate)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("تأكيد وحفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun CategoryCrudModal(
    categories: List<Category>,
    subCategories: List<SubCategory>,
    allTransactions: List<TransactionEntity>,
    onDismiss: () -> Unit,
    onAddCategory: (String, String?) -> Unit,
    onUpdateCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onAddSubCategory: (Int, String) -> Unit,
    onUpdateSubCategory: (SubCategory) -> Unit,
    onDeleteSubCategory: (SubCategory) -> Unit
) {
    val context = LocalContext.current
    var categoryName by remember { mutableStateOf("") }
    var categoryIcon by remember { mutableStateOf("🏷️") }
    var selectedCategoryForSub by remember { mutableStateOf<Category?>(null) }
    var subCategoryName by remember { mutableStateOf("") }

    // Inline edit states
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var editCatName by remember { mutableStateOf("") }
    var editCatIcon by remember { mutableStateOf("") }

    var editingSubCategory by remember { mutableStateOf<SubCategory?>(null) }
    var editSubName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إدارة الفئات والأقسام الفرعية", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier.width(340.dp).height(500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Section 1: Add Category
                Text("إضافة فئة جديدة", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        modifier = Modifier.weight(2f),
                        placeholder = { Text("اسم الفئة") },
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = categoryIcon,
                        onValueChange = { categoryIcon = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("أيقونة") },
                        shape = RoundedCornerShape(8.dp)
                    )
                    Button(
                        onClick = {
                            if (categoryName.isNotEmpty()) {
                                onAddCategory(categoryName, categoryIcon)
                                categoryName = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterVertically),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("أضف")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // Section 2: Inline Editing Category Panel
                if (editingCategory != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("تعديل فئة: ${editingCategory!!.name}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(
                                    value = editCatName,
                                    onValueChange = { editCatName = it },
                                    modifier = Modifier.weight(2f),
                                    placeholder = { Text("الاسم الجديد") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = editCatIcon,
                                    onValueChange = { editCatIcon = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("أيقونة") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { editingCategory = null }) {
                                    Text("إلغاء", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        if (editCatName.isNotEmpty()) {
                                            onUpdateCategory(editingCategory!!.copy(name = editCatName, icon = editCatIcon))
                                            editingCategory = null
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                    ) {
                                    Text("حفظ", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Inline Editing Subcategory Panel
                if (editingSubCategory != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("تعديل الفئة الفرعية: ${editingSubCategory!!.name}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = editSubName,
                                onValueChange = { editSubName = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("الاسم الجديد للفئة الفرعية") },
                                shape = RoundedCornerShape(8.dp)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { editingSubCategory = null }) {
                                    Text("إلغاء", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        if (editSubName.isNotEmpty()) {
                                            onUpdateSubCategory(editingSubCategory!!.copy(name = editSubName))
                                            editingSubCategory = null
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("حفظ", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Section 3: View List + Stats
                Text("الأقسام وفروعها والبيانات المالية المنظمة لها 📂", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(categories) { cat ->
                        // Calculate stats for this category
                        val catTxList = allTransactions.filter { it.categoryId == cat.id }
                        val totalInVal = catTxList.filter { it.type == "INCOME" }.sumOf { it.amount }
                        val totalExVal = catTxList.filter { it.type == "EXPENSE" }.sumOf { it.amount }

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            border = BorderStroke(1.dp, if (selectedCategoryForSub?.id == cat.id) MaterialTheme.colorScheme.primary else Color.Transparent),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).clickable { selectedCategoryForSub = cat }
                                    ) {
                                        Text(cat.icon ?: "📂", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cat.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (selectedCategoryForSub?.id == cat.id) MaterialTheme.colorScheme.primary else Color.Unspecified
                                        )
                                    }

                                    Row {
                                        // Inline editing button for category
                                        IconButton(onClick = {
                                            editingCategory = cat
                                            editCatName = cat.name
                                            editCatIcon = cat.icon ?: ""
                                        }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                        }

                                        // Export buttons for category
                                        IconButton(onClick = {
                                            val reportBytes = buildString {
                                                appendLine("تقرير مالي مفصل للفئة: ${cat.icon ?: ""} ${cat.name}")
                                                appendLine("===========================================")
                                                appendLine("تاريخ التصدير: ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())}")
                                                appendLine("مجموع الواردات: $totalInVal ₪")
                                                appendLine("مجموع المصاريف: $totalExVal ₪")
                                                appendLine("صافي الرصيد للفئة: ${totalInVal - totalExVal} ₪")
                                                appendLine("-------------------------------------------")
                                                catTxList.forEach { tx ->
                                                    val subName = subCategories.find { it.id == tx.subCategoryId }?.name ?: "رئيسية"
                                                    appendLine("- [${if (tx.type == "EXPENSE") "مصروف" else "وارد"}] ${tx.title}: ${tx.amount} ${tx.currency} (القسم الفرعي: $subName) بتاريخ: ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(tx.date))}")
                                                }
                                            }
                                            val shareIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, reportBytes)
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "تصدير كشف حساب الفئة"))
                                        }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = TealPrimary)
                                        }

                                        // Delete button for category
                                        IconButton(onClick = { onDeleteCategory(cat) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }

                                // Interactive statistics summary inside Category representation
                                if (catTxList.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("واردات: ₪%.1f".format(Locale.ENGLISH, totalInVal), fontSize = 10.sp, color = TealPrimary, fontWeight = FontWeight.Bold)
                                        Text("مصاريف: ₪%.1f".format(Locale.ENGLISH, totalExVal), fontSize = 10.sp, color = ExpenseRed, fontWeight = FontWeight.Bold)
                                        Text("الصافي: ₪%.1f".format(Locale.ENGLISH, totalInVal - totalExVal), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                                    }
                                } else {
                                    Text("لا توجد حركات مسجلة لهذه الفئة حتى الآن", fontSize = 9.sp, color = Color.Gray)
                                }

                                // Show sub-categories under this category if selected
                                if (selectedCategoryForSub?.id == cat.id) {
                                    Column(modifier = Modifier.padding(start = 16.dp, top = 6.dp)) {
                                        val subs = subCategories.filter { it.categoryId == cat.id }
                                        subs.forEach { sub ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("- ${sub.name}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Row {
                                                    IconButton(onClick = {
                                                        editingSubCategory = sub
                                                        editSubName = sub.name
                                                    }, modifier = Modifier.size(20.dp)) {
                                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                    IconButton(onClick = { onDeleteSubCategory(sub) }, modifier = Modifier.size(20.dp)) {
                                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                            }
                                        }

                                        // Field to add sub
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = subCategoryName,
                                                onValueChange = { subCategoryName = it },
                                                modifier = Modifier.weight(1f).height(40.dp),
                                                placeholder = { Text("فرع جديد...", fontSize = 11.sp) },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            IconButton(
                                                onClick = {
                                                    if (subCategoryName.isNotEmpty()) {
                                                        onAddSubCategory(cat.id, subCategoryName)
                                                        subCategoryName = ""
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("إغلاق")
            }
        }
    )
}
