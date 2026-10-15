package factor.hamrah.app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import factor.hamrah.app.model.BankAccount
import factor.hamrah.app.model.BusinessSettings
import factor.hamrah.app.model.Customer
import factor.hamrah.app.model.Invoice
import factor.hamrah.app.model.InvoiceItem

@Database(
    entities = [
        Customer::class,
        Invoice::class,
        InvoiceItem::class,
        BusinessSettings::class,
        BankAccount::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun businessSettingsDao(): BusinessSettingsDao
    abstract fun bankAccountDao(): BankAccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "factorino_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
