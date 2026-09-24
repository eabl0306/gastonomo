package com.bahoque.gastonomo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bahoque.gastonomo.feature.health.HealthRoute
import com.bahoque.gastonomo.feature.health.HealthViewModel
import com.bahoque.gastonomo.ui.theme.GastonomoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = (application as GastonomoApplication).container.healthViewModelFactory
        setContent {
            GastonomoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HealthRoute(
                        viewModel = viewModel<HealthViewModel>(factory = factory),
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
