package com.khata.app

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    foreignKeys = [ForeignKey(entity = Customer::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("customerId")]
)
data class Txn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val type: String,      // RECEIVED or GIVEN
    val amount: Double,
    val date: String,      // yyyy-MM-dd
    val time: String,      // HH:mm
    val note: String = ""
)

@Entity(tableName = "profile")
data class Profile(@PrimaryKey val id: Int, val name: String, val businessName: String, val phone: String, val address: String)

@Entity(tableName = "settings")
data class Settings(@PrimaryKey val id: Int, val theme: String)

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE") fun all(): Flow<List<Customer>>
    @Insert suspend fun insert(c: Customer): Long
    @Update suspend fun update(c: Customer)
    @Delete suspend fun delete(c: Customer)
}

@Dao
interface TxnDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, time DESC, id DESC") fun all(): Flow<List<Txn>>
    @Insert suspend fun insert(t: Txn)
    @Update suspend fun update(t: Txn)
    @Delete suspend fun delete(t: Txn)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1") fun get(): Flow<Profile?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(p: Profile)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1") fun get(): Flow<Settings?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(s: Settings)
}

@Database(entities = [Customer::class, Txn::class, Profile::class, Settings::class], version = 1, exportSchema = false)
abstract class KhataDb : RoomDatabase() {
    abstract fun customers(): CustomerDao
    abstract fun txns(): TxnDao
    abstract fun profile(): ProfileDao
    abstract fun settings(): SettingsDao

    companion object {
        @Volatile private var inst: KhataDb? = null
        fun get(c: Context): KhataDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, KhataDb::class.java, "khata.db").build().also { inst = it }
        }
    }
}
