package com.froggocatto.distresscall

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import androidx.annotation.DrawableRes
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
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


    override fun show() {
        super.show()
    }

    private fun getCurrentLocation() : Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER)
            } else {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
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
            "reporter" to FirebaseAuth.getInstance().currentUser!!.uid
        )
        (context as MainActivity).sendFCMMessage(distress)

        colRef.document().set(data).addOnSuccessListener {
            run {
//                Toast.makeText(context, "Distress Reported", Toast.LENGTH_SHORT).show()
//                (context as MainActivity).addAnnotationToMap(longitude = location.longitude, latitude = location.latitude, resourceId)
                (context as MainActivity).getDistresses()
                dismiss()
            }
        }
    }

    override fun onClick(v: View?) {
    }
}