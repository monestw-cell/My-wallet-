package com.example.features.debts

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import android.widget.Toast
import com.example.core.database.*
import com.example.features.wallet.WalletViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(viewModel: WalletViewModel) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsState(initial = emptyList())
    val debts by viewModel.debts.collectAsState()
    val payments by viewModel.debtPayments.collectAsState()

    var showAddClientDialog by remember { mutableStateOf(false) }
    var selectedClientForProfile by remember { mutableStateOf<Client?>(null) }
    var showEditClientDialog by remember { mutableStateOf<Client?>(null) }
    var showAddDebtForClient by remember { mutableStateOf<Client?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (selectedClientForProfile == null) {
            // Main directory of clients
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دفتـر العمليات والديـون",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = { showAddClientDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("عميل جديد")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (clients.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👥", fontSize = 48.sp)
                        Text("دفتر العملاء فارغ حالياً", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("أضف عملاء أولاً لتسجيل الديون المتبادلة", color = TextGray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(clients) { client ->
                        val clientDebts = debts.filter { it.clientId == client.id }
                        val lentSum = clientDebts.filter { it.debtType.equals("LENT", ignoreCase = true) }.sumOf { it.remainingAmount }
                        val borrowedSum = clientDebts.filter { it.debtType.equals("BORROWED", ignoreCase = true) }.sumOf { it.remainingAmount }

                        ClientItemCard(
                            client = client,
                            lentAmount = lentSum,
                            borrowedAmount = borrowedSum,
                            onOpenProfile = { selectedClientForProfile = client },
                            onDelete = { viewModel.deleteClient(client) },
                            onEdit = { showEditClientDialog = client },
                            onQuickAddDebt = { showAddDebtForClient = client }
                        )
                    }
                }
            }
        } else {
            // Live client mapping to sync modifications
            val liveClient = clients.find { it.id == selectedClientForProfile!!.id } ?: selectedClientForProfile!!
            // Client detail profile
            ClientProfileView(
                client = liveClient,
                debts = debts,
                payments = payments,
                onBack = { selectedClientForProfile = null },
                onAddDebt = { amount, cur, isLent, dueDate, createdAt ->
                    val walletBalances = viewModel.walletBalances.value
                    val currentBal = walletBalances[cur] ?: 0.0
                    if (isLent && currentBal < amount) {
                        Toast.makeText(context, "⚠️ رصيدك غير كافٍ في المحفظة لإقراض هذا الدين! (الرصيد المتاح: $currentBal $cur)", Toast.LENGTH_LONG).show()
                    } else {
                        viewModel.addDebt(
                            clientId = liveClient.id,
                            totalAmount = amount,
                            currency = cur,
                            debtType = if (isLent) "LENT" else "BORROWED",
                            dueDate = dueDate,
                            createdAt = createdAt
                        )
                    }
                },
                onAddPayment = { debtId, amount ->
                    viewModel.addDebtPayment(debtId, amount, System.currentTimeMillis())
                },
                onDeleteDebt = { debt ->
                    viewModel.deleteDebt(debt)
                },
                onEditProfile = { showEditClientDialog = liveClient }
            )
        }
    }

    if (showAddClientDialog) {
        var clientName by remember { mutableStateOf("") }
        var clientPhone by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddClientDialog = false },
            title = { Text("تسجيل عميل جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("الاسم الكامل للعميل") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("رقم الهاتف (اختياري)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clientName.isNotEmpty()) {
                            viewModel.addClient(clientName, clientPhone.ifEmpty { null })
                            clientName = ""
                            clientPhone = ""
                            showAddClientDialog = false
                        }
                    }
                ) {
                    Text("تسجيل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClientDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showEditClientDialog != null) {
        val editingClient = showEditClientDialog!!
        var editedName by remember(editingClient) { mutableStateOf(editingClient.name) }
        var editedPhone by remember(editingClient) { mutableStateOf(editingClient.phone ?: "") }
        var editedPhotoUri by remember(editingClient) { mutableStateOf(editingClient.photoUri) }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                editedPhotoUri = it.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showEditClientDialog = null },
            title = { Text("تعديل بيانات العميل", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(40.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!editedPhotoUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = editedPhotoUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Text(
                        text = "اضغط لتعديل أو رفع الصورة 📸",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("الاسم الكامل") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editedPhone,
                        onValueChange = { editedPhone = it },
                        label = { Text("رقم الهاتف") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedName.isNotEmpty()) {
                            viewModel.updateClient(
                                editingClient.copy(
                                    name = editedName,
                                    phone = editedPhone.ifEmpty { null },
                                    photoUri = editedPhotoUri
                                )
                            )
                            showEditClientDialog = null
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حفظ التغييرات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditClientDialog = null }) { Text("إلغاء") }
            }
        )
    }

    if (showAddDebtForClient != null) {
        var debtAmountStr by remember { mutableStateOf("") }
        var currency by remember { mutableStateOf("ILS") }
        var isLent by remember { mutableStateOf(true) } // true: lent, false: borrowed
        var daysToDueStr by remember { mutableStateOf("30") }
        var createdDate by remember { mutableStateOf(System.currentTimeMillis()) }
        val targetClient = showAddDebtForClient!!

        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { showAddDebtForClient = null },
            title = { Text("تسجيل دين على: ${targetClient.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { isLent = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isLent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("أقرضته")
                        }
                        Button(
                            onClick = { isLent = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isLent) ExpenseRed else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!isLent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("اقترضت منه")
                        }
                    }

                    OutlinedTextField(
                        value = debtAmountStr,
                        onValueChange = { debtAmountStr = it },
                        label = { Text("مبلغ الدين (أرقام إنجليزية)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Currency Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ILS", "USD", "JOD").forEach { cur ->
                            val isSelected = currency == cur
                            Card(
                                modifier = Modifier.weight(1f).clickable { currency = cur },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(cur, modifier = Modifier.padding(10.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Deadline removed as per user request

                    // Date Picker Button for Debt Creation Date
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = createdDate
                    val datePickerDialog = android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val newCal = Calendar.getInstance()
                            newCal.set(Calendar.YEAR, year)
                            newCal.set(Calendar.MONTH, month)
                            newCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            createdDate = newCal.timeInMillis
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )

                    OutlinedButton(
                        onClick = { datePickerDialog.show() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تاريخ تسجيل الدين: ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(createdDate))}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = debtAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            val walletBalances = viewModel.walletBalances.value
                            val currentBal = walletBalances[currency] ?: 0.0
                            if (isLent && currentBal < amt) {
                                Toast.makeText(context, "⚠️ رصيدك غير كافٍ في المحفظة لإقراض هذا الدين! (الرصيد المتاح: $currentBal $currency)", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.addDebt(
                                    clientId = targetClient.id,
                                    totalAmount = amt,
                                    currency = currency,
                                    debtType = if (isLent) "LENT" else "BORROWED",
                                    dueDate = null,
                                    createdAt = createdDate
                                )
                                showAddDebtForClient = null
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إضافة الدين")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDebtForClient = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun ClientItemCard(
    client: Client,
    lentAmount: Double,
    borrowedAmount: Double,
    onOpenProfile: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onQuickAddDebt: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenProfile() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!client.photoUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = client.photoUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = client.name.take(1).uppercase(Locale.getDefault()),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = client.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (!client.phone.isNullOrEmpty()) {
                            Text(
                                text = client.phone,
                                fontSize = 12.sp,
                                color = TextGray
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (lentAmount > 0) {
                        Text(
                            text = String.format(Locale.ENGLISH, "له: ₪ %,.1f", lentAmount),
                            fontSize = 12.sp,
                            color = TealPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (borrowedAmount > 0) {
                        Text(
                            text = String.format(Locale.ENGLISH, "عليه: ₪ %,.1f", borrowedAmount),
                            fontSize = 12.sp,
                            color = ExpenseRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (lentAmount == 0.0 && borrowedAmount == 0.0) {
                        Text(
                            text = "لا توجد ديون",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                }
            }

            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("تسجيل دين جديد") },
                    onClick = {
                        onQuickAddDebt()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.AddCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )
                DropdownMenuItem(
                    text = { Text("تعديل الاسم والصورة") },
                    onClick = {
                        onEdit()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("حذف ملف العميل", color = Color.Red) },
                    onClick = {
                        onDelete()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientProfileView(
    client: Client,
    debts: List<DebtEntity>,
    payments: List<DebtPaymentEntity>,
    onBack: () -> Unit,
    onAddDebt: (amount: Double, cur: String, isLent: Boolean, dueDate: Long?, createdAt: Long) -> Unit,
    onAddPayment: (debtId: Int, amount: Double) -> Unit,
    onDeleteDebt: (DebtEntity) -> Unit,
    onEditProfile: () -> Unit
) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
    val clientDebts = debts.filter { it.clientId == client.id }

    var showAddDebtDialog by remember { mutableStateOf(false) }
    var selectedDebtForPayment by remember { mutableStateOf<DebtEntity?>(null) }

    // Aggregate statistics
    val totalLent = remember(clientDebts) {
        clientDebts.filter { it.debtType.equals("LENT", ignoreCase = true) }.sumOf { it.totalAmount }
    }
    val totalBorrowed = remember(clientDebts) {
        clientDebts.filter { it.debtType.equals("BORROWED", ignoreCase = true) }.sumOf { it.totalAmount }
    }
    val remainingLent = remember(clientDebts) {
        clientDebts.filter { it.debtType.equals("LENT", ignoreCase = true) }.sumOf { it.remainingAmount }
    }
    val remainingBorrowed = remember(clientDebts) {
        clientDebts.filter { it.debtType.equals("BORROWED", ignoreCase = true) }.sumOf { it.remainingAmount }
    }
    val clientDebtIds = remember(clientDebts) { clientDebts.map { it.id }.toSet() }
    val clientPayments = remember(payments, clientDebtIds) { payments.filter { it.debtId in clientDebtIds } }
    val totalPaymentsAmt = remember(clientPayments) { clientPayments.sumOf { it.amountPaid } }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!client.photoUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = client.photoUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = client.name.take(1).uppercase(Locale.getDefault()),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(client.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("كشف حساب وتفاصيل ديون العميل", fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Quick profile edit button
            Row {
                IconButton(onClick = onEditProfile) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل الملف الشخصي", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Modern visual statistics card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "📊 إحصائيات العمليات المالية مع العميل",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Lent stats
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("إجمالي ما أقرضته 📤", fontSize = 10.sp, color = TextGray)
                            Text("₪ %,.1f".format(Locale.ENGLISH, totalLent), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("المتبقي: ₪ %,.1f".format(Locale.ENGLISH, remainingLent), fontSize = 9.sp, color = TealPrimary.copy(alpha = 0.8f))
                        }
                    }

                    // Borrowed stats
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("إجمالي ما اقترضته 📥", fontSize = 10.sp, color = TextGray)
                            Text("₪ %,.1f".format(Locale.ENGLISH, totalBorrowed), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("المتبقي: ₪ %,.1f".format(Locale.ENGLISH, remainingBorrowed), fontSize = 9.sp, color = ExpenseRed.copy(alpha = 0.8f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إجمالي دفعات السداد الجزئية حتّى الآن:", fontSize = 11.sp, color = TextGray, fontWeight = FontWeight.Medium)
                    Text("₪ %,.1f".format(Locale.ENGLISH, totalPaymentsAmt), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = TealPrimary)
                }
            }
        }

        // Action row to add debts or export files
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAddDebtDialog = true },
                modifier = Modifier.weight(1.3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ثبت دين جديد")
            }

            var exportExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { exportExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ImportExport, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تصدير ملفات")
                }

                DropdownMenu(expanded = exportExpanded, onDismissRequest = { exportExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("تصدير كشف حساب (نص)") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                        onClick = {
                            exportExpanded = false
                            val reportText = buildString {
                                appendLine("📑 كشف حساب مالي - تطبيق محفظتي")
                                appendLine("=======================================")
                                appendLine("العميل: ${client.name}")
                                if (client.phone != null) appendLine("الهاتف: ${client.phone}")
                                appendLine("التاريخ والوقت: ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())}")
                                appendLine("=======================================")
                                appendLine("إحصائيات إجمالية:")
                                appendLine("- إجمالي قيمة الديون المقرضة: $totalLent ₪ (المتبقي: $remainingLent ₪)")
                                appendLine("- إجمالي قيمة الديون المقترضة: $totalBorrowed ₪ (المتبقي: $remainingBorrowed ₪)")
                                appendLine("- إجمالي السدادات المستلمة/المرسلة: $totalPaymentsAmt ₪")
                                appendLine("---------------------------------------")
                                appendLine("الديون المتبادلة وتفاصيل السداد:")
                                clientDebts.forEachIndexed { index, debt ->
                                    val status = if (debt.debtType.equals("LENT", ignoreCase = true)) "دين أقرضتـه للعميل (لنا)" else "دين اقترضتـه من العميل (علينا)"
                                    val currencySymbol = when(debt.currency) { "ILS" -> "₪" "USD" -> "$" "JOD" -> "JD" else -> debt.currency }
                                    appendLine("${index + 1}. $status:")
                                    appendLine("   - المبلغ الكلي: ${debt.totalAmount} $currencySymbol | المتبقي غير المسدد: ${debt.remainingAmount} $currencySymbol")
                                    val pmtsForThis = payments.filter { it.debtId == debt.id }
                                    if (pmtsForThis.isNotEmpty()) {
                                        appendLine("   - دفعات السداد الجزئي لهذا الدين:")
                                        pmtsForThis.forEach { pmt ->
                                            appendLine("     * سداد بقيمة: ${pmt.amountPaid} $currencySymbol بالتاريخ: ${sdf.format(Date(pmt.paymentDate))}")
                                        }
                                    }
                                }
                                appendLine("=======================================")
                                appendLine("تم تصديره عبر تطبيق الحسابات المالي - محفظتي")
                            }
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, reportText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة كشف الحساب التفصيلي"))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("تصدير كملف PDF 📄") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = TealPrimary) },
                        onClick = {
                            exportExpanded = false
                            val pdfLines = mutableListOf<String>()
                            pdfLines.add("العميل: ${client.name}")
                            if (client.phone != null) pdfLines.add("الهاتف: ${client.phone}")
                            pdfLines.add("تاريخ التصدير: ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())}")
                            pdfLines.add("---------------------------------------")
                            pdfLines.add("إجمالي قيمة الديون المقرضة: $totalLent ₪ (المتبقي: $remainingLent ₪)")
                            pdfLines.add("إجمالي قيمة الديون المقترضة: $totalBorrowed ₪ (المتبقي: $remainingBorrowed ₪)")
                            pdfLines.add("إجمالي السدادات المستلمة/المرسلة: $totalPaymentsAmt ₪")
                            pdfLines.add("---------------------------------------")
                            pdfLines.add("تفاصيل الديون والسدادات:")
                            clientDebts.forEachIndexed { idx, debt ->
                                val status = if (debt.debtType.equals("LENT", ignoreCase = true)) "أقرضتـه للعميل (لنا)" else "اقترضتـه من العميل (علينا)"
                                pdfLines.add("${idx + 1}. [ $status ] المبلغ: ${debt.totalAmount} ${debt.currency} | المتبقي : ${debt.remainingAmount} ${debt.currency}")
                                val pmtsForThis = payments.filter { it.debtId == debt.id }
                                if (pmtsForThis.isNotEmpty()) {
                                    pmtsForThis.forEach { pmt ->
                                        pdfLines.add("   * سدد بقيمة: ${pmt.amountPaid} ${debt.currency} بتاريخ: ${sdf.format(Date(pmt.paymentDate))}")
                                    }
                                }
                            }
                            com.example.core.utils.PdfExporter.exportToPdf(
                                context = context,
                                filename = "كشف_حساب_${client.name}",
                                title = "كشف حساب مالي - تطبيق محفظتي",
                                contentLines = pdfLines
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("تصدير كملف Excel (CSV)") },
                        leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, tint = TealPrimary) },
                        onClick = {
                            exportExpanded = false
                            val csvText = buildString {
                                appendLine("العميل,${client.name}")
                                appendLine("رقم_الهاتف,${client.phone ?: "غير متوفر"}")
                                appendLine("تاريخ_التصدير,${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}")
                                appendLine()
                                appendLine("معرف_الدين,نوع_الدين,المبلغ_الاصلي,المبلغ_المتبقي,العملة,تاريخ_النشوء")
                                clientDebts.forEach { d ->
                                    appendLine("${d.id},${if (d.debtType == "LENT") "أقرضته" else "اقترضته"},${d.totalAmount},${d.remainingAmount},${d.currency},${sdf.format(Date(d.createdAt))}")
                                }
                            }
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, csvText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "تصدير كملف CSV للإكسل"))
                        }
                    )
                }
            }
        }

        if (clientDebts.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("لا توجد ديون مسجلة للعميل حتى الآن", color = TextGray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(clientDebts) { debt ->
                    val isLent = debt.debtType.equals("LENT", ignoreCase = true)
                    val symbol = when(debt.currency) { "ILS" -> "₪" "USD" -> "$" "JOD" -> "JD" else -> debt.currency }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (debt.remainingAmount == 0.0) MaterialTheme.colorScheme.outlineVariant else if (isLent) TealPrimary.copy(alpha = 0.4f) else ExpenseRed.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isLent) "دين أقرضتـه للعميل" else "دين اقترضتـه من العميل",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isLent) TealPrimary else ExpenseRed
                                    )
                                    Text(
                                        text = "تاريخ الإنشاء: ${sdf.format(Date(debt.createdAt))}",
                                        fontSize = 11.sp,
                                        color = TextGray
                                    )
                                }

                                Row {
                                    IconButton(onClick = { onDeleteDebt(debt) }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("المبلغ الإجمالي", fontSize = 11.sp, color = TextGray)
                                    Text("${debt.totalAmount} $symbol", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("المبلغ المتبقي غير المسدد", fontSize = 11.sp, color = TextGray)
                                    Text(
                                        text = "${debt.remainingAmount} $symbol",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = if (debt.remainingAmount == 0.0) TextGray else if (isLent) TealPrimary else ExpenseRed
                                    )
                                }
                            }

                            if (debt.dueDate != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "تاريخ الاستحقاق المتوقع: ${sdf.format(Date(debt.dueDate))}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // List Payments for this debt
                            val debtPmts = payments.filter { it.debtId == debt.id }
                            if (debtPmts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("سجل الدفعات الجزئية:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextGray)
                                debtPmts.forEach { pmt ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("سداد بقيمة: ${pmt.amountPaid} $symbol", fontSize = 11.sp)
                                        Text(sdf.format(Date(pmt.paymentDate)), fontSize = 11.sp, color = TextGray)
                                    }
                                }
                            }

                            if (debt.remainingAmount > 0.0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { selectedDebtForPayment = debt },
                                    modifier = Modifier.align(Alignment.End),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("سداد جزء", fontSize = 12.sp)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .padding(top = 8.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TealPrimary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("تم السداد بالكامل ✅", fontSize = 11.sp, color = TealPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDebtDialog) {
        var debtAmountStr by remember { mutableStateOf("") }
        var currency by remember { mutableStateOf("ILS") }
        var isLent by remember { mutableStateOf(true) } // true: lent, false: borrowed
        var daysToDueStr by remember { mutableStateOf("30") }
        var createdDate by remember { mutableStateOf(System.currentTimeMillis()) }

        AlertDialog(
            onDismissRequest = { showAddDebtDialog = false },
            title = { Text("تسجيل دين على العميل") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { isLent = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isLent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("أنا أقرضته")
                        }
                        Button(
                            onClick = { isLent = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isLent) ExpenseRed else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!isLent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("أنا اقترضت منه")
                        }
                    }

                    OutlinedTextField(
                        value = debtAmountStr,
                        onValueChange = { debtAmountStr = it },
                        label = { Text("مبلغ الدين (أرقام إنجليزية)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Currency Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ILS", "USD", "JOD").forEach { cur ->
                            val isSelected = currency == cur
                            Card(
                                modifier = Modifier.weight(1f).clickable { currency = cur },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(cur, modifier = Modifier.padding(10.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Deadline removed as per user request

                    // Date Picker Button for Debt Creation Date
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = createdDate
                    val datePickerDialog = android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val newCal = Calendar.getInstance()
                            newCal.set(Calendar.YEAR, year)
                            newCal.set(Calendar.MONTH, month)
                            newCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            createdDate = newCal.timeInMillis
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )

                    OutlinedButton(
                        onClick = { datePickerDialog.show() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تاريخ تسجيل الدين: ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(createdDate))}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = debtAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onAddDebt(amt, currency, isLent, null, createdDate)
                            showAddDebtDialog = false
                        }
                    }
                ) {
                    Text("إضافة الدين")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDebtDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (selectedDebtForPayment != null) {
        var partialPaymentStr by remember { mutableStateOf("") }
        val dbt = selectedDebtForPayment!!
        val paymentCurrencySymbol = when(dbt.currency) { "ILS" -> "₪" "USD" -> "$" "JOD" -> "JD" else -> dbt.currency }

        AlertDialog(
            onDismissRequest = { selectedDebtForPayment = null },
            title = { Text("تسديد دفعة مالية جزئية") },
            text = {
                Column {
                    Text("العميل الملتزم بالدفع: ${client.name}")
                    Text("الرصيد الكلي المتبقي: ${dbt.remainingAmount} $paymentCurrencySymbol", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = partialPaymentStr,
                        onValueChange = { partialPaymentStr = it },
                        label = { Text("مبلغ السداد المستلم (أرقام إنجليزية)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paymentAmt = partialPaymentStr.toDoubleOrNull() ?: 0.0
                        if (paymentAmt > 0 && paymentAmt <= dbt.remainingAmount) {
                            onAddPayment(dbt.id, paymentAmt)
                            selectedDebtForPayment = null
                        }
                    }
                ) {
                    Text("تسجيل وتعديل الرصيد")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDebtForPayment = null }) { Text("إلغاء") }
            }
        )
    }
}
