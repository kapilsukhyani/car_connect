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
import io.reactivex.functions.BiFunction
import redux.api.Store
import redux.asObservable

data class PersistedAppState(val knownDongles: Set<Dongle> = hashSetOf(),
                             val knownVehicles: Set<Vehicle> = hashSetOf(),
                             val lastConnectedDongle: Dongle? = null,
                             val lastConnectedVehicle: Vehicle? = null,
                             val appSettings: AppSettings = AppSettings())


class BaseStore(val context: Context, val ioScheduler: Scheduler) {
    companion object {
        const val TAG = "BaseStore"
    }

    class StateListener(val store: Store<AppState>,
                        val ioScheduler: Scheduler,
                        val baseAppStateDao: BaseAppStateDao) {

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


    private val loadAllDongles = baseAppStateDao
            .getAllDonglesAndComplete()
            .onErrorReturn { listOf() }
            .map {
                Pair(it.firstOrNull { it.recentlyUsed }?.toDongle(),
                        it.map { it.toDongle() }.toHashSet())
            }


    private val loadAllVehicles = baseAppStateDao
            .getAllVehiclesAndComplete()
            .onErrorReturn { listOf() }
            .map {
                Pair(it.firstOrNull { it.recentlyUsed }?.toVehicle(),
                        it.map { it.toVehicle() }.toHashSet())
            }


    private val loadAppSettings = baseAppStateDao
            .getAppSettingsAndComplete()
            .map { it.toAppSettings() }
            .onErrorReturn {
                AppSettings()
            }

    init {
        db.openHelper.writableDatabase
    }

    fun startListening(store: Store<AppState>) {
        StateListener(store, ioScheduler, baseAppStateDao)
    }

    internal fun loadAppState(): Single<PersistedAppState> {
        return loadAllDongles
                .zipWith(loadAllVehicles, BiFunction<Pair<Dongle?, HashSet<Dongle>>,
                        Pair<Vehicle?, HashSet<Vehicle>>, PersistedAppState> { t1, t2 ->
                    PersistedAppState(knownDongles = t1.second,
                            knownVehicles = t2.second,
                            lastConnectedDongle = t1.first,
                            lastConnectedVehicle = t2.first)
                })
                .zipWith(loadAppSettings, BiFunction<PersistedAppState, AppSettings, PersistedAppState> { t1, t2 ->
                    t1.copy(appSettings = t2)
                })
                .onErrorReturn {
                    Logger.log(TAG, "unexpected error while loading state", it)
                    PersistedAppState()
                }

    }

}