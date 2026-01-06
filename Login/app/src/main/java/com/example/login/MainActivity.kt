package com.example.login

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.login.ui.theme.LoginTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthScreen(
                        onAuthSuccess = {
                            // User is logged in
                            val user = FirebaseAuth.getInstance().currentUser
                            Toast.makeText(
                                this,
                                "Welcome ${user?.displayName ?: user?.email}!",
                                Toast.LENGTH_LONG
                            ).show()
                            // TODO: Navigate to home screen
                        }
                    )
                }
            }
        }
    }
}