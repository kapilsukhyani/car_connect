package com.exp.carconnect.base.store

import android.arch.persistence.room.*
import com.exp.carconnect.base.UnAvailableAvailableData
import com.exp.carconnect.base.state.Dongle
import com.exp.carconnect.base.state.Vehicle
import com.exp.carconnect.base.state.VehicleAttributes
import com.exp.carconnect.obdlib.obdmessage.FuelType
import io.reactivex.Flowable
import io.reactivex.Single

fun Dongle.toEntity(recentlyUsed: Boolean = true): DongleEntity {
    return DongleEntity(this.address, this.name, recentlyUsed)
}

fun Vehicle.toEntity(recentlyUsed: Boolean = true): VehicleEntity {
    var model = ""
    var make = ""
    var manufacturer = ""
    var modelYear = ""
    if (this.attributes is UnAvailableAvailableData.Available) {
        model = this.attributes.data.model
        make = this.attributes.data.make
        manufacturer = this.attributes.data.manufacturer
        modelYear = this.attributes.data.modelYear
    }

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
    }, recentlyUsed, make, model, manufacturer, modelYear)
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
                                     var recentlyUsed: Boolean,
                                     @ColumnInfo var make: String,
                                     @ColumnInfo var model: String,
                                     @ColumnInfo var manufacturer: String,
                                     @ColumnInfo var modelYear: String) {
    fun toVehicle(): Vehicle {
        val attributes = if (this.model.isEmpty() && this.make.isEmpty() && this.manufacturer.isEmpty() && this.modelYear.isEmpty()) {
            UnAvailableAvailableData.UnAvailable
        } else {
            UnAvailableAvailableData.Available(VehicleAttributes(this.make, this.model, this.manufacturer, this.modelYear))
        }
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
        }, attributes
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

    @Transaction
    open fun insertVehicleAndDongleAsRecentlyUser(vehicle: VehicleEntity, dongle: DongleEntity) {
        unsetRecentlyUsedVehicle()
        unsetRecentlyUsedDongle()
        insertDongle(dongle)
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