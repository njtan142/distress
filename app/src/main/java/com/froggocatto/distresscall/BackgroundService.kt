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
import com.mapbox.maps.plugin.annotation.annotations

class BackgroundService : Service() {

    private val CHANNEL_ID = "DistressCallApp"
    private val NOTIFICATION_ID = 12

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
    }

    private fun createNotificationChannel() {
        // Create a notification channel for Android Oreo and higher
        // Notification channels are required for foreground services
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
        getDistresses()
    }

//    private fun showNotification(title: String, content: String) {
//        val notificationIntent = Intent(this, MainActivity::class.java)
//        val pendingIntent = PendingIntent.getActivity(
//            this,
//            0,
//            notificationIntent,
//            0
//        )
//
//        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle(title)
//            .setContentText(content)
//            .setSmallIcon(R.mipmap.ic_launcher)
//            .setContentIntent(pendingIntent)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//            .build()
//
//        with(NotificationManagerCompat.from(this)) {
////            notify(NOTIFICATION_ID, notification)
//        }
//    }

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
//        colRef.get().addOnCompleteListener { task ->
//            run {
//                val snapshot = task.result
//                documents = snapshot.documents
//                for (documentSnapshot in documents!!) {
//                    val data = documentSnapshot.data!!
//                    val type = data["distress"].toString()
//                    val resourceId = when (type) {
//                        "Fire" -> R.drawable.fire_symbol
//                        "Crime" -> R.drawable.crime
//                        "Accident" -> R.drawable.accident_symbol
//                        "Earthquake" -> R.drawable.earthquake
//                        else -> {
//                            R.drawable.other_symbol
//                        }
//                    }
//
//                    addAnnotationToMap(
//                        data["longitude"].toString().toDouble(),
//                        data["latitude"].toString().toDouble(),
//                        resourceId
//                    )
//                }
//            }
//        }
        val listener = colRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Handle errors
                return@addSnapshotListener
            }

            if (snapshot != null) {
                generateNotification("Alert!!!", "New messages")
                // Clear existing annotations
                // Process new data
//                for (documentSnapshot in snapshot.documents) {
//                    val data = documentSnapshot.data!!
//                    val type = data["distress"].toString()
//                    val resourceId = when (type) {
//                        "Fire" -> R.drawable.fire_symbol
//                        "Crime" -> R.drawable.crime
//                        "Accident" -> R.drawable.accident_symbol
//                        "Earthquake" -> R.drawable.earthquake
//                        else -> R.drawable.other_symbol
//                    }
//
//                    addAnnotationToMap(
//                        data["longitude"].toString().toDouble(),
//                        data["latitude"].toString().toDouble(),
//                        resourceId
//                    )
//                }
            }
        }

    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: Start your background tasks or services here.
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO: Perform any cleanup or finalization here.
    }
}