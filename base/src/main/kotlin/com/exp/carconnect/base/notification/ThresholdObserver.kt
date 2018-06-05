package com.exp.carconnect.base.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
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

    companion object {
        const val DEFAULT_LEFT_SPEAKER_SOUND_LEVEL = 0.6f
        const val DEFAULT_RIGHT_SPEAKER_SOUND_LEVEL = 0.6f
        const val SPEED_NOTIFICATION_PRIORITY = 1
        const val FUEL_NOTIFICATION_PRIORITY = 2
        const val ENGINE_LIGHT_NOTIFICATION_PRIORITY = 3
    }

    enum class ThresholdState {
        UNKNOWN,
        BELOW_THRESHOLD,
        ABOVE_THRESHOLD,
        MOVING_BELOW_THRESHOLD,
        MOVING_ABOVE_THRESHOLD
    }

    private val notificationSoundPool: SoundPool
    private var currentStreamId = 0

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
        val attributes = AudioAttributes
                .Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .build()

        notificationSoundPool = SoundPool
                .Builder()
                .setMaxStreams(3)
                .setAudioAttributes(attributes).build()
        val speedNotificationSoundId = notificationSoundPool.load(context, R.raw.speed_limit_notification_sev2, 1)
        val fuelNotificationSoundId = notificationSoundPool.load(context, R.raw.fuel_limit_notification, 2)
        val engineLightNotificationSoundId = notificationSoundPool.load(context, R.raw.engine_light_is_on_notification, 3)

        speedLimitThresholdObservable.subscribe {
            playNotification(speedNotificationSoundId, SPEED_NOTIFICATION_PRIORITY)
        }

        fuelLimitThresholdObservable.subscribe {
            playNotification(fuelNotificationSoundId, FUEL_NOTIFICATION_PRIORITY)
        }

        milStatusObservable.subscribe {
            playNotification(engineLightNotificationSoundId, ENGINE_LIGHT_NOTIFICATION_PRIORITY)
        }

    }


    private fun playNotification(soundID: Int, priority: Int) {
        notificationSoundPool.stop(currentStreamId)
        currentStreamId = notificationSoundPool.play(soundID, DEFAULT_LEFT_SPEAKER_SOUND_LEVEL, DEFAULT_RIGHT_SPEAKER_SOUND_LEVEL, priority, 0, 1f)
    }
}