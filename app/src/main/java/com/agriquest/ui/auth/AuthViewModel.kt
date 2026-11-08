package com.agriquest.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agriquest.data.remote.AuthException
import com.agriquest.data.remote.AuthSession
import com.agriquest.data.remote.SupabaseAuthApi
import com.agriquest.data.repository.AgriQuestRepositoryImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val authenticated: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val recoverySent: Boolean = false,
    val recoveryVerified: Boolean = false,
    val signupPending: Boolean = false,
    val signupEmail: String? = null,
    val signupCompleted: Boolean = false
)

/** Tokens live only in memory. A restored activity flag can never authorize Play. */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val api = SupabaseAuthApi()
    private val mutableState = MutableStateFlow(AuthUiState())
    val state = mutableState.asStateFlow()
    private val mutableRepository = MutableStateFlow<AgriQuestRepositoryImpl?>(null)
    val accountRepository = mutableRepository.asStateFlow()
    private var loginSession: AuthSession? = null
    private var recoverySession: AuthSession? = null
    private var recoveryEmail: String? = null
    private var expiryJob: Job? = null

    fun clearFeedback() { mutableState.update { it.copy(error = null, message = null) } }

    fun signIn(username: String, password: String) = perform {
        val session = api.signIn(username, password)
        val repository = AgriQuestRepositoryImpl.forAccount(getApplication(), session)
        mutableRepository.value?.close()
        mutableRepository.value = repository
        loginSession = session
        mutableState.update { it.copy(authenticated = true) }
        expiryJob?.cancel()
        expiryJob = viewModelScope.launch {
            delay(session.expiresInSeconds * 1000)
            loginSession = null
            mutableRepository.value?.close()
            mutableRepository.value = null
            mutableState.update { it.copy(authenticated = false, message = "Your session expired. Please log in again.") }
        }
    }

    override fun onCleared() {
        mutableRepository.value?.close()
        super.onCleared()
    }

    fun signOut() {
        expiryJob?.cancel()
        val session = loginSession
        loginSession = null
        mutableRepository.value?.close()
        mutableRepository.value = null
        mutableState.value = AuthUiState(message = "You have been logged out.")
        if (session != null) viewModelScope.launch {
            try { api.signOut(session) } catch (_: AuthException) { /* Local progress and account isolation are retained. */ }
        }
    }

    fun signUp(username: String, email: String, password: String) = perform {
        api.signUp(username, email, password)
        mutableState.update { it.copy(signupPending = true, signupEmail = email.trim(), signupCompleted = false,
            message = "Check your inbox and spam folder for the verification code. Your account must be verified before you can log in.") }
    }

    fun resendSignup() = perform {
        val email = state.value.signupEmail ?: throw AuthException("Sign up first.")
        api.resendSignup(email)
        mutableState.update { it.copy(message = "A new verification code was requested. Check your inbox and spam folder.") }
    }

    fun verifySignup(code: String) = perform {
        api.verifySignup(state.value.signupEmail ?: throw AuthException("Sign up first."), code)
        mutableState.update { it.copy(signupPending = false, signupEmail = null, signupCompleted = true,
            message = "Account verified. Log in to continue.") }
    }

    fun acknowledgeSignup() { mutableState.update { it.copy(signupCompleted = false) } }

    fun sendRecovery(email: String) = perform {
        api.requestRecovery(email)
        recoverySession = null
        recoveryEmail = email.trim()
        mutableState.update { it.copy(recoverySent = true, recoveryVerified = false,
            message = "If an account exists for this email, a reset code has been sent. Check your inbox and spam folder.") }
    }

    fun verifyRecovery(code: String) = perform {
        val email = recoveryEmail ?: throw AuthException("Request a reset code first.")
        recoverySession = api.verifyRecovery(email, code)
        // Recovery verification never unlocks Play.
        mutableState.update { it.copy(recoveryVerified = true, message = "Code verified. Choose a new password.") }
    }

    fun changePassword(password: String) = perform {
        val session = recoverySession ?: throw AuthException("Verify your reset code first.")
        api.updatePassword(session, password)
        recoverySession = null
        recoveryEmail = null
        mutableState.update { it.copy(recoveryVerified = false, recoverySent = false,
            message = "Password updated. Return to Log In and use your new password.") }
        // Revoke the temporary recovery session after updating the password.
        try { api.signOut(session) } catch (_: AuthException) { /* The token is discarded locally regardless. */ }
    }

    fun leaveRecovery() {
        if (state.value.busy) return
        val session = recoverySession
        recoverySession = null
        recoveryEmail = null
        mutableState.update { it.copy(recoverySent = false, recoveryVerified = false, error = null, message = null) }
        if (session != null) viewModelScope.launch { try { api.signOut(session) } catch (_: AuthException) { } }
    }

    private fun perform(action: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableState.update { it.copy(error = if (e is AuthException) e.message else "Unable to complete this request. Please try again.") }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }
}
