package com.agriquest.ui.auth

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.agriquest.R
import com.agriquest.ui.components.AppCard
import com.agriquest.ui.components.AppText
import com.agriquest.ui.components.PixelButton
import com.agriquest.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val auth by viewModel.state.collectAsStateWithLifecycle()
    var signUp by rememberSaveable { mutableStateOf(false) }
    var forgotPassword by rememberSaveable { mutableStateOf(false) }
    var resetEmail by rememberSaveable { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    // Passwords stay in memory and are never saved into activity state.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(auth.signupCompleted) {
        if (auth.signupCompleted) {
            signUp = false
            code = ""
            password = ""
            confirmation = ""
            error = null
            viewModel.acknowledgeSignup()
        }
    }
    val switchForm = {
        if (!auth.busy) {
            signUp = !signUp
            password = ""
            confirmation = ""
            code = ""
            error = null
            viewModel.clearFeedback()
        }
    }
    BackHandler(enabled = signUp || forgotPassword) {
        if (!auth.busy) {
            if (forgotPassword) {
                forgotPassword = false; error = null; password = ""; confirmation = ""; code = ""
                viewModel.leaveRecovery()
            } else switchForm()
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.start_bg), contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
        )
        Column(
            Modifier.widthIn(max = AppLayout.maxWidth).fillMaxWidth().safeDrawingPadding()
                .imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = AppLayout.pagePadding, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.aqlogo),
                contentDescription = "AgriQuest",
                modifier = Modifier.fillMaxWidth().height(140.dp),
                contentScale = ContentScale.Fit
            )
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(AppLayout.cardPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (forgotPassword) {
                        AppText("Forgot password?", style = MaterialTheme.typography.titleMedium, heading = true)
                        if (auth.recoveryVerified) {
                            AuthField(password, { password = it; error = null }, "New Password", Icons.Rounded.Lock, secret = true, enabled = !auth.busy)
                            AuthField(confirmation, { confirmation = it; error = null }, "Confirm Password", Icons.Rounded.Lock, secret = true, enabled = !auth.busy)
                            PixelButton("Update Password", enabled = !auth.busy, onClick = {
                                error = when {
                                    password.length < 8 -> "Use at least 8 characters for your password."
                                    password != confirmation -> "Passwords do not match."
                                    else -> null
                                }
                                if (error == null) {
                                    viewModel.changePassword(password)
                                    password = ""; confirmation = ""
                                }
                            })
                        } else {
                        AppText("Enter your email address to request a password reset code.")
                        AuthField(resetEmail, { resetEmail = it; error = null }, "Email Address", Icons.Rounded.Email, email = true, enabled = !auth.busy && !auth.recoverySent)
                        PixelButton(
                            text = if (auth.recoverySent) "Resend Code" else "Send Code",
                            enabled = !auth.busy,
                            modifier = Modifier.heightIn(min = 48.dp),
                            onClick = {
                                if (!Patterns.EMAIL_ADDRESS.matcher(resetEmail.trim()).matches()) {
                                    error = "Enter a valid email address."
                                } else {
                                    error = null
                                    code = ""
                                    viewModel.sendRecovery(resetEmail)
                                }
                            }
                        )
                        if (auth.recoverySent) {
                            AuthField(code, { code = it; error = null }, "Reset Code", Icons.Rounded.Key, enabled = !auth.busy)
                            PixelButton("Verify Code", enabled = !auth.busy, onClick = {
                                if (code.isBlank()) error = "Enter the code from your email."
                                else { error = null; viewModel.verifyRecovery(code); code = "" }
                            })
                        }
                        }
                        TextButton(enabled = !auth.busy, onClick = {
                            forgotPassword = false; error = null; password = ""; confirmation = ""; code = ""
                            viewModel.leaveRecovery()
                        }) {
                            AppText("Back to Log In", color = AppAccentBackground)
                        }
                    } else {
                    AppText(
                        when {
                            signUp && auth.signupPending -> "Verify your email"
                            signUp -> "Create an account"
                            else -> "Welcome Back!"
                        },
                        style = MaterialTheme.typography.titleMedium, heading = true
                    )
                    AppText(
                        when {
                            signUp && auth.signupPending -> "Enter the verification code sent to ${auth.signupEmail.orEmpty()}."
                            signUp -> "Join AgriQuest today!"
                            else -> "Log In to continue your journey!"
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    if (signUp && auth.signupPending) {
                        AuthField(code, { code = it; error = null }, "Confirmation Code", Icons.Rounded.Key, enabled = !auth.busy)
                        PixelButton("Verify Account", enabled = !auth.busy, onClick = {
                            if (code.isBlank()) error = "Enter the code from your email."
                            else { error = null; viewModel.verifySignup(code); code = "" }
                        })
                        PixelButton(
                            text = "Resend Code",
                            enabled = !auth.busy,
                            onClick = { error = null; code = ""; viewModel.resendSignup() }
                        )
                    } else {
                    AuthField(username, { username = it; error = null }, "Username", Icons.Rounded.Person, enabled = !auth.busy)
                    if (signUp) AuthField(email, { email = it; error = null }, "Email Address", Icons.Rounded.Email, email = true, enabled = !auth.busy)
                    AuthField(password, { password = it; error = null }, if (signUp) "Create Password" else "Password", Icons.Rounded.Lock, secret = true, enabled = !auth.busy)
                    if (signUp) AuthField(confirmation, { confirmation = it; error = null }, "Confirm Password", Icons.Rounded.Lock, secret = true, enabled = !auth.busy)
                    if (!signUp) TextButton(
                        enabled = !auth.busy,
                        onClick = { forgotPassword = true; password = ""; error = null; viewModel.clearFeedback() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { AppText("Forgot password?", color = AppAccentBackground) }
                    PixelButton(
                        text = if (signUp) "Sign Up" else "Log In",
                        enabled = !auth.busy,
                        onClick = {
                            error = when {
                                username.isBlank() -> "Enter your username."
                                username.trim().length > 128 -> "Use at most 128 characters for your username."
                                signUp && !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address."
                                password.isBlank() -> "Enter your password."
                                signUp && password.length < 8 -> "Use at least 8 characters for your password."
                                signUp && password != confirmation -> "Passwords do not match."
                                else -> null
                            }
                            if (error == null) {
                                if (signUp) viewModel.signUp(username, email, password)
                                else viewModel.signIn(username, password)
                                password = ""
                                confirmation = ""
                            }
                        },
                        modifier = Modifier.widthIn(min = 170.dp).heightIn(min = 48.dp)
                    )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        HorizontalDivider(Modifier.weight(1f), color = AppCardBorder)
                        AppText("OR", style = MaterialTheme.typography.labelMedium)
                        HorizontalDivider(Modifier.weight(1f), color = AppCardBorder)
                    }
                    TextButton(onClick = switchForm, enabled = !auth.busy) {
                        AppText(
                            if (signUp) "Already have an account? Log In" else "Don't have an account? Sign Up",
                            style = MaterialTheme.typography.bodySmall, color = AppAccentBackground
                        )
                    }
                    }
                    if (auth.busy) CircularProgressIndicator(Modifier.size(24.dp), color = AppAccentBackground)
                    (error ?: auth.error)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    auth.message?.let { AppText(it) }
                }
            }
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    secret: Boolean = false,
    email: Boolean = false,
    enabled: Boolean = true
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onValueChange, enabled = enabled,
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        label = { AppText(label) },
        textStyle = MaterialTheme.typography.bodySmall,
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        trailingIcon = if (secret) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = if (visible) "Hide password" else "Show password"
                    )
                }
            }
        } else null,
        visualTransformation = if (secret && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = when {
            secret -> KeyboardType.Password
            email -> KeyboardType.Email
            else -> KeyboardType.Text
        }),
        shape = AppLayout.buttonShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppTextColor, unfocusedTextColor = AppTextColor,
            focusedContainerColor = AppCardBackground, unfocusedContainerColor = AppCardBackground,
            focusedBorderColor = AppAccentBackground, unfocusedBorderColor = AppCardBorder,
            focusedLeadingIconColor = AppAccentBackground, unfocusedLeadingIconColor = AppTextColor,
            focusedTrailingIconColor = AppAccentBackground, unfocusedTrailingIconColor = AppTextColor,
            cursorColor = AppAccentBackground
        )
    )
}
