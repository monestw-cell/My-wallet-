package com.example.features.wallet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = WalletRepository(database.appDao())

    // Live Flowing data from Room
    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subCategories: StateFlow<List<SubCategory>> = repository.allSubCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeSources: StateFlow<List<IncomeSource>> = repository.allIncomeSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clients: StateFlow<List<Client>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debts: StateFlow<List<DebtEntity>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debtPayments: StateFlow<List<DebtPaymentEntity>> = repository.allDebtPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exchangeRate: StateFlow<ExchangeRateEntity?> = repository.exchangeRate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExchangeRateEntity(id = 1, usdToIls = 3.7, jodToIls = 5.2))

    val savingGoals: StateFlow<List<SavingGoal>> = repository.allSavingGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgetLimit: StateFlow<BudgetLimit?> = repository.budgetLimit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetLimit(id = 1, monthlyLimit = 0.0, currency = "ILS"))

    // Dynamic Balance calculation state flow
    val walletBalances: StateFlow<Map<String, Double>> = combine(
        transactions, debts, debtPayments
    ) { txs, dbts, pmts ->
        mapOf(
            "ILS" to calculateDynamicBalance("ILS", txs, dbts, pmts),
            "USD" to calculateDynamicBalance("USD", txs, dbts, pmts),
            "JOD" to calculateDynamicBalance("JOD", txs, dbts, pmts)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), mapOf("ILS" to 0.0, "USD" to 0.0, "JOD" to 0.0))

    private fun calculateDynamicBalance(
        currency: String,
        txs: List<TransactionEntity>,
        dbts: List<DebtEntity>,
        pmts: List<DebtPaymentEntity>
    ): Double {
        var balance = 0.0
        // standard transactions
        for (tx in txs) {
            if (tx.currency.equals(currency, ignoreCase = true)) {
                if (tx.type.equals("INCOME", ignoreCase = true)) {
                    balance += tx.amount
                } else {
                    balance -= tx.amount
                }
            }
        }
        // Debts
        // borrowing money gives us instant wallet cash (+totalAmount)
        // lending money takes instant wallet cash (-totalAmount)
        for (debt in dbts) {
            if (debt.currency.equals(currency, ignoreCase = true)) {
                if (debt.debtType.equals("BORROWED", ignoreCase = true)) {
                    balance += debt.totalAmount
                } else {
                    balance -= debt.totalAmount
                }
            }
        }

        // Debt Payments
        // pay back borrowed debt -> decreases wallet cash (-amountPaid)
        // collect payment for lent debt -> increases wallet cash (+amountPaid)
        for (pay in pmts) {
            val parentDebt = dbts.find { it.id == pay.debtId }
            if (parentDebt != null && parentDebt.currency.equals(currency, ignoreCase = true)) {
                if (parentDebt.debtType.equals("BORROWED", ignoreCase = true)) {
                    balance -= pay.amountPaid
                } else {
                    balance += pay.amountPaid
                }
            }
        }
        return balance
    }

    // Categories Operations
    fun addCategory(name: String, icon: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCategory(Category(name = name, icon = icon))
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(category)
        }
    }

    // Subcategories Operations
    fun addSubCategory(categoryId: Int, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSubCategory(SubCategory(categoryId = categoryId, name = name))
        }
    }

    fun updateSubCategory(subCategory: SubCategory) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSubCategory(subCategory)
        }
    }

    fun deleteSubCategory(subCategory: SubCategory) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSubCategory(subCategory)
        }
    }

    // Income Sources
    fun addIncomeSource(name: String, icon: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertIncomeSource(IncomeSource(name = name, icon = icon))
        }
    }

    fun updateIncomeSource(incomeSource: IncomeSource) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateIncomeSource(incomeSource)
        }
    }

    fun deleteIncomeSource(incomeSource: IncomeSource) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteIncomeSource(incomeSource)
        }
    }

    // Transactions
    fun addTransaction(
        title: String,
        amount: Double,
        currency: String,
        type: String,
        categoryId: Int,
        subCategoryId: Int?,
        incomeSourceId: Int?,
        date: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTransaction(
                TransactionEntity(
                    title = title,
                    amount = amount,
                    currency = currency,
                    type = type,
                    categoryId = categoryId,
                    subCategoryId = subCategoryId,
                    incomeSourceId = incomeSourceId,
                    date = date
                )
            )
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(transaction)
        }
    }

    // Clients
    fun addClient(name: String, phone: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertClient(Client(name = name, phone = phone))
        }
    }

    fun updateClient(client: Client) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateClient(client)
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteClient(client)
        }
    }

    // Debts
    fun addDebt(
        clientId: Int,
        totalAmount: Double,
        currency: String,
        debtType: String,
        dueDate: Long?,
        createdAt: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertDebt(
                DebtEntity(
                    clientId = clientId,
                    totalAmount = totalAmount,
                    remainingAmount = totalAmount, // initially same
                    currency = currency,
                    debtType = debtType,
                    dueDate = dueDate,
                    createdAt = createdAt
                )
            )
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDebt(debt)
        }
    }

    // Debt Payments
    fun addDebtPayment(debtId: Int, amountPaid: Double, date: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            // First log the payment
            repository.insertDebtPayment(
                DebtPaymentEntity(
                    debtId = debtId,
                    amountPaid = amountPaid,
                    paymentDate = date
                )
            )
            // Relieve remaining balance
            val existingDebts = debts.value
            val parentDebt = existingDebts.find { it.id == debtId }
            if (parentDebt != null) {
                val nextRemaining = (parentDebt.remainingAmount - amountPaid).coerceAtLeast(0.0)
                repository.updateDebt(parentDebt.copy(remainingAmount = nextRemaining))
            }
        }
    }

    fun updateExchangeRates(usdToIls: Double, jodToIls: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertExchangeRate(
                ExchangeRateEntity(
                    id = 1,
                    usdToIls = usdToIls,
                    jodToIls = jodToIls,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    // Saving Goals operations
    fun addSavingGoal(name: String, target: Double, currency: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSavingGoal(SavingGoal(name = name, targetAmount = target, currency = currency))
        }
    }

    fun updateSavingGoal(goal: SavingGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSavingGoal(goal)
        }
    }

    fun deleteSavingGoal(goal: SavingGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSavingGoal(goal)
        }
    }

    // Budget Limits operations
    fun updateBudgetLimit(limit: Double, currency: String = "ILS") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertBudgetLimit(BudgetLimit(id = 1, monthlyLimit = limit, currency = currency))
        }
    }
}
