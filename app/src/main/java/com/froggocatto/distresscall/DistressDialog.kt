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
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.cardview.widget.CardView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class DistressDialog(private val c: Activity, private val context:Context) : Dialog(c), View.OnClickListener {
    lateinit var cardView: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.distress_call_dialog)
        cardView = findViewById<CardView>(R.id.distress_cardview)
        val layoutParams = cardView.layoutParams
        layoutParams.width = (getScreenWidth() * 0.8).toInt()
        layoutParams.height = (getScreenHeight() * 0.8).toInt()
        window?.setBackgroundDrawable( ColorDrawable(Color.TRANSPARENT))
        setListeners()
    }

    private fun setListeners() {
        findViewById<Button>(R.id.distress_fire_button).setOnClickListener {
            run {
                findViewById<Button>(R.id.distress_fire_button).isEnabled = false;
                reportDistress("Fire", R.drawable.fire_symbol)
            }
        }
        findViewById<Button>(R.id.distress_accident_button).setOnClickListener {
            run {
                findViewById<Button>(R.id.distress_accident_button).isEnabled = false;
                reportDistress("Accident", R.drawable.accident_symbol)
            }
        }
        findViewById<Button>(R.id.distress_earthquake_button).setOnClickListener {
            run {
                findViewById<Button>(R.id.distress_earthquake_button).isEnabled = false;
                reportDistress("Earthquake", R.drawable.earthquake)
            }
        }
        findViewById<Button>(R.id.distress_crime_button).setOnClickListener {
            run {
                findViewById<Button>(R.id.distress_crime_button).isEnabled = false;
                reportDistress("Crime", R.drawable.crime)
            }
        }
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {


        }
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

    override fun onClick(v: View) {

    }

    override fun show() {
        super.show()
    }

    private fun getCurrentLocation() : Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            val location: Location? =
                locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER)
            if (location != null) {
                return location
            } else {
                return null
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            return null
        }
    }



    private fun reportDistress(distress:String, @DrawableRes resourceId: Int) {
        val db = FirebaseFirestore.getInstance()
        val colRef = db.collection("distresses")
        val location = getCurrentLocation() ?: return;
        val data = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "distress" to distress,
            "timestamp" to FieldValue.serverTimestamp(),
        )

        colRef.document().set(data).addOnSuccessListener {
            run {
                Toast.makeText(context, "Distress Reported", Toast.LENGTH_SHORT).show()
                (context as MainActivity).addAnnotationToMap(longitude = location.longitude, latitude = location.latitude, resourceId)
//                (context as MainActivity).getDistresses()
                dismiss()
            }
        }
    }
}