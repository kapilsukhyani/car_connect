package com.exp.carconnect.base.store

import android.arch.persistence.room.*
import com.exp.carconnect.base.UnAvailableAvailableData
import com.exp.carconnect.base.state.Dongle
import com.exp.carconnect.base.state.Vehicle
import com.exp.carconnect.obdlib.obdmessage.FuelType
import io.reactivex.Flowable
import io.reactivex.Single

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
    open fun insertDongleAsRecentlyUsed(dongle: DongleEntity) {
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
    open fun insertVehicleAsRecentlyUsed(vehicle: VehicleEntity) {
        unsetRecentlyUsedVehicle()
        insertVehicle(vehicle)
    }

    @Query("DELETE FROM VehicleEntity")
    abstract fun deleteAllVehicles()

    @Delete
    abstract fun deleteVehicle(dongle: VehicleEntity)

}

@Database(entities = [DongleEntity::class,
    VehicleEntity::class],
        version = 1)
abstract class CarConnectBaseDB : RoomDatabase() {

    abstract fun baseAppStateDao(): BaseAppStateDao

}