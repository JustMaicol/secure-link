package com.securelink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource

// spec 100 · Versión mínima del esqueleto (A1). Desde el hito H0 es de P2.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Text(stringResource(R.string.app_name))
        }
    }
}
