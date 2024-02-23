package com.froggocatto.distresscall

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mapbox.maps.plugin.annotation.annotations

class BackgroundService : Service() {

    private val CHANNEL_ID = "DistressCallApp"
    private val NOTIFICATION_ID = 12
    private lateinit var listener : ListenerRegistration

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            Toast.makeText(this, "Notification channel initialized", Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(this, "Error notif channel", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Distress Alert Monitoring")
            .setContentText("Keeping an eye to incidents...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
        Toast.makeText(this, "Foreground service started", Toast.LENGTH_SHORT).show()
    }

    fun generateNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        var builder: NotificationCompat.Builder = NotificationCompat.Builder(
            applicationContext, channel_ID
        )
            .setSmallIcon(R.drawable.distress_pin)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000,1000,1000,1000))
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)

        builder = builder.setContent(getRemoteView(title, message))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT
            >= Build.VERSION_CODES.O
        ) {
            val notificationChannel = NotificationChannel(
                channel_ID, channel_Name,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(
                notificationChannel
            )
        }

        notificationManager.notify(0, builder.build())
    }

    fun getRemoteView(title:String, message: String): RemoteViews {
        val remoteView = RemoteViews("com.froggocatto.distresscall", R.layout.notification_layout)
        remoteView.setTextViewText(R.id.notification_title, title)
        remoteView.setTextViewText(R.id.notification_message, message)
        return remoteView
    }

    fun getDistresses() {
        val db = FirebaseFirestore.getInstance()
        val colRef = db.collection("distresses");
        listener = colRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Handle errors
                return@addSnapshotListener
            }
            if (snapshot != null) {
                generateNotification("Alert!!!", "New messages")
            }
        }

    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        getDistresses()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        listener.remove()
    }
}