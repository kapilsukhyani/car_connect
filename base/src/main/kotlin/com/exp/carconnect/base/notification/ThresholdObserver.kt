package com.exp.carconnect.base.notification

import android.content.Context
import android.media.MediaPlayer
import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.R
import com.exp.carconnect.base.state.*
import io.reactivex.Observable
import io.reactivex.Scheduler

class ThresholdObserver(private val context: Context,
                        private val stateObservable: Observable<AppState>,
                        private val ioScheduler: Scheduler,
                        private val mainScheduler: Scheduler,
                        private val computationScheduler: Scheduler) {

    enum class ThresholdState {
        UNKNOWN,
        BELOW_THRESHOLD,
        ABOVE_THRESHOLD,
        MOVING_BELOW_THRESHOLD,
        MOVING_ABOVE_THRESHOLD
    }

    private val speedNotificationPlaybackPlayer = MediaPlayer.create(context, R.raw.speed_limit_notification_sev2)
    private val fuelNotificationPlaybackPlayer = MediaPlayer.create(context, R.raw.fuel_limit_notification)
    private val egineLightNotificationPlaybackPlayer = MediaPlayer.create(context, R.raw.engine_light_is_on_notification)

    private val vehicleDataAvailableObservable = stateObservable
            .observeOn(ioScheduler)
            .filter {
                it.isVehicleDataLoaded()
            }
            .distinctUntilChanged()

    private val speedLimitThresholdObservable = vehicleDataAvailableObservable
            .filter {
                it.isActiveVehicleSpeedLoaded() &&
                        it.isSpeedNotificationOn()
            }
            .map {
                Pair(it.getActiveVehicleSpeed(),
                        it.getMaxSpeedThresholdFromSettings())
            }
            .map { it.first.toInt() > it.second }
            .buffer(2, 1)
            .map {
                val thresholdState = if (it[0] == false && it[1] == false) {
                    ThresholdState.BELOW_THRESHOLD
                } else if (it[0] == false && it[1] == true) {
                    ThresholdState.MOVING_ABOVE_THRESHOLD
                } else if (it[0] == true && it[1] == true) {
                    ThresholdState.ABOVE_THRESHOLD
                } else {
                    ThresholdState.MOVING_BELOW_THRESHOLD
                }
                thresholdState
            }
            .distinctUntilChanged()
            .startWith(ThresholdState.UNKNOWN)
            .filter { it == ThresholdState.MOVING_ABOVE_THRESHOLD }


    private val fuelLimitThresholdObservable = vehicleDataAvailableObservable
            .filter {
                it.isActiveVehicleFuelLoaded() &&
                        it.isFuelNotificationOn()
            }
            .map {
                Pair(it.getActiveVehicleFuelLevel(),
                        it.getMinFuelThresholdFromSettings())
            }
            .map { it.first < it.second }
            .buffer(2, 1)
            .map {
                val thresholdState = if (it[0] == false && it[1] == false) {
                    ThresholdState.BELOW_THRESHOLD
                } else if (it[0] == false && it[1] == true) {
                    ThresholdState.MOVING_ABOVE_THRESHOLD
                } else if (it[0] == true && it[1] == true) {
                    ThresholdState.ABOVE_THRESHOLD
                } else {
                    ThresholdState.MOVING_BELOW_THRESHOLD
                }
                thresholdState
            }
            .distinctUntilChanged()
            .startWith(ThresholdState.UNKNOWN)
            .filter { it == ThresholdState.MOVING_ABOVE_THRESHOLD }


    private val milStatusObservable = vehicleDataAvailableObservable
            .filter {
                it.isActiveVehicleMilStatusLoaded()
            }
            .map {
                it.getMilStatus()
            }
            .map { it is MILStatus.On }
            .buffer(2, 1)
            .map {
                val thresholdState = if (it[0] == false && it[1] == false) {
                    ThresholdState.BELOW_THRESHOLD
                } else if (it[0] == false && it[1] == true) {
                    ThresholdState.MOVING_ABOVE_THRESHOLD
                } else if (it[0] == true && it[1] == true) {
                    ThresholdState.ABOVE_THRESHOLD
                } else {
                    ThresholdState.MOVING_BELOW_THRESHOLD
                }
                thresholdState
            }
            .distinctUntilChanged()
            .startWith(ThresholdState.UNKNOWN)
            .filter { it == ThresholdState.MOVING_ABOVE_THRESHOLD }


    init {
        speedLimitThresholdObservable.subscribe {
            speedNotificationPlaybackPlayer.seekTo(0)
            speedNotificationPlaybackPlayer.start()
        }

        fuelLimitThresholdObservable.subscribe {
            fuelNotificationPlaybackPlayer.seekTo(0)
            fuelNotificationPlaybackPlayer.start()
        }

        milStatusObservable.subscribe {
            egineLightNotificationPlaybackPlayer.seekTo(0)
            egineLightNotificationPlaybackPlayer.start()
        }

    }
}