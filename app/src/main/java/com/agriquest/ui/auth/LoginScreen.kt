package com.agriquest.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agriquest.R
import com.agriquest.ui.components.PixelButton
import com.agriquest.ui.components.PixelGreen
import com.agriquest.ui.components.PixelPanel
import com.agriquest.ui.theme.AppTextColor

/** Log in or create an AgriQuest account (Supabase Auth, email + password). */
@Composable
fun LoginScreen(viewModel: AuthViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginContent(
        state = state,
        isConfigured = viewModel.isConfigured,
        onModeChange = viewModel::setRegistering,
        onSubmit = viewModel::submit,
        modifier = modifier,
    )
}

@Composable
private fun LoginContent(
    state: AuthUiState,
    isConfigured: Boolean,
    onModeChange: (Boolean) -> Unit,
    onSubmit: (name: String, email: String, password: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.start_bg), contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Column(
            Modifier.widthIn(max = 430.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .imePadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(painterResource(R.drawable.aqlogo), contentDescription = "AgriQuest",
                modifier = Modifier.fillMaxWidth(0.7f).height(120.dp), contentScale = ContentScale.Fit)
            PixelPanel {
                Text(if (state.isRegistering) "CREATE ACCOUNT" else "LOG IN",
                    style = MaterialTheme.typography.titleMedium, color = AppTextColor)
                if (!isConfigured) {
                    Text("Supabase is not set up in this build. Add agriquest.supabaseUrl and agriquest.supabaseKey to local.properties, then rebuild.",
                        style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                }
                if (state.isRegistering) {
                    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Your name") },
                        singleLine = true, enabled = !state.isLoading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                }
                OutlinedTextField(email, { email = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Email") },
                    singleLine = true, enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
                OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") },
                    singleLine = true, enabled = !state.isLoading,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done))
                state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ErrorRed) }
                if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = PixelGreen)
                PixelButton(
                    if (state.isRegistering) "Create Account" else "Log In",
                    { onSubmit(name, email, password) },
                    Modifier.fillMaxWidth(), color = PixelGreen, enabled = !state.isLoading && isConfigured
                )
                TextButton(onClick = { onModeChange(!state.isRegistering) }, enabled = !state.isLoading,
                    modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if (state.isRegistering) "Already have an account? Log in" else "New here? Create an account",
                        color = AppTextColor)
                }
            }
        }
    }
}

private val ErrorRed = Color(0xFFB53220)
