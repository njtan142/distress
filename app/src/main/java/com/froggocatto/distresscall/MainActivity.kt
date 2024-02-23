package com.froggocatto.distresscall

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.animation.Interpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.animation.PathInterpolatorCompat
import com.google.android.gms.maps.model.CameraPosition
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.messaging
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.common.MapboxOptions
import com.mapbox.common.location.Location
import com.mapbox.common.location.LocationObserver
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBoundsZoom
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.style.sources.addGeoJSONSourceFeatures
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.CameraAnimatorChangeListener
import com.mapbox.maps.plugin.animation.CameraAnimatorsFactory
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.viewport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    lateinit var permissionsManager: PermissionsManager
    private val REQUEST_ENABLE_LOCATION = 123;
    private val NOTIFICATION_PERMISSION_CODE = 256
    var symbolLayerIconFeatureList: List<Feature> = ArrayList()
    private var documents: List<DocumentSnapshot>? = null
    private var listener: ListenerRegistration? = null
    var zoom: Double = 6.0;
    private lateinit var fcmToken: String

    private val enableLocationLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (isLocationEnabled()) {
            goToLocation()
        } else {
            Toast.makeText(this, "Location is still not enabled.", Toast.LENGTH_SHORT).show()
        }
    }


    var permissionsListener: PermissionsListener = object : PermissionsListener {
        override fun onExplanationNeeded(permissionsToExplain: List<String>) {
            Toast.makeText(this@MainActivity, "explanation needed", Toast.LENGTH_SHORT).show()
        }

        override fun onPermissionResult(granted: Boolean) {
            if (granted) {
                goToLocation()
            } else {
            }
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
                startBackgroundService()
            } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            startBackgroundService()
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    fun sendFCMMessage(message: String) {
        Thread {
            try {
                // Construct the FCM message
                val message = mapOf(
                    "to" to fcmToken,
                    "title" to "Your Notification Title",
                    "body" to message
                )

                // Send the FCM message
                val response = FirebaseMessaging.getInstance().send(
                    RemoteMessage.Builder(fcmToken).setData(message).build()
                )

                // Log the message ID and response
                Log.d("FCM Message", "Successfully sent message: $response")
            } catch (e: Exception) {
                Log.e("FCM Message", "Error sending message", e)
            }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkLoggedIn()) {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
            return
        }
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM Token", "Token: $token")
                fcmToken = token
            } else {
                Log.e("FCM Token", "Error getting token", task.exception)
            }
        }
        setContentView(R.layout.activity_main)
        initializeMapBox()
        mapView.camera.addCameraZoomChangeListener(
            CameraAnimatorChangeListener {
                val scalingFactor = 2.0.pow(it - 6.0)
                zoom = 0.0005 * scalingFactor
                onZoomChanged(0.0005 * scalingFactor)
            }
        )
        setListeners()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.FOREGROUND_SERVICE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.FOREGROUND_SERVICE),
                NOTIFICATION_PERMISSION_CODE
            )
        } else {
            startBackgroundService()
        }
        getDistresses()
    }

    private fun onMarkerClick(id:String): Boolean {
        Toast.makeText(this, "Marker Clicked", Toast.LENGTH_SHORT).show()
        val dialog = IncidentDialog(this@MainActivity, this@MainActivity, id)
        dialog.show();
        return true
    }

    private fun checkLoggedIn(): Boolean {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            return true
        }
        return false
    }


    private fun startBackgroundService() {
        val serviceIntent = Intent(this, BackgroundService::class.java)

        // Check if BackgroundService is already running
        if (!isServiceRunning(BackgroundService::class.java)) {
            startService(serviceIntent)
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun initializeMapBox() {
        MapboxOptions.accessToken =
            "sk.eyJ1Ijoibmp0YW4xNDIiLCJhIjoiY2xzOXR4eWxkMDE1MjJxcGRnZDZ6bTJjNCJ9.k372nNxCVyFcEuLpdpWCoA"
        mapView = findViewById(R.id.mapView)
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(123.8, 10.0))
                .pitch(0.0)
                .zoom(6.0)
                .bearing(0.0)
                .build()
        )
        mapView.mapboxMap.loadStyle(Style.SATELLITE)
        permissionsManager = PermissionsManager(permissionsListener)
        askNotificationPermission()
    }

    private fun getLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
            if (isLocationEnabled()) {
                goToLocation()
            } else {
                showEnableLocationDialog();
            }
        } else {
            permissionsManager.requestLocationPermissions(this)
        }

    }

    private fun goToLocation() {
        with(mapView) {
            location.locationPuck = createDefault2DPuck(withBearing = true)
            location.enabled = true
            location.puckBearing = PuckBearing.HEADING
            val currentLocation = getCurrentLocation() ?: return
            val target = cameraOptions {
                center(Point.fromLngLat(currentLocation.longitude, currentLocation.latitude))
                zoom(16.0)
                pitch(0.0)
                bearing(0.0)
            }
            mapboxMap.flyTo(target, mapAnimationOptions {
                duration(3_000)
                interpolator(CUSTOM_INTERPOLATOR2)
            })
        }
    }

    val CUSTOM_INTERPOLATOR: Interpolator = PathInterpolatorCompat.create(
        1.0f,
        0.27f,
        0.68f,
        1.25f
    )
    val CUSTOM_INTERPOLATOR2: Interpolator = PathInterpolatorCompat.create(
        0.16f,
        -0.38f,
        0.66f,
        1.46f
    )

    private fun getCurrentLocation(): android.location.Location? {
        val locationManager =
            this@MainActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            val location: android.location.Location? =
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

    private fun setListeners() {
        findViewById<Button>(R.id.distress_call_button).setOnClickListener {
            run {
//                resetMarkers()
                val dialog = DistressDialog(this@MainActivity, this@MainActivity)
                dialog.show();

            }
        }
        findViewById<ImageButton>(R.id.my_location_button).setOnClickListener {
            run {
                getLocation()
            }
        }
        findViewById<ImageButton>(R.id.profile_button).setOnClickListener {
            run {
                val auth = FirebaseAuth.getInstance()
                auth.signOut()
                val intent = Intent(this, Login::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun showEnableLocationDialog() {
        Toast.makeText(
            this,
            "Location is not enabled. Please enable it in settings.",
            Toast.LENGTH_SHORT
        ).show()

        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        enableLocationLauncher.launch(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startBackgroundService()
        }
    }

    fun addAnnotationToMap(
        longitude: Double,
        latitude: Double,
        @DrawableRes resourceId: Int,
        markerID:String,
        scale: Double = 1.0,
        hasRange: Boolean = false,
        rangeRadius: Int = 1000,
    ) {
        bitmapFromDrawableRes(
            this@MainActivity,
            resourceId
        )?.let {
            val annotationApi = mapView?.annotations
            val pointAnnotationManager = annotationApi?.createPointAnnotationManager()
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withGeometry(Point.fromLngLat(longitude, latitude))
                .withIconImage(it)
                .withIconOpacity(0.8)
                .withIconSize(scale)
            pointAnnotationManager?.create(pointAnnotationOptions)
            pointAnnotationManager?.addClickListener(
                OnPointAnnotationClickListener {
                    onMarkerClick(markerID)
                }
            )

            if (hasRange) {

                val circleAnnotationManager = annotationApi?.createCircleAnnotationManager()
                val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
                    .withPoint(Point.fromLngLat(longitude, latitude))
                    .withCircleRadius(rangeRadius * zoom)
                    .withCircleColor("#ee0000")
                    .withCircleOpacity(0.2)
                    .withCircleStrokeWidth(2.0)
                    .withCircleStrokeColor("#ff0033")
                    .withCircleStrokeOpacity(0.5)
                circleAnnotationManager?.create(circleAnnotationOptions)
            }

        }
    }


    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    fun onZoomChanged(zoom: Double) {
        mapView?.annotations?.cleanup()
        if (documents != null) {
            val currentTime = Timestamp.now()
            for (documentSnapshot in documents!!) {
                val data = documentSnapshot.data!!
                val type = data["distress"].toString()
                var timestamp = 0L
                if (data["timestamp"] != null) {
                    timestamp = (data["timestamp"] as Timestamp).seconds
                }
                val secondsDifference = currentTime.seconds - timestamp
                val resourceId = when (type) {
                    "Fire" -> R.drawable.fire_symbol
                    "Crime" -> R.drawable.crime
                    "Accident" -> R.drawable.accident_symbol
                    "Earthquake" -> R.drawable.earthquake
                    else -> {
                        R.drawable.other_symbol
                    }
                }
                val scale = when (type) {
                    "Fire" -> 1.0
                    "Crime" -> 0.4
                    "Accident" -> 0.5
                    "Earthquake" -> 1.5
                    else -> {
                        0.01
                    }
                }
                val range = when (type) {
                    "Fire" -> 300
                    "Crime" -> 0
                    "Accident" -> 0
                    "Earthquake" -> 3000
                    else -> {
                        0
                    }
                }
                if (secondsDifference <= 2 * 60 * 60) {
                    addAnnotationToMap(
                        data["longitude"].toString().toDouble(),
                        data["latitude"].toString().toDouble(),
                        resourceId,
                        documentSnapshot.id,
                        zoom * scale,
                        range != 0,
                        rangeRadius = range
                    )
                }

            }
        }
    }


    fun getDistresses() {
        mapView?.annotations?.cleanup()
        val db = FirebaseFirestore.getInstance()
        val colRef = db.collection("distresses");
        val listener = colRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                return@addSnapshotListener
            }
            if (snapshot != null) {
                mapView?.annotations?.cleanup()
                documents = snapshot.documents
                onZoomChanged(zoom)
            }
        }

    }

}