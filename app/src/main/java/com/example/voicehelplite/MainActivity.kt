package com.example.voicehelplite

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech

    // ---------------- PERMISSIONS ----------------
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (perms.all { it.value }) {
                startSpeechRecognition()
            } else {
                Toast.makeText(this, "Permissions required", Toast.LENGTH_SHORT).show()
            }
        }

    // ---------------- SPEECH ----------------
    private val speechLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val text =
                    result.data
                        ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        ?.get(0)
                        ?.lowercase()

                if (text != null) detectKeywords(text)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        // Demo defaults
        getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
            .putString("home_address", "21 Green Park, near City Hospital, New Delhi")
            .putString("emergency_contact", "9999999999")
            .apply()

        setContent {
            VoiceHelpUI { checkPermissions() }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    private fun checkPermissions() {
        val needed = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (needed.all {
                ContextCompat.checkSelfPermission(this, it) ==
                        PackageManager.PERMISSION_GRANTED
            }) {
            startSpeechRecognition()
        } else {
            permissionLauncher.launch(needed)
        }
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechLauncher.launch(intent)
    }

    // ---------------- CORE LOGIC ----------------
    private fun detectKeywords(text: String) {
        when {

            // 🗺️ DIRECTION TO HOME (CHECK FIRST)
            text.contains("direction") ||
                    text.contains("navigate") ||
                    text.contains("way home") -> {
                openDirectionsToHome()
            }

            // 🚨 EMERGENCY
            text.contains("help") ||
                    text.contains("emergency") ||
                    text.contains("lost") -> {
                handleEmergency()
            }

            // 🏠 SPEAK HOME ADDRESS
            text.contains("home") ||
                    text.contains("address") -> {
                speakHomeAddress()
            }

            else -> {
                tts.speak(
                    "Sorry, I did not understand",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    null
                )
            }
        }
    }

    // ---------------- FEATURES ----------------
    private fun speakHomeAddress() {
        val address = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getString("home_address", null)

        tts.speak(
            address ?: "Home address not set",
            TextToSpeech.QUEUE_FLUSH,
            null,
            null
        )
    }

    private fun openDirectionsToHome() {
        val homeAddress = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getString("home_address", null)

        if (homeAddress == null) {
            tts.speak("Home address not set", TextToSpeech.QUEUE_FLUSH, null, null)
            return
        }

        val uri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(homeAddress)}"
        )
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun handleEmergency() {
        tts.speak(
            "Emergency detected. Calling help and sharing location",
            TextToSpeech.QUEUE_FLUSH,
            null,
            null
        )

        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
        sendLocationSms()
    }

    private fun sendLocationSms() {
        val contact = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getString("emergency_contact", null) ?: return

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val message =
                    "EMERGENCY! My location: https://maps.google.com/?q=${location.latitude},${location.longitude}"

                SmsManager.getDefault().sendTextMessage(
                    contact,
                    null,
                    message,
                    null,
                    null
                )
            }
        }
    }
}

// ---------------- UI ----------------
@Composable
fun VoiceHelpUI(onMicClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Voice Help Assistant",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onMicClick) {
                Text("🎤 Speak")
            }
        }
    }
}
