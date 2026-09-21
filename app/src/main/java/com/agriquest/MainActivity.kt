package com.agriquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.agriquest.data.repository.AgriQuestRepositoryImpl
import com.agriquest.ui.dashboard.DashboardScreen
import com.agriquest.ui.splash.SplashScreen
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.viewmodel.AgriQuestViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AgriQuestViewModel by viewModels {
        AgriQuestViewModel.Factory(AgriQuestRepositoryImpl.getInstance(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AgriQuestTheme {
                var isStarted by rememberSaveable { mutableStateOf(false) }

                if (!isStarted) {
                    SplashScreen(
                        onStartGame = { isStarted = true }
                    )
                } else {
                    DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}
