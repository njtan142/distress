package com.froggocatto.distresscall

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.cardview.widget.CardView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class IncidentDialog(
    private val c: Activity,
    private val context: Context,
    private val id: String
) : Dialog(c), View.OnClickListener {
    lateinit var cardView: CardView
    lateinit var imageView: ImageView
    lateinit var distressView: TextView
    lateinit var nameView: TextView
    lateinit var contactView: TextView
    lateinit var locationView: TextView
    lateinit var timeView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.incident_dialog)
        getViews()
        expandDialog()
        getData()
    }

    private fun getViews() {
        cardView = findViewById(R.id.incident_cardview)
        imageView = findViewById(R.id.incident_imageview)
        distressView = findViewById(R.id.incident_distress_name)
        nameView = findViewById(R.id.incident_reporter_name)
        contactView = findViewById(R.id.incident_reporter_contact)
        locationView = findViewById(R.id.incident_marker_location)
        timeView = findViewById(R.id.incident_time_passed)
    }

    private fun expandDialog() {
        val layoutParams = cardView.layoutParams
        layoutParams.width = (getScreenWidth() * 0.8).toInt()
        layoutParams.height = (getScreenHeight() * 0.8).toInt()
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun getData() {
        val db = FirebaseFirestore.getInstance()
        db.collection("distresses").document(id).get()
            .addOnSuccessListener {
                run {
                    val data = it.data!!
                    val userID = data["reporter"].toString()
//                    Toast.makeText(context, userID, Toast.LENGTH_SHORT).show()
                    db.collection("users").document(userID).get()
                        .addOnSuccessListener {
                            run {
                                val user = it.data
                                if(user == null){
//                                    Toast.makeText(context, "User is null", Toast.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }
                                setViews(data, user)
                            }
                        }
                }
            }
    }

    private fun setViews(distressData: Map<String, Any>, userData: Map<String, Any>) {
        val distress = distressData["distress"].toString()
        val name = userData["name"].toString()
        val contact = userData["phone"].toString()
        val timestamp = distressData["timestamp"] as Timestamp
        val longitude = (distressData["longitude"] as Double)
        val latitude = (distressData["latitude"] as Double)

        distressView.text = distress
        nameView.text = name
        contactView.text = contact
        timeView.text = getTimePassed(timestamp)
        locationView.text = getFormattedLocation(latitude, longitude)

        val resourceId = when (distress) {
            "Fire" -> R.drawable.fire_symbol
            "Crime" -> R.drawable.crime
            "Accident" -> R.drawable.accident_symbol
            "Earthquake" -> R.drawable.earthquake
            else -> {
                R.drawable.other_symbol
            }
        }

        imageView.setImageResource(resourceId)
    }

    private fun getTimePassed(timestamp: Timestamp): String {
        val currentTime = Timestamp.now()
        val elapsedTimeInSeconds = currentTime.seconds - timestamp.seconds

        return when {
            elapsedTimeInSeconds >= 3600 -> {
                val hours = elapsedTimeInSeconds / 3600
                "$hours hours ago"
            }
            elapsedTimeInSeconds >= 60 -> {
                val minutes = elapsedTimeInSeconds / 60
                "$minutes minutes ago"
            }
            else -> "$elapsedTimeInSeconds seconds ago"
        }
    }

    private fun getFormattedLocation(latitude: Double, longitude: Double): String {
        val latDegrees = latitude.toInt()
        val latMinutes = ((latitude - latDegrees) * 60).toInt()
        val latSeconds = ((latitude - latDegrees - latMinutes / 60.0) * 3600).toInt()

        val lonDegrees = longitude.toInt()
        val lonMinutes = ((longitude - lonDegrees) * 60).toInt()
        val lonSeconds = ((longitude - lonDegrees - lonMinutes / 60.0) * 3600).toInt()

        return "$latDegrees°$latMinutes'$latSeconds\"N $lonDegrees°$lonMinutes'$lonSeconds\"E"
    }


    private fun getScreenWidth(): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    private fun getScreenHeight(): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    override fun onClick(v: View?) {
        TODO("Not yet implemented")
    }


}