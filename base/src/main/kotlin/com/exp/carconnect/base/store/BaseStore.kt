package com.exp.carconnect.base.store

import android.arch.persistence.room.*
import com.exp.carconnect.base.Frequency
import com.exp.carconnect.base.UnAvailableAvailableData
import com.exp.carconnect.base.state.*
import com.exp.carconnect.obdlib.obdmessage.FuelType
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

data class PersistedAppState(val knownDongles: Set<Dongle> = hashSetOf(),
                             val knownVehicles: Set<Vehicle> = hashSetOf(),
                             val lastConnectedDongle: Dongle? = null,
                             val lastConnectedVehicle: Vehicle? = null,
                             val appSettings: AppSettings = AppSettings())

internal fun loadAppState(): Single<PersistedAppState> {

    return Single.fromCallable {
        //todo implement this
        Thread.sleep(3000)
        PersistedAppState()
    }

}

fun Dongle.toEntity(recentlyUsed: Boolean = true): DongleEntity {
    return DongleEntity(this.address, this.name, recentlyUsed)
}

fun Vehicle.toEntity(recentlyUsed: Boolean = true): VehicleEntity {
    return VehicleEntity(this.vin, this.fuelType.let {
        if (it is UnAvailableAvailableData.Available) {
            it.data
        } else {
            null
        }
    }, this.supportedPIDs.let {
        if (it is UnAvailableAvailableData.Available) {
            it.data
        } else {
            null
        }
    }, recentlyUsed)
}

fun AppSettings.toEntity(): AppSettingEntity {
    return AppSettingEntity(this.autoConnectToLastConnectedDongleOnLaunch,
            this.backgroundConnectionEnabled,
            this.dataSettings.fastChangingDataRefreshFrequency,
            this.dataSettings.fuelLevelRefreshFrequency,
            this.dataSettings.pressureRefreshFrequency,
            this.dataSettings.temperatureRefreshFrequency,
            this.dataSettings.unitSystem,
            this.displaySettings.dashboardTheme,
            this.notificationSettings.fuelNotificationSettings,
            this.notificationSettings.speedNotificationSettings)
}

@Entity
data class DongleEntity(@PrimaryKey val address: String,
                        @ColumnInfo val name: String,
                        val recentlyUsed: Boolean)

@Entity
data class VehicleEntity(@PrimaryKey val vin: String,
                         @ColumnInfo val fuelType: FuelType?,
                         @ColumnInfo val supportedPIDs: Set<String>?,
                         val recentlyUsed: Boolean)

@Entity
data class AppSettingEntity(@ColumnInfo val autoConnectToLastConnectedDongleOnLaunch: Boolean,
                            @ColumnInfo val backgroundConnectionEnabled: Boolean,

                            @Embedded(prefix = "fastChangingDataRefreshFrequency") @ColumnInfo val fastChangingDataRefreshFrequency: Frequency,
                            @Embedded(prefix = "fuelLevelRefreshFrequency") @ColumnInfo val fuelLevelRefreshFrequency: Frequency,
                            @Embedded(prefix = "pressureRefreshFrequency") @ColumnInfo val pressureRefreshFrequency: Frequency,
                            @Embedded(prefix = "temperatureRefreshFrequency") @ColumnInfo val temperatureRefreshFrequency: Frequency,
                            @Embedded @ColumnInfo val unitSystem: UnitSystem,
                            @ColumnInfo val dashboardTheme: DashboardTheme,
                            @Embedded(prefix = "fuelNotificationSettings") @ColumnInfo val fuelNotificationSettings: FuelNotificationSettings,
                            @Embedded(prefix = "speedNotificationSettings") @ColumnInfo val speedNotificationSettings: SpeedNotificationSettings,
                            @PrimaryKey val id: String = "1")


@Dao
interface BaseAppStateDao {

    @Query("SELECT * FROM DongleEntity")
    fun getAllDongles(): Flowable<List<DongleEntity>>

    @Query("SELECT * FROM DongleEntity")
    fun getAllDonglesAndComplete(): Maybe<List<DongleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDongle(dongle: DongleEntity)

    @Query("UPDATE DongleEntity SET recentlyUsed='false' WHERE recentlyUsed='true'")
    fun unsetRecentlyUsedDongle()

    @Transaction
    fun insertDongleAsRecenltyUsed(dongle: DongleEntity) {
        unsetRecentlyUsedDongle()
        insertDongle(dongle)
    }

    @Query("DELETE FROM DongleEntity")
    fun deleteAllDongles()

    @Delete
    fun deleteDongle(dongle: DongleEntity)

    @Query("SELECT * FROM VehicleEntity")
    fun getAllVehicles(): Flowable<List<VehicleEntity>>

    @Query("SELECT * FROM VehicleEntity")
    fun getAllVehiclesAndComplete(): Maybe<List<VehicleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVehicle(dongle: VehicleEntity)

    @Query("UPDATE VehicleEntity SET recentlyUsed='false' WHERE recentlyUsed='true'")
    fun unsetRecentlyUsedVehicle()

    @Transaction
    fun insertVehicleAsRecenltyUsed(vehicle: VehicleEntity) {
        unsetRecentlyUsedVehicle()
        insertVehicle(vehicle)
    }

    @Query("DELETE FROM VehicleEntity")
    fun deleteAllVehicles()

    @Delete
    fun deleteVechile(dongle: VehicleEntity)

    @Query("SELECT * FROM AppSettingEntity WHERE id='1' LIMIT 1")
    fun getAppSettings(): Flowable<AppSettingEntity>

    @Query("SELECT * FROM AppSettingEntity WHERE id='1' LIMIT 1")
    fun getAppSettingsAndComplete(): Maybe<AppSettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateAppSettings(settings: AppSettingEntity)

}

@Database(entities = [DongleEntity::class,
    VehicleEntity::class,
    AppSettingEntity::class],
        version = 1)
abstract class CarConnectBaseDB : RoomDatabase() {

    abstract fun baseAppStateDao(): BaseAppStateDao

}