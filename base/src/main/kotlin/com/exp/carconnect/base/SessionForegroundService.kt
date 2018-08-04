package com.exp.carconnect.base

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import com.crashlytics.android.answers.CustomEvent

class SessionForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder {
        return LocalBinder()
    }

    class LocalBinder : Binder()


    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 102201
        const val CHANNEL_ID = "101"
        const val STOP_CONNECTION_REQUEST = "STOP_CONNECTION_REQUEST"
    }

    override fun onCreate() {
        super.onCreate()
        application.logContentViewEvent("BackgroundSessionNotification")
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
        builder.mContentTitle = getString(R.string.foreground_notification_title)
        builder.mContentText = getString(R.string.foreground_notification_description)
        builder.setSmallIcon(R.drawable.ic_background_connection)
        builder.setAutoCancel(false)
        val intent = Intent(this, SessionForegroundService::class.java)
        intent.putExtra(STOP_CONNECTION_REQUEST, true)
        builder.addAction(R.drawable.ic_background_connection, getString(R.string.stop), PendingIntent.getService(this, 101, intent, PendingIntent.FLAG_UPDATE_CURRENT))
        val notification = builder.build()
        startForeground(FOREGROUND_NOTIFICATION_ID, notification)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra(STOP_CONNECTION_REQUEST, false) == true) {
            stopForeground(true)
            application.killSession()
            stopSelf()
            application.logEvent(CustomEvent("stop_background_connection_clicked"))
        }
        return START_NOT_STICKY
    }
}