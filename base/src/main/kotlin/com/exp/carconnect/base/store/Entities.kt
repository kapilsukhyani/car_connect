package com.exp.carconnect.base.store

import android.arch.persistence.room.*
import com.exp.carconnect.base.Frequency
import com.exp.carconnect.base.UnAvailableAvailableData
import com.exp.carconnect.base.state.*
import io.reactivex.Flowable
import io.reactivex.Maybe

fun Dongle.toEntity(recentlyUsed: Boolean = true): DongleEntity {
    return DongleEntity(this.address, this.name, recentlyUsed)
}

fun Vehicle.toEntity(recentlyUsed: Boolean = true): VehicleEntity {
    return VehicleEntity(this.vin, this.fuelType.let {
        if (it is UnAvailableAvailableData.Available) {
            it.data.name
        } else {
            ""
        }
    }, this.supportedPIDs.let {
        if (it is UnAvailableAvailableData.Available) {
            it.data.toString()
        } else {
            ""
        }
    }, recentlyUsed)
}

fun Frequency.serialize(): String {
    return "${this.frequency}-${this.unit.name}"
}

fun FuelNotificationSettings.serialize(): String {
    return when (this) {
        is FuelNotificationSettings.On -> {
            "ON-${this.minFuelPercentageThreshold}"
        }
        is FuelNotificationSettings.Off -> {
            "OFF"
        }
    }
}

fun SpeedNotificationSettings.serialize(): String {
    return when (this) {
        is SpeedNotificationSettings.On -> {
            "ON-${this.maxSpeedThreshold}"
        }
        is SpeedNotificationSettings.Off -> {
            "OFF"
        }
    }
}

fun AppSettings.toEntity(): AppSettingEntity {
    return AppSettingEntity(this.autoConnectToLastConnectedDongleOnLaunch,
            this.backgroundConnectionEnabled,
            this.dataSettings.fastChangingDataRefreshFrequency.serialize(),
            this.dataSettings.fuelLevelRefreshFrequency.serialize(),
            this.dataSettings.pressureRefreshFrequency.serialize(),
            this.dataSettings.temperatureRefreshFrequency.serialize(),
            this.dataSettings.unitSystem.name,
            this.displaySettings.dashboardTheme.name,
            this.notificationSettings.fuelNotificationSettings.serialize(),
            this.notificationSettings.speedNotificationSettings.serialize())
}

@Entity
data class DongleEntity(@PrimaryKey val address: String,
                        @ColumnInfo val name: String,
                        val recentlyUsed: Boolean)

@Entity
data class VehicleEntity constructor(@PrimaryKey var vin: String,
                                     @ColumnInfo var fuelType: String,
                                     @ColumnInfo var supportedPIDs: String,
                                     var recentlyUsed: Boolean)

@Entity
data class AppSettingEntity(@ColumnInfo val autoConnectToLastConnectedDongleOnLaunch: Boolean,
                            @ColumnInfo val backgroundConnectionEnabled: Boolean,

                            @ColumnInfo val fastChangingDataRefreshFrequency: String,
                            @ColumnInfo val fuelLevelRefreshFrequency: String,
                            @ColumnInfo val pressureRefreshFrequency: String,
                            @ColumnInfo val temperatureRefreshFrequency: String,
                            @ColumnInfo val unitSystem: String,
                            @ColumnInfo val dashboardTheme: String,
                            @ColumnInfo val fuelNotificationSettings: String,
                            @ColumnInfo val speedNotificationSettings: String,
                            @PrimaryKey val id: String = "1")


@Dao
abstract class BaseAppStateDao {

    @Query("SELECT * FROM DongleEntity")
    abstract fun getAllDongles(): Flowable<List<DongleEntity>>

    @Query("SELECT * FROM DongleEntity")
    abstract fun getAllDonglesAndComplete(): Maybe<List<DongleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertDongle(dongle: DongleEntity)

    @Query("UPDATE DongleEntity SET recentlyUsed='false' WHERE recentlyUsed='true'")
    abstract fun unsetRecentlyUsedDongle()

    @Transaction
    open fun insertDongleAsRecenltyUsed(dongle: DongleEntity) {
        unsetRecentlyUsedDongle()
        insertDongle(dongle)
    }

    @Query("DELETE FROM DongleEntity")
    abstract fun deleteAllDongles()

    @Delete
    abstract fun deleteDongle(dongle: DongleEntity)

    @Query("SELECT * FROM VehicleEntity")
    abstract fun getAllVehicles(): Flowable<List<VehicleEntity>>

    @Query("SELECT * FROM VehicleEntity")
    abstract fun getAllVehiclesAndComplete(): Maybe<List<VehicleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertVehicle(dongle: VehicleEntity)

    @Query("UPDATE VehicleEntity SET recentlyUsed='false' WHERE recentlyUsed='true'")
    abstract fun unsetRecentlyUsedVehicle()

    @Transaction
    open fun insertVehicleAsRecenltyUsed(vehicle: VehicleEntity) {
        unsetRecentlyUsedVehicle()
        insertVehicle(vehicle)
    }

    @Query("DELETE FROM VehicleEntity")
    abstract fun deleteAllVehicles()

    @Delete
    abstract fun deleteVechile(dongle: VehicleEntity)

    @Query("SELECT * FROM AppSettingEntity WHERE id='1' LIMIT 1")
    abstract fun getAppSettings(): Flowable<AppSettingEntity>

    @Query("SELECT * FROM AppSettingEntity WHERE id='1' LIMIT 1")
    abstract fun getAppSettingsAndComplete(): Maybe<AppSettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun updateAppSettings(settings: AppSettingEntity)

}

@Database(entities = [DongleEntity::class,
    VehicleEntity::class,
    AppSettingEntity::class],
        version = 1)
abstract class CarConnectBaseDB : RoomDatabase() {

    abstract fun baseAppStateDao(): BaseAppStateDao

}