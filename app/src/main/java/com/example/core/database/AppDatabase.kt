package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Category::class,
        SubCategory::class,
        IncomeSource::class,
        TransactionEntity::class,
        Client::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
        ExchangeRateEntity::class,
        SavingGoal::class,
        BudgetLimit::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, coroutineScope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_wallet_database"
                )
                    .addCallback(DatabaseCallback(coroutineScope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.appDao())
                }
            }
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.appDao())
                }
            }
        }

        suspend fun populateDatabase(dao: AppDao) {
            // Pre-populate Exchange Rate
            dao.insertExchangeRate(ExchangeRateEntity(id = 1, usdToIls = 3.7, jodToIls = 5.2))

            // Pre-populate Categories & Subcategories (Arabic)
            val cat1 = dao.insertCategory(Category(name = "الطعام والشراب", icon = "🍕"))
            val cat2 = dao.insertCategory(Category(name = "المنزل والسلع", icon = "🛋️"))
            val cat3 = dao.insertCategory(Category(name = "المواصلات والسيارة", icon = "🏎️"))
            val cat4 = dao.insertCategory(Category(name = "الصحة والطب", icon = "🩺"))
            val cat5 = dao.insertCategory(Category(name = "الترفيه والأنشطة", icon = "👾"))
            val cat6 = dao.insertCategory(Category(name = "التسوق والمشتريات", icon = "🛍️"))
            val cat7 = dao.insertCategory(Category(name = "التعليم والدراسة", icon = "🎓"))
            val cat8 = dao.insertCategory(Category(name = "فواتير والتزامات", icon = "💳"))

            dao.insertSubCategory(SubCategory(categoryId = cat1.toInt(), name = "خضار وفواكه"))
            dao.insertSubCategory(SubCategory(categoryId = cat1.toInt(), name = "مطاعم ومقاهي"))
            dao.insertSubCategory(SubCategory(categoryId = cat1.toInt(), name = "مواد تموينية"))

            dao.insertSubCategory(SubCategory(categoryId = cat2.toInt(), name = "فواتير كهرباء وماء"))
            dao.insertSubCategory(SubCategory(categoryId = cat2.toInt(), name = "إيجار المنزل"))
            dao.insertSubCategory(SubCategory(categoryId = cat2.toInt(), name = "أثاث وصيانة"))

            dao.insertSubCategory(SubCategory(categoryId = cat3.toInt(), name = "وقود سيارة"))
            dao.insertSubCategory(SubCategory(categoryId = cat3.toInt(), name = "أجرة تاكسي وحافلة"))
            dao.insertSubCategory(SubCategory(categoryId = cat3.toInt(), name = "تأمين ورسوم"))

            dao.insertSubCategory(SubCategory(categoryId = cat4.toInt(), name = "أدوية وصيدلية"))
            dao.insertSubCategory(SubCategory(categoryId = cat4.toInt(), name = "زيارة عيادات ويوميات طبيّة"))

            dao.insertSubCategory(SubCategory(categoryId = cat5.toInt(), name = "اشتراكات وسينما"))
            dao.insertSubCategory(SubCategory(categoryId = cat5.toInt(), name = "هدايا وألعاب"))

            dao.insertSubCategory(SubCategory(categoryId = cat6.toInt(), name = "ملابس وأحذية"))
            dao.insertSubCategory(SubCategory(categoryId = cat6.toInt(), name = "أجهزة وإلكترونيات"))

            dao.insertSubCategory(SubCategory(categoryId = cat7.toInt(), name = "كتب ودورات تدريبية"))
            dao.insertSubCategory(SubCategory(categoryId = cat7.toInt(), name = "أقساط ورسوم دراسية"))

            dao.insertSubCategory(SubCategory(categoryId = cat8.toInt(), name = "فواتير اتصالات وإنترنت"))
            dao.insertSubCategory(SubCategory(categoryId = cat8.toInt(), name = "رسوم تراخيص حكومية"))

            // Pre-populate Income Sources with the cool comprehensive list
            dao.insertIncomeSource(IncomeSource(name = "الراتب الشهري الأساسي", icon = "💸"))
            dao.insertIncomeSource(IncomeSource(name = "العمل الحر والمستقل", icon = "💻"))
            dao.insertIncomeSource(IncomeSource(name = "أرباح تجارية ومبيعات", icon = "🛍️"))
            dao.insertIncomeSource(IncomeSource(name = "أرباح أسهم واستثمار", icon = "📈"))
            dao.insertIncomeSource(IncomeSource(name = "عائدات عقارية وإيجار", icon = "🏢"))
            dao.insertIncomeSource(IncomeSource(name = "قروض وتمويل بنكي", icon = "🏦"))
            dao.insertIncomeSource(IncomeSource(name = "هدايا ومكافآت وعيديات", icon = "🎁"))
            dao.insertIncomeSource(IncomeSource(name = "مصادر إيرادات أخرى", icon = "💎"))
        }
    }
}
