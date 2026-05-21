package com.example.core.database

import androidx.room.*

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // Arabic name
    val icon: String? = null
)

@Entity(
    tableName = "subcategories",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class SubCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val name: String
)

@Entity(tableName = "income_sources")
data class IncomeSource(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String? = null
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val currency: String, // "ILS", "USD", "JOD"
    val type: String, // "INCOME", "EXPENSE"
    val categoryId: Int,
    val subCategoryId: Int? = null,
    val incomeSourceId: Int? = null,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String? = null,
    val photoUri: String? = null
)

@Entity(
    tableName = "debts",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["clientId"])]
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val totalAmount: Double,
    val remainingAmount: Double,
    val currency: String, // "ILS", "USD", "JOD"
    val debtType: String, // "LENT", "BORROWED"
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "debt_payments",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["debtId"])]
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val debtId: Int,
    val amountPaid: Double,
    val paymentDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val id: Int = 1,
    val usdToIls: Double = 3.7,
    val jodToIls: Double = 5.2,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "saving_goals")
data class SavingGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val currency: String = "ILS",
    val savingDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "budget_limits")
data class BudgetLimit(
    @PrimaryKey val id: Int = 1,
    val monthlyLimit: Double = 0.0,
    val currency: String = "ILS"
)
