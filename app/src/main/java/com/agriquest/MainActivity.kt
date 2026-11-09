package com.agriquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.agriquest.ui.theme.AppAccentBackground
import com.agriquest.ui.theme.AppCardBackground
import com.agriquest.data.repository.AgriQuestRepositoryImpl
import com.agriquest.ui.dashboard.DashboardScreen
import com.agriquest.ui.auth.AuthScreen
import com.agriquest.ui.auth.AuthViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agriquest.ui.splash.SplashScreen
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.viewmodel.AgriQuestViewModel
import com.agriquest.ui.updates.AppUpdatePrompt

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AgriQuestTheme {
                AppUpdatePrompt()
                var isStarted by rememberSaveable { mutableStateOf(false) }
                val authState by authViewModel.state.collectAsStateWithLifecycle()
                val repository by authViewModel.accountRepository.collectAsStateWithLifecycle()
                LaunchedEffect(authState.authenticated) {
                    if (!authState.authenticated) isStarted = false
                }

                if (!authState.authenticated || repository == null) {
                    AuthScreen(authViewModel)
                } else if (!isStarted) {
                    SplashScreen(
                        onStartGame = { isStarted = true }
                    )
                } else {
                    AccountDashboard(requireNotNull(repository), onSignOut = authViewModel::signOut)
                }
            }
        }
    }
}

@Composable
private fun AccountDashboard(repository: AgriQuestRepositoryImpl, onSignOut: () -> Unit) {
    // Removing this account from composition cancels its ViewModel jobs as well.
    val store = remember(repository) { ViewModelStore() }
    val owner = remember(store) { object : ViewModelStoreOwner { override val viewModelStore = store } }
    DisposableEffect(store) { onDispose { store.clear() } }
    val accountViewModel: AgriQuestViewModel = viewModel(viewModelStoreOwner = owner,
        factory = AgriQuestViewModel.Factory(repository))
    Column(Modifier.fillMaxSize()) {
        Surface(color = AppAccentBackground, contentColor = AppCardBackground) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onSignOut, colors = ButtonDefaults.textButtonColors(contentColor = AppCardBackground)) { Text("Log Out") }
            }
        }
        DashboardScreen(modifier = Modifier.weight(1f), viewModel = accountViewModel)
    }
}
