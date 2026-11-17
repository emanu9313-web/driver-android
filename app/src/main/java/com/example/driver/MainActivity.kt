package com.example.driver

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.example.driver.ui.navigation.TaxiNavGraph
import com.example.driver.ui.theme.DriverTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        org.osmdroid.config.Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid_global_prefs", MODE_PRIVATE)
        )
        org.osmdroid.config.Configuration.getInstance().userAgentValue = "TaxiGO_App_Client/2.0 (Android_App)"

        enableEdgeToEdge()
        setContent {
            DriverTheme {
                // Solicitar permisos de GPS al iniciar
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }

                TaxiNavGraph()
            }
        }
    }
}
