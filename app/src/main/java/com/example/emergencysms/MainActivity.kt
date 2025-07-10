package com.example.emergencysms

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.text.format.DateFormat
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var sensorManager: SensorManager
    private var lastShakeTime: Long = 0
    private val SHAKE_THRESHOLD = 12.0f

    private lateinit var txtTime: TextView
    private lateinit var txtDate: TextView
    private lateinit var txtEmergency: TextView
    private lateinit var btnSendSOS: View
    private lateinit var hiddenContactsArea: View

    // for Time/Date
    private val timeHandler = Handler(Looper.getMainLooper())
    private val timeUpdater = object : Runnable {
        override fun run() {
            updateTimeAndDate()
            timeHandler.postDelayed(this, 1000)
        }
    }

    // For gestures
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtTime = findViewById(R.id.txtTime)
        txtDate = findViewById(R.id.txtDate)
        txtEmergency = findViewById(R.id.txtEmergency)
        btnSendSOS = findViewById(R.id.btnSendSOS)
        hiddenContactsArea = findViewById(R.id.hiddenContactsArea)

        // Setting up time/date and permissions
        updateTimeAndDate()
        timeHandler.post(timeUpdater)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        checkPermissions()

        // Gesture detection
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 != null && e2.y - e1.y < -100) { // Swipe up
                    openContacts()
                    return true
                }
                return false
            }
        })

        // Long click to display the label "emergency", just a reminder for the user
        btnSendSOS.setOnLongClickListener {
            txtEmergency.visibility = View.VISIBLE
            Handler(Looper.getMainLooper()).postDelayed({
                txtEmergency.visibility = View.INVISIBLE
            }, 2000)
            true
        }

        btnSendSOS.setOnClickListener {
            sendSOS()
        }
    }

    private fun updateTimeAndDate() {
        val timeFormat = DateFormat.is24HourFormat(this)
        val timePattern = if (timeFormat) "HH:mm" else "h:mm a"
        val datePattern = "EEEE, MMMM d"

        val now = Calendar.getInstance().time
        txtTime.text = SimpleDateFormat(timePattern, Locale.getDefault()).format(now)
        txtDate.text = SimpleDateFormat(datePattern, Locale.getDefault()).format(now)
    }

    override fun onResume() {
        super.onResume()
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        timeHandler.post(timeUpdater)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorListener)
        timeHandler.removeCallbacks(timeUpdater)
    }

    // Handle touch events for swipe gestures to open the add contact stuff
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    // This one is for the shaking feature
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val values = event?.values ?: return
            val acceleration = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
            if (acceleration > SHAKE_THRESHOLD && System.currentTimeMillis() - lastShakeTime > 2000) {
                lastShakeTime = System.currentTimeMillis()
                txtEmergency.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed({
                    txtEmergency.visibility = View.INVISIBLE
                }, 2000)
                sendSOS()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
    }

    private fun sendSOS() {
        Toast.makeText(this, "Getting your location...", Toast.LENGTH_SHORT).show()

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { locationManager.isProviderEnabled(it) }

        if (providers.isEmpty()) {
            Toast.makeText(this, "No location providers available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // First, try to get last known location from all available providers
            var bestLocation: Location? = null
            for (provider in providers) {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null && (bestLocation == null ||
                            location.accuracy < bestLocation.accuracy)) {
                    bestLocation = location
                }
            }

            if (bestLocation != null && isLocationFresh(bestLocation)) {
                sendSMSToContacts(bestLocation.latitude, bestLocation.longitude)
            } else {
                // Then, we try to request fresh location with timeout
                val locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationManager.removeUpdates(this)
                        sendSMSToContacts(location.latitude, location.longitude)
                    }

                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    locationManager.removeUpdates(locationListener)
                    if (bestLocation != null) {
                        // Fallback to last known location if fresh one isn't available
                        sendSMSToContacts(bestLocation.latitude, bestLocation.longitude)
                    } else {
                        Toast.makeText(this,
                            "Couldn't get current location.",
                            Toast.LENGTH_SHORT).show()
                    }
                }, 15000)

                // This to check all the location providers
                for (provider in providers) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        locationManager.requestLocationUpdates(
                            provider,
                            0L,
                            0f,
                            locationListener
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isLocationFresh(location: Location): Boolean {
        return System.currentTimeMillis() - location.time < 5 * 60 * 1000
    }

    private fun sendSMSToContacts(lat: Double, lon: Double) {
        val locationLink = "https://maps.google.com/?q=$lat,$lon"
        val contacts = loadContactsFromFile(this)

        if (contacts.isEmpty()) {
            Toast.makeText(this, "No emergency contacts set", Toast.LENGTH_SHORT).show()
            openContacts()
            return
        }

        contacts.forEach { contact ->
            try {
                SmsManager.getDefault().sendTextMessage(
                    contact.phone, null,
                    "EMERGENCY! I need help. My location: $locationLink",
                    null, null
                )
            } catch (e: Exception) {
                Log.e("SMS", "Failed to send SMS to ${contact.phone}", e)
            }
        }

        Toast.makeText(this, "Emergency alert sent to ${contacts.size} contacts", Toast.LENGTH_SHORT).show()
    }

    private fun openContacts() {
        startActivity(Intent(this, ContactListActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        timeHandler.removeCallbacks(timeUpdater)
    }
}