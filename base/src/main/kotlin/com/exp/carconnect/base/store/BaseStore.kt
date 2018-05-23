package com.exp.carconnect.base.store

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import com.exp.carconnect.Logger
import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.LoadableState
import com.exp.carconnect.base.UnAvailableAvailableData
import com.exp.carconnect.base.state.*
import io.reactivex.Scheduler
import io.reactivex.Single
import redux.api.Store
import redux.asObservable

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

class BaseStore(val context: Context,
                val store: Store<AppState>,
                val ioScheduler: Scheduler) {
    companion object {
        const val TAG = "BaseStore"
    }

    private val db: CarConnectBaseDB = Room
            .databaseBuilder(context, CarConnectBaseDB::class.java, "CarConnectBaseDB")
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Logger.log(TAG, "DB created")
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    Logger.log(TAG, "DB opened")
                }
            })
            .build()

    private val baseAppStateDao = db.baseAppStateDao()

    private val activeSessionObservable = store.asObservable()
            .filter { it.isBaseStateLoaded() }
            .filter { it.getBaseAppState().activeSession is UnAvailableAvailableData.Available }
            .map { (it.getBaseAppState().activeSession as UnAvailableAvailableData.Available).data }

    private val recentlyUsedDongleObservable = activeSessionObservable
            .map { it.dongle }
            .distinctUntilChanged()
            .observeOn(ioScheduler)

    private val recentlyUsedVehicleObservable = activeSessionObservable
            .filter { it.vehicle is LoadableState.Loaded }
            .map { (it.vehicle as LoadableState.Loaded).savedState }
            .distinctUntilChanged()
            .observeOn(ioScheduler)

    init {
        db.openHelper.writableDatabase
        startListening()
    }

    private fun startListening() {
        recentlyUsedDongleObservable
                .subscribe {
                    Logger.log(TAG, "persisting new dongle")
                    baseAppStateDao.insertDongleAsRecenltyUsed(it.toEntity())
                }
        recentlyUsedVehicleObservable
                .subscribe {
                    Logger.log(TAG, "persisting new vehicle")
                    baseAppStateDao.insertVehicle(it.toEntity())
                }
    }

}