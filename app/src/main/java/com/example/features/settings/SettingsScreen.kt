package com.example.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.ExchangeRateEntity
import com.example.features.auth.SecurityPrefs
import com.example.features.wallet.WalletViewModel
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: WalletViewModel,
    prefs: SecurityPrefs,
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit
) {
    val exchangeRateOpt by viewModel.exchangeRate.collectAsState()
    val rates = exchangeRateOpt ?: ExchangeRateEntity(1, 3.7, 5.2)

    var appLockEnabled by remember { mutableStateOf(prefs.isLockEnabled()) }
    var biometricEnabled by remember { mutableStateOf(prefs.isBiometricEnabled()) }

    var showPinDialog by remember { mutableStateOf(false) }
    var showRatesDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "إعدادات التطبيق والأمان",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // General settings Card
        Text("المظهر العام وسعر الصرف", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("المظهر الداكن (Dark Mode)") },
                    supportingContent = { Text("تحويل واجهة التطبيق إلى مظهر ليلي مريح للعين") },
                    trailingContent = {
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = onThemeToggle
                        )
                    },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.Brightness4, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                )

                Divider()

                ListItem(
                    headlineContent = { Text("أسعار صرف العملات اليدوية") },
                    supportingContent = {
                        Text(
                            String.format(
                                Locale.ENGLISH,
                                "الدولار: ₪ %.2f | الدينار: ₪ %.2f",
                                rates.usdToIls,
                                rates.jodToIls
                            )
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { showRatesDialog = true }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.CurrencyExchange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                )
            }
        }

        // Security card Settings
        Text("حماية التطبيق والخصوصية", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("قفل التطبيق الآمن") },
                    supportingContent = { Text("تفعيل رمز PIN والتحقق عند بدء التشغيل") },
                    trailingContent = {
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = { checked ->
                                if (checked && prefs.getPin().isNullOrEmpty()) {
                                    // Must prompt to set PIN first
                                    showPinDialog = true
                                } else {
                                    prefs.setLockEnabled(checked)
                                    appLockEnabled = checked
                                }
                            }
                        )
                    },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                )

                Divider()

                ListItem(
                    headlineContent = { Text("ضبط وتغيير رمز PIN") },
                    supportingContent = {
                        val userPin = prefs.getPin()
                        Text(if (userPin.isNullOrEmpty()) "لم يتم تعيين رمز رمز PIN بعد" else "تم تعيين رمز PIN بنجاح")
                    },
                    modifier = Modifier.clickable { showPinDialog = true },
                    trailingContent = {
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.Dialpad, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                )

                Divider()

                ListItem(
                    headlineContent = { Text("المصادقة ببصمة الإصبع") },
                    supportingContent = { Text("استخدام المقاييس الحيوية البيومترية لتأمين الدخول السريع") },
                    trailingContent = {
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { checked ->
                                prefs.setBiometricEnabled(checked)
                                biometricEnabled = checked
                            }
                        )
                    },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "تطبيق محفظتي الإصدار الاوفلاين ١.٠ ✨",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    // PIN dialog entry modal
    if (showPinDialog) {
        var inputPin by remember { mutableStateOf("") }
        var inputPinConfirm by remember { mutableStateOf("") }
        var pinStep by remember { mutableStateOf(1) } // 1: set PIN, 2: confirm PIN
        var dialogError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = {
                Text(if (pinStep == 1) "تعيين رمز PIN الجديد" else "تأكيد رمز PIN")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("أدخل رمزًا مكونًا من 4 أرقام لتأمين محفظتك المالية.", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = if (pinStep == 1) inputPin else inputPinConfirm,
                        onValueChange = { newVal ->
                            if (newVal.length <= 4 && newVal.all { it.isDigit() }) {
                                if (pinStep == 1) inputPin = newVal else inputPinConfirm = newVal
                                dialogError = ""
                            }
                        },
                        label = { Text("رمز PIN (4 أرقام)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (dialogError.isNotEmpty()) {
                        Text(dialogError, color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinStep == 1) {
                            if (inputPin.length == 4) {
                                pinStep = 2
                            } else {
                                dialogError = "يجب أن يتكون الرمز من 4 أرقام تمامًا!"
                            }
                        } else {
                            if (inputPinConfirm == inputPin) {
                                prefs.setPin(inputPin)
                                prefs.setLockEnabled(true)
                                appLockEnabled = true
                                showPinDialog = false
                            } else {
                                dialogError = "الرموز غير متطابقة! ابدأ مجددًا"
                                inputPin = ""
                                inputPinConfirm = ""
                                pinStep = 1
                            }
                        }
                    }
                ) {
                    Text(if (pinStep == 1) "التالي" else "تأكيد وحفظ")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPinDialog = false
                        inputPin = ""
                        inputPinConfirm = ""
                        pinStep = 1
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Rates dialog entry modal
    if (showRatesDialog) {
        var usdStr by remember { mutableStateOf(rates.usdToIls.toString()) }
        var jodStr by remember { mutableStateOf(rates.jodToIls.toString()) }

        AlertDialog(
            onDismissRequest = { showRatesDialog = false },
            title = { Text("تحديث أسعار الصرف اليدوية") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("قم بتحديد أسعار الصرف الحالية مقابل الشيكل الإسرائيلي (ILS):", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = usdStr,
                        onValueChange = { usdStr = it },
                        label = { Text("سعر الدولار مقابل الشيكل (USD -> ILS)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = jodStr,
                        onValueChange = { jodStr = it },
                        label = { Text("سعر الدينار مقابل الشيكل (JOD -> ILS)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val usd = usdStr.toDoubleOrNull() ?: 3.7
                        val jod = jodStr.toDoubleOrNull() ?: 5.2
                        viewModel.updateExchangeRates(usd, jod)
                        showRatesDialog = false
                    }
                ) {
                    Text("حفظ وتحديث أسعار الصرف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRatesDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
