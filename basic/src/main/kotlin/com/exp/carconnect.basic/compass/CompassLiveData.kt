package com.exp.carconnect.basic.compass

import android.arch.lifecycle.LiveData
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.exp.carconnect.Logger


class CompassLiveData(context: Context) : LiveData<CompassEvent>(), SensorEventListener {
    companion object {
        private val TAG = "Compass"
        private val DATA_X = 0
        private val DATA_Y = 1
        private val DATA_Z = 2
        /**
         * Returned from onOrientationChanged when the device orientation cannot be determined
         * (typically when the device is in a close to flat position).
         *
         * @see .onOrientationChanged
         */
        val ORIENTATION_UNKNOWN = -1
    }

    internal inner class SensorEventListenerImpl : SensorEventListener {
        private var mOrientation = ORIENTATION_UNKNOWN

        override fun onSensorChanged(event: SensorEvent) {
            val values = event.values
            var orientation = ORIENTATION_UNKNOWN
            val x = -values[DATA_X]
            val y = -values[DATA_Y]
            val z = -values[DATA_Z]
            val magnitude = x * x + y * y
            // Don't trust the angle if the magnitude is small compared to the y value
            if (magnitude * 4 >= z * z) {
                val oneEightyOverPi = 57.29577957855f
                val angle = Math.atan2((-y).toDouble(), x.toDouble()).toFloat() * oneEightyOverPi
                orientation = 90 - Math.round(angle)
                // normalize to 0 - 359 range
                while (orientation >= 360) {
                    orientation -= 360
                }
                while (orientation < 0) {
                    orientation += 360
                }
            }

            if (orientation != mOrientation) {
                mOrientation = orientation
                onOrientationChanged(orientation)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

        }


    }


    private val sensorManager: SensorManager = context
            .getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gsensor: Sensor
    private val msensor: Sensor
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var azimuth = 0f
    private var currectAzimuth = 0f
    private val orientationCalculateListener = SensorEventListenerImpl()

    init {
        gsensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        msensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }


    override fun onActive() {
        super.onActive()
        sensorManager.registerListener(this, gsensor,
                SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, msensor,
                SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onInactive() {
        super.onInactive()
        sensorManager.unregisterListener(this)
    }


    private fun updateAzimuth() {
        value = CompassEvent(azimuth)
    }

    private fun onOrientationChanged(orientation: Int) {
        Logger.log(TAG, "On new orientation: " + orientation)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val alpha = 0.97f

        synchronized(this) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                orientationCalculateListener.onSensorChanged(event)

                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

                // gravity = event.values;

                // Log.e(TAG, Float.toString(gravity[0]));
            }

            if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                // geomagnetic = event.values;

                geomagnetic[0] = alpha * geomagnetic[0] + (1 - alpha) * event.values[0]
                geomagnetic[1] = alpha * geomagnetic[1] + (1 - alpha) * event.values[1]
                geomagnetic[2] = alpha * geomagnetic[2] + (1 - alpha) * event.values[2]
                // Log.e(TAG, Float.toString(event.values[0]));

            }

            val R = FloatArray(9)
            val I = FloatArray(9)
            val success = SensorManager.getRotationMatrix(R, I, gravity,
                    geomagnetic)
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)
                // Log.d(TAG, "currentAzimuth (rad): " + currentAzimuth);
                azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat() // orientation
                azimuth = (azimuth + 360) % 360
                // Log.d(TAG, "currentAzimuth (deg): " + currentAzimuth);
                updateAzimuth()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}


}

data class CompassEvent(val azimuth: Float)