package com.exp.carconnect.basic.compass

import android.arch.lifecycle.LiveData
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.ImageView


class LifeCycleAwareCompass : LiveData<CompassEvent>, SensorEventListener {
    companion object {
        private val TAG = "Compass"
    }

    private var sensorManager: SensorManager
    private var gsensor: Sensor
    private var msensor: Sensor
    private val mGravity = FloatArray(3)
    private val mGeomagnetic = FloatArray(3)
    private var azimuth = 0f
    private var currectAzimuth = 0f

    // compass arrow to rotate
    var arrowView: ImageView? = null

    constructor(context: Context) {
        sensorManager = context
                .getSystemService(Context.SENSOR_SERVICE) as SensorManager
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

    override fun onSensorChanged(event: SensorEvent) {
        val alpha = 0.97f

        synchronized(this) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {

                mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0]
                mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1]
                mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2]

                // mGravity = event.values;

                // Log.e(TAG, Float.toString(mGravity[0]));
            }

            if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                // mGeomagnetic = event.values;

                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * event.values[0]
                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * event.values[1]
                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * event.values[2]
                // Log.e(TAG, Float.toString(event.values[0]));

            }

            val R = FloatArray(9)
            val I = FloatArray(9)
            val success = SensorManager.getRotationMatrix(R, I, mGravity,
                    mGeomagnetic)
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