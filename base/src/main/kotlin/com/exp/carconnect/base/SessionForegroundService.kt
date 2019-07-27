package com.exp.carconnect.base

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
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
        val notification = getNotification()
        startForeground(FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun getNotification(): Notification {
        val channelId =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel(CHANNEL_ID, "CarConnect")
                } else {
                    // If earlier version channel ID is not used
                    // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                    ""
                }
        val builder = NotificationCompat.Builder(this, channelId)
        builder.setContentTitle(getString(R.string.foreground_notification_title))
        builder.setContentText(getString(R.string.foreground_notification_description))
        builder.setSmallIcon(R.drawable.ic_background_connection)
        builder.setAutoCancel(false)
        val intent = Intent(this, SessionForegroundService::class.java)
        intent.putExtra(STOP_CONNECTION_REQUEST, true)
        builder.addAction(R.drawable.ic_background_connection,
                getString(R.string.stop),
                PendingIntent.getService(this,
                        101,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT))
        return builder.build()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String,
                                          channelName: String): String {
        val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
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