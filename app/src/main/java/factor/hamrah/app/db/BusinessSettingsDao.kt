package factor.hamrah.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import factor.hamrah.app.model.BusinessSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessSettingsDao {

    @Query("SELECT * FROM business_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<BusinessSettings?>

    @Query("SELECT * FROM business_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): BusinessSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: BusinessSettings)
}
