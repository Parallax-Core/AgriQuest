package com.agriquest.ui.updates

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agriquest.data.remote.AppRelease
import com.agriquest.data.remote.AppUpdateApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class AppUpdateState(val release: AppRelease? = null, val downloading: Boolean = false,
    val progress: Int = 0, val apk: File? = null, val error: String? = null)

class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val api = AppUpdateApi()
    private val mutableState = MutableStateFlow(AppUpdateState())
    val state = mutableState.asStateFlow()
    private var checkJob: Job? = null
    private var dismissedVersion: Int? = null

    fun checkForUpdate() {
        if (checkJob?.isActive == true || state.value.release != null) return
        checkJob = viewModelScope.launch {
            try {
                val release = api.latest()
                if (release != null && release.versionCode != dismissedVersion) mutableState.value = AppUpdateState(release)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Update checks must not block login or plant care. */ }
        }
    }

    fun download() {
        val release = state.value.release ?: return
        if (state.value.downloading) return
        mutableState.update { it.copy(downloading = true, progress = 0, error = null) }
        viewModelScope.launch {
            try {
                val apk = api.download(getApplication(), release) { percent -> mutableState.update { it.copy(progress = percent) } }
                mutableState.update { it.copy(downloading = false, apk = apk) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutableState.update { it.copy(downloading = false, error = e.message ?: "Could not download the update.") } }
        }
    }

    fun dismiss() {
        if (state.value.downloading) return
        dismissedVersion = state.value.release?.versionCode
        mutableState.value = AppUpdateState()
    }

    fun installationOpened() { dismiss() }
    fun installationFailed() { mutableState.update { it.copy(error = "Could not open the installer. Please try again.") } }
    override fun onCleared() { api.cancel(); super.onCleared() }
}
