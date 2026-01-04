package com.example.mindanchorcopy

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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import java.util.*
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RelativeLayout
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer






enum class Screen {
    SPLASH, PROFILE_SETUP, MAIN, PROFILE_EDIT
}

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private val prefs by lazy { getSharedPreferences("user_prefs", MODE_PRIVATE) }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    private val speechLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val text = result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.get(0)?.lowercase()
                if (text != null) detectKeywords(text)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)
        requestPermissions()

        setContent {
            var screen by remember { mutableStateOf(Screen.SPLASH) }

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                screen =
                    if (prefs.getBoolean("profile_done", false))
                        Screen.MAIN else Screen.PROFILE_SETUP
            }

            when (screen) {
                Screen.SPLASH -> SplashScreen()
                Screen.PROFILE_SETUP -> ProfileScreen(false, prefs) { n, a, c ->
                    saveProfile(n, a, c)
                    screen = Screen.MAIN
                }
                Screen.MAIN -> MainScreen(
                    onMicClick = { startSpeechRecognition() },
                    onProfileClick = { screen = Screen.PROFILE_EDIT }
                )
                Screen.PROFILE_EDIT -> ProfileScreen(true, prefs) { n, a, c ->
                    saveProfile(n, a, c)
                    screen = Screen.MAIN
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    private fun requestPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE
            )
        )
    }

    private fun saveProfile(name: String, address: String, contact: String) {
        prefs.edit()
            .putString("name", name)
            .putString("home_address", address)
            .putString("emergency_contact", contact)
            .putBoolean("profile_done", true)
            .apply()
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        speechLauncher.launch(intent)
    }

    private fun detectKeywords(text: String) {
        when {
            text.contains("lost") || text.contains("go home") -> {
                tts.speak("Helping you go home", TextToSpeech.QUEUE_FLUSH, null, null)
                sendLostSms()
                openDirectionsToHome()
            }
            text.contains("help") || text.contains("emergency") -> {
                tts.speak("Calling emergency contact", TextToSpeech.QUEUE_FLUSH, null, null)
                callEmergencyContact()
            }
            else -> {
                tts.speak("Please say help or lost", TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    private fun callEmergencyContact() {
        val number = prefs.getString("emergency_contact", null) ?: return
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }

    private fun openDirectionsToHome() {
        val address = prefs.getString("home_address", null) ?: return
        val uri =
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(address)}")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun sendLostSms() {
        val contact = prefs.getString("emergency_contact", null) ?: return
        val fused = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fused.lastLocation.addOnSuccessListener { loc: Location? ->
            loc?.let {
                SmsManager.getDefault().sendTextMessage(
                    contact,
                    null,
                    "I am LOST: https://maps.google.com/?q=${it.latitude},${it.longitude}",
                    null,
                    null
                )
            }
        }
    }
}

/* ---------- UI ---------- */

@Composable
fun SplashScreen() {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.05f,
            animationSpec = tween(
                durationMillis = 600,
                easing = FastOutSlowInEasing
            )
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 600,
                easing = FastOutSlowInEasing
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 🔵 Background
        Image(
            painter = painterResource(id = R.drawable.splash_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 🔵 Center content
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 🫀 Soft pulse logo
            Image(
                painter = painterResource(id = R.drawable.mindanchor_logo),
                contentDescription = "MindAnchor Logo",
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your voice, your safety.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f),
                letterSpacing = 0.6.sp
            )
        }
    }
}





@Composable
fun ProfileScreen(
    isEdit: Boolean,
    prefs: android.content.SharedPreferences,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        name = prefs.getString("name", "") ?: ""
        address = prefs.getString("home_address", "") ?: ""
        contact = prefs.getString("emergency_contact", "") ?: ""
    }

    GradientBg {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    if (isEdit) "Edit Profile" else "Setup Profile",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(name, { name = it }, label = { Text("Name") })
                OutlinedTextField(address, { address = it }, label = { Text("Home Address") })
                OutlinedTextField(contact, { contact = it }, label = { Text("Emergency Contact") })

                Spacer(Modifier.height(16.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (name.isNotEmpty() && address.isNotEmpty() && contact.isNotEmpty()) {
                            onSave(name, address, contact)
                        }
                    }
                ) {
                    Text("Save Profile")
                }
            }
        }
    }
}

@Composable
fun MainScreen(onMicClick: () -> Unit, onProfileClick: () -> Unit) {
    GradientBg {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.White, CircleShape)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Text("👤")
            }
        }

        Spacer(Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Voice Help Assistant",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onMicClick,
                shape = CircleShape,
                modifier = Modifier.size(140.dp)
            ) {
                Text("🎤", fontSize = 32.sp)
            }
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun GradientBg(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))
                )
            )
            .padding(24.dp),
        content = content
    )
}

