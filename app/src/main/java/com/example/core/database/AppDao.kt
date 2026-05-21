package com.example.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Categories
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    // SubCategories
    @Query("SELECT * FROM subcategories WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getSubCategoriesForCategory(categoryId: Int): Flow<List<SubCategory>>

    @Query("SELECT * FROM subcategories ORDER BY name ASC")
    fun getAllSubCategories(): Flow<List<SubCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubCategory(subCategory: SubCategory): Long

    @Update
    suspend fun updateSubCategory(subCategory: SubCategory)

    @Delete
    suspend fun deleteSubCategory(subCategory: SubCategory)

    // Income Sources
    @Query("SELECT * FROM income_sources ORDER BY name ASC")
    fun getAllIncomeSources(): Flow<List<IncomeSource>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeSource(incomeSource: IncomeSource): Long

    @Update
    suspend fun updateIncomeSource(incomeSource: IncomeSource)

    @Delete
    suspend fun deleteIncomeSource(incomeSource: IncomeSource)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsInDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    // Clients
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)

    // Debts
    @Query("SELECT * FROM debts ORDER BY createdAt DESC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun getDebtsForClient(clientId: Int): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    // Debt Payments
    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY paymentDate DESC")
    fun getPaymentsForDebt(debtId: Int): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments ORDER BY paymentDate DESC")
    fun getAllDebtPayments(): Flow<List<DebtPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtPayment(payment: DebtPaymentEntity): Long

    @Delete
    suspend fun deleteDebtPayment(payment: DebtPaymentEntity)

    // Exchange Rates
    @Query("SELECT * FROM exchange_rates WHERE id = 1")
    fun getExchangeRate(): Flow<ExchangeRateEntity?>

    @Query("SELECT * FROM exchange_rates WHERE id = 1")
    suspend fun getExchangeRateOneShot(): ExchangeRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExchangeRate(exchangeRate: ExchangeRateEntity)

    // Saving Goals
    @Query("SELECT * FROM saving_goals ORDER BY savingDate DESC")
    fun getAllSavingGoals(): Flow<List<SavingGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingGoal(goal: SavingGoal): Long

    @Update
    suspend fun updateSavingGoal(goal: SavingGoal)

    @Delete
    suspend fun deleteSavingGoal(goal: SavingGoal)

    // Budget Limits
    @Query("SELECT * FROM budget_limits WHERE id = 1")
    fun getBudgetLimit(): Flow<BudgetLimit?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetLimit(limit: BudgetLimit)
}
