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
import com.agriquest.ui.theme.AppTextColor
import com.agriquest.ui.components.PixelGreen
import com.agriquest.data.repository.AgriQuestRepositoryImpl
import com.agriquest.ui.dashboard.DashboardScreen
import com.agriquest.ui.auth.AuthScreen
import com.agriquest.ui.auth.AuthViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agriquest.ui.splash.SplashScreen
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.viewmodel.AgriQuestViewModel
import com.agriquest.ui.updates.AppUpdatePrompt
import androidx.compose.runtime.CompositionLocalProvider
import com.agriquest.ui.components.AddPlantDialog
import com.agriquest.ui.components.LocalPlantSpecies
import com.agriquest.ui.components.PlantSwitcher

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.agriquest.ui.sound.GameSounds.init(this)
        com.agriquest.notifications.MissedDayReminder.schedule(this)
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
                    AccountDashboard(requireNotNull(repository), authViewModel, onSignOut = authViewModel::signOut)
                }
            }
        }
    }
}

@Composable
private fun AccountDashboard(repository: AgriQuestRepositoryImpl, authViewModel: AuthViewModel, onSignOut: () -> Unit) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    var addingPlant by rememberSaveable { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(authState.plantMessage) {
        authState.plantMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            authViewModel.clearPlantMessage()
        }
    }
    // Close the dialog once the new plant is showing.
    LaunchedEffect(repository) { addingPlant = false }
    if (addingPlant) {
        AddPlantDialog(busy = authState.plantBusy, onAdd = authViewModel::addPlant, onDismiss = { addingPlant = false })
    }
    // Removing this account from composition cancels its ViewModel jobs as well.
    val store = remember(repository) { ViewModelStore() }
    val owner = remember(store) { object : ViewModelStoreOwner { override val viewModelStore = store } }
    DisposableEffect(store) { onDispose { store.clear() } }
    val accountViewModel: AgriQuestViewModel = viewModel(viewModelStoreOwner = owner,
        factory = AgriQuestViewModel.Factory(repository))
    var confirmingLogout by rememberSaveable { mutableStateOf(false) }
    if (confirmingLogout) {
        LogoutConfirmDialog(
            onYes = { confirmingLogout = false; onSignOut() },
            onNo = { confirmingLogout = false }
        )
    }
    Column(Modifier.fillMaxSize()) {
        Surface(color = AppAccentBackground, contentColor = AppCardBackground) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                PlantSwitcher(plants = repository.ownedPlants, currentPlantId = repository.currentPlantId,
                    busy = authState.plantBusy, onSwitch = authViewModel::switchPlant, onAdd = { addingPlant = true },
                    modifier = Modifier.weight(1f))
                TextButton(onClick = { confirmingLogout = true }, colors = ButtonDefaults.textButtonColors(contentColor = AppCardBackground)) { Text("Log Out") }
            }
        }
        CompositionLocalProvider(LocalPlantSpecies provides repository.species) {
            DashboardScreen(modifier = Modifier.weight(1f), viewModel = accountViewModel)
        }
    }
}

/** Asks before logging out. Yes uses the app's pixel green, No the app's tomato red. */
@Composable
private fun LogoutConfirmDialog(onYes: () -> Unit, onNo: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNo,
        containerColor = AppCardBackground,
        titleContentColor = AppTextColor,
        title = {
            Text("Do you want to\nlog out?", modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        },
        // Both buttons live in one centered row; AlertDialog would otherwise right-align them.
        confirmButton = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                Button(onClick = onNo, colors = ButtonDefaults.buttonColors(
                    containerColor = LogoutNoRed, contentColor = AppCardBackground)) {
                    Text("No", style = MaterialTheme.typography.headlineSmall)
                }
                Button(onClick = onYes, colors = ButtonDefaults.buttonColors(
                    containerColor = PixelGreen, contentColor = AppCardBackground)) {
                    Text("Yes", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    )
}

// Same red the app uses for the "unhealthy" scan state (ScanPresentation.RED).
private val LogoutNoRed = androidx.compose.ui.graphics.Color(0xFFB53220)
