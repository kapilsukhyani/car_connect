package com.exp.carconnect.base.store

import android.arch.persistence.room.*
import com.exp.carconnect.base.Frequency
import com.exp.carconnect.base.UnAvailableAvailableData
import com.exp.carconnect.base.state.*
import com.exp.carconnect.obdlib.obdmessage.FuelType
import io.reactivex.Flowable
import io.reactivex.Single
import java.util.concurrent.TimeUnit

fun Dongle.toEntity(recentlyUsed: Boolean = true): DongleEntity {
    return DongleEntity(this.address, this.name, recentlyUsed)
}

fun Vehicle.toEntity(recentlyUsed: Boolean = true): VehicleEntity {
    return VehicleEntity(this.vin, this.fuelType.let {
        if (it is UnAvailableAvailableData.Available) {
            it.data.value
        } else {
            -1
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

fun String.toTimeUnit(): TimeUnit {
    return when {
        this == TimeUnit.DAYS.name -> TimeUnit.DAYS
        this == TimeUnit.HOURS.name -> TimeUnit.HOURS
        this == TimeUnit.MINUTES.name -> TimeUnit.MINUTES
        this == TimeUnit.SECONDS.name -> TimeUnit.SECONDS
        this == TimeUnit.MILLISECONDS.name -> TimeUnit.MILLISECONDS
        this == TimeUnit.MICROSECONDS.name -> TimeUnit.MICROSECONDS
        else -> TimeUnit.NANOSECONDS
    }
}

fun String.toFrequency(): Frequency {
    val args = this.split("-")
    return Frequency(args[0].toLong(), args[1].toTimeUnit())
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

fun String.toFuelNotificationSettings(): FuelNotificationSettings {
    return if (this == "OFF") {
        FuelNotificationSettings.Off
    } else {
        val args = this.split("-")
        FuelNotificationSettings.On(args[1].toFloat())
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

fun String.toSpeedNotificationSettings(): SpeedNotificationSettings {
    return if (this == "OFF") {
        SpeedNotificationSettings.Off
    } else {
        val args = this.split("-")
        SpeedNotificationSettings.On(args[1].toInt())
    }
}

fun String.toUnitSystem(): UnitSystem {
    return if (this == UnitSystem.Imperial.name) {
        UnitSystem.Imperial
    } else {
        UnitSystem.Matrix
    }
}

fun String.toDashboardTheme(): DashboardTheme {
    return if (this == DashboardTheme.Dark.name) {
        DashboardTheme.Dark
    } else {
        DashboardTheme.Light
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
                        val recentlyUsed: Boolean) {

    fun toDongle(): Dongle {
        return Dongle(address, name)
    }
}


@Entity
data class VehicleEntity constructor(@PrimaryKey var vin: String,
                                     @ColumnInfo var fuelType: Int,
                                     @ColumnInfo var supportedPIDs: String,
                                     var recentlyUsed: Boolean) {
    fun toVehicle(): Vehicle {
        return Vehicle(vin, if (supportedPIDs.isEmpty()) {
            UnAvailableAvailableData.UnAvailable
        } else {
            UnAvailableAvailableData.Available(supportedPIDs
                    .split(",")
                    .toSet())
        }, if (fuelType == -1) {
            UnAvailableAvailableData.UnAvailable
        } else {
            UnAvailableAvailableData.Available(FuelType.fromValue(fuelType))
        }
        )
    }
}

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
                            @PrimaryKey val id: String = "1") {
    fun toAppSettings(): AppSettings {
        return AppSettings(dataSettings = DataSettings(unitSystem = unitSystem.toUnitSystem(),
                fastChangingDataRefreshFrequency = fastChangingDataRefreshFrequency.toFrequency(),
                fuelLevelRefreshFrequency = fuelLevelRefreshFrequency.toFrequency(),
                pressureRefreshFrequency = pressureRefreshFrequency.toFrequency(),
                temperatureRefreshFrequency = temperatureRefreshFrequency.toFrequency()),
                notificationSettings = NotificationSettings(
                        fuelNotificationSettings = fuelNotificationSettings.toFuelNotificationSettings(),
                        speedNotificationSettings = speedNotificationSettings.toSpeedNotificationSettings()),
                displaySettings = DisplaySettings(dashboardTheme.toDashboardTheme()),
                autoConnectToLastConnectedDongleOnLaunch = autoConnectToLastConnectedDongleOnLaunch,
                backgroundConnectionEnabled = backgroundConnectionEnabled)
    }
}


@Dao
abstract class BaseAppStateDao {

    @Query("SELECT * FROM DongleEntity")
    abstract fun getAllDongles(): Flowable<List<DongleEntity>>

    @Query("SELECT * FROM DongleEntity")
    abstract fun getAllDonglesAndComplete(): Single<List<DongleEntity>>

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
    abstract fun getAllVehiclesAndComplete(): Single<List<VehicleEntity>>

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
    abstract fun getAppSettingsAndComplete(): Single<AppSettingEntity>

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