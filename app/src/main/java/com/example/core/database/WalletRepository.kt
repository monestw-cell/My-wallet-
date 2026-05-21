package com.example.core.database

import kotlinx.coroutines.flow.Flow

class WalletRepository(private val dao: AppDao) {

    // Categories
    val allCategories: Flow<List<Category>> = dao.getAllCategories()
    suspend fun insertCategory(category: Category): Long = dao.insertCategory(category)
    suspend fun updateCategory(category: Category) = dao.updateCategory(category)
    suspend fun deleteCategory(category: Category) = dao.deleteCategory(category)

    // SubCategories
    val allSubCategories: Flow<List<SubCategory>> = dao.getAllSubCategories()
    fun getSubCategoriesForCategory(categoryId: Int): Flow<List<SubCategory>> = dao.getSubCategoriesForCategory(categoryId)
    suspend fun insertSubCategory(subCategory: SubCategory): Long = dao.insertSubCategory(subCategory)
    suspend fun updateSubCategory(subCategory: SubCategory) = dao.updateSubCategory(subCategory)
    suspend fun deleteSubCategory(subCategory: SubCategory) = dao.deleteSubCategory(subCategory)

    // Income Sources
    val allIncomeSources: Flow<List<IncomeSource>> = dao.getAllIncomeSources()
    suspend fun insertIncomeSource(incomeSource: IncomeSource): Long = dao.insertIncomeSource(incomeSource)
    suspend fun updateIncomeSource(incomeSource: IncomeSource) = dao.updateIncomeSource(incomeSource)
    suspend fun deleteIncomeSource(incomeSource: IncomeSource) = dao.deleteIncomeSource(incomeSource)

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    fun getTransactionsInDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        dao.getTransactionsInDateRange(startDate, endDate)
    suspend fun insertTransaction(transaction: TransactionEntity): Long = dao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: TransactionEntity) = dao.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: TransactionEntity) = dao.deleteTransaction(transaction)

    // Clients
    val allClients: Flow<List<Client>> = dao.getAllClients()
    suspend fun insertClient(client: Client): Long = dao.insertClient(client)
    suspend fun updateClient(client: Client) = dao.updateClient(client)
    suspend fun deleteClient(client: Client) = dao.deleteClient(client)

    // Debts
    val allDebts: Flow<List<DebtEntity>> = dao.getAllDebts()
    fun getDebtsForClient(clientId: Int): Flow<List<DebtEntity>> = dao.getDebtsForClient(clientId)
    suspend fun insertDebt(debt: DebtEntity): Long = dao.insertDebt(debt)
    suspend fun updateDebt(debt: DebtEntity) = dao.updateDebt(debt)
    suspend fun deleteDebt(debt: DebtEntity) = dao.deleteDebt(debt)

    // Debt Payments
    val allDebtPayments: Flow<List<DebtPaymentEntity>> = dao.getAllDebtPayments()
    fun getPaymentsForDebt(debtId: Int): Flow<List<DebtPaymentEntity>> = dao.getPaymentsForDebt(debtId)
    suspend fun insertDebtPayment(payment: DebtPaymentEntity): Long {
        // Also update the remaining balance on the Debt object
        val debtList = dao.getPaymentsForDebt(payment.debtId)
        return dao.insertDebtPayment(payment)
    }
    suspend fun deleteDebtPayment(payment: DebtPaymentEntity) = dao.deleteDebtPayment(payment)

    // Exchange Rates
    val exchangeRate: Flow<ExchangeRateEntity?> = dao.getExchangeRate()
    suspend fun getExchangeRateOneShot(): ExchangeRateEntity? = dao.getExchangeRateOneShot()
    suspend fun insertExchangeRate(exchangeRate: ExchangeRateEntity) = dao.insertExchangeRate(exchangeRate)

    // Saving Goals
    val allSavingGoals: Flow<List<SavingGoal>> = dao.getAllSavingGoals()
    suspend fun insertSavingGoal(goal: SavingGoal): Long = dao.insertSavingGoal(goal)
    suspend fun updateSavingGoal(goal: SavingGoal) = dao.updateSavingGoal(goal)
    suspend fun deleteSavingGoal(goal: SavingGoal) = dao.deleteSavingGoal(goal)

    // Budget Limits
    val budgetLimit: Flow<BudgetLimit?> = dao.getBudgetLimit()
    suspend fun insertBudgetLimit(limit: BudgetLimit) = dao.insertBudgetLimit(limit)
}
