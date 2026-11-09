package com.agriquest.ui.updates

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** Optional update prompt; existing login, navigation and screens stay unchanged. */
@Composable
fun AppUpdatePrompt(model: AppUpdateViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    fun mayInstall() = Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()
    fun openInstaller() {
        val apk = model.state.value.apk ?: return
        try {
            if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")))
            } else {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
                context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                model.installationOpened()
            }
        } catch (_: Exception) { model.installationFailed() }
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (model.state.value.apk != null && mayInstall()) openInstaller()
                else model.checkForUpdate()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(model) { model.checkForUpdate() }
    LaunchedEffect(state.apk) { if (state.apk != null) openInstaller() }
    val release = state.release ?: return
    AlertDialog(
        onDismissRequest = model::dismiss,
        title = { Text("Update available") },
        text = { Column {
            Text("AgriQuest ${release.versionName} is ready to install.")
            if (state.downloading) {
                LinearProgressIndicator(progress = { state.progress / 100f })
                Text("Downloading ${state.progress}%")
            }
            if (state.apk != null) Text("Allow AgriQuest to install updates if Android asks, then tap Install.")
            state.error?.let { Text(it) }
        } },
        confirmButton = { TextButton(enabled = !state.downloading,
            onClick = { if (state.apk != null) openInstaller() else model.download() }) {
            Text(if (state.apk != null) "Install" else "Update")
        } },
        dismissButton = { TextButton(enabled = !state.downloading, onClick = model::dismiss) { Text("Later") } }
    )
}
