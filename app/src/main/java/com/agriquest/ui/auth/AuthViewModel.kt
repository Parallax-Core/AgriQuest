package com.agriquest.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.agriquest.data.remote.SupabaseClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Login / register screen state. */
data class AuthUiState(
    val isRegistering: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * Logs the player in or creates an account through [SupabaseClient].
 * When it succeeds, [SupabaseClient.session] changes and MainActivity shows the game.
 */
class AuthViewModel(private val supabase: SupabaseClient) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val isConfigured: Boolean get() = supabase.isConfigured

    fun setRegistering(registering: Boolean) {
        _state.update { it.copy(isRegistering = registering, error = null) }
    }

    fun submit(name: String, email: String, password: String) {
        val current = _state.value
        if (current.isLoading) return
        val problem = when {
            current.isRegistering && name.isBlank() -> "Enter your name."
            !email.contains('@') -> "Enter a valid email address."
            password.length < 6 -> "The password needs at least 6 characters."
            else -> null
        }
        if (problem != null) {
            _state.update { it.copy(error = problem) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                if (current.isRegistering) supabase.signUp(email, password, name) else supabase.signIn(email, password)
                _state.update { it.copy(isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = readable(e)) }
            }
        }
    }

    private fun readable(e: Exception): String = when (e) {
        is SupabaseClient.SupabaseException -> e.message ?: "Something went wrong."
        is UnknownHostException -> "No internet connection. Check your connection and try again."
        is SocketTimeoutException -> "The server took too long to answer. Please try again."
        is IOException -> e.message ?: "Could not reach the server."
        else -> "Something went wrong. Please try again."
    }

    class Factory(private val supabase: SupabaseClient) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(supabase) as T
    }
}
