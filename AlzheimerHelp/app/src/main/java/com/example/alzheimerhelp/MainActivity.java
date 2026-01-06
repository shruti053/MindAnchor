package com.example.alzheimerhelp;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Locale;


public class

MainActivity extends AppCompatActivity {

    Button startListeningBtn;
    FusedLocationProviderClient locationClient;

    String[] emergencyNumbers = {
        "9876543210",
        "9123456789"
    };
    private ActivityResultLauncher<Intent> speechLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startListeningBtn = findViewById(R.id.startListeningBtn);
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        ActivityCompat.requestPermissions(this, new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS
        }, 1);

        startListeningBtn.setOnClickListener(v -> startVoiceRecognition());
        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> data =
                                result.getData().getStringArrayListExtra(
                                        RecognizerIntent.EXTRA_RESULTS);

                        if (data != null && data.get(0).toLowerCase().contains("help")) {
                            sendLocationSMS();
                        }
                    }
                }
        );

    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechLauncher.launch(intent);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);

            if (result != null && result.get(0).toLowerCase().contains("help")) {
                sendLocationSMS();
            }
        }
    }

    private void sendLocationSMS() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationClient.getLastLocation().addOnSuccessListener(location -> {
        if (location != null) {
            double lat = location.getLatitude();
            double lon = location.getLongitude();

            String link = "https://maps.google.com/?q=" + lat + "," + lon;
            String msg = "EMERGENCY! Patient needs help.\nLocation:\n" + link;

            SmsManager sms = SmsManager.getDefault();
            for (String number : emergencyNumbers) {
                sms.sendTextMessage(number, null, msg, null, null);
            }

            Toast.makeText(this,
                "Emergency message sent!",
                Toast.LENGTH_LONG).show();
        }
    });
    }
}
