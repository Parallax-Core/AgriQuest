package com.agriquest.data.remote

import com.agriquest.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class AuthSession(val accessToken: String, val userId: String, val expiresInSeconds: Long)

class AuthException(message: String) : IOException(message)

/** Only the public Supabase key belongs in this client. Password checks run in Supabase Auth. */
class SupabaseAuthApi(
    private val baseUrl: String = BuildConfig.SUPABASE_URL,
    private val publicKey: String = BuildConfig.SUPABASE_ANON_KEY,
    private val openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }
) {
    suspend fun signIn(username: String, password: String): AuthSession {
        val response = request("/username-login", body = JSONObject()
            .put("username", username.trim()).put("password", password), usernameLogin = true)
        return verifiedSession(response)
    }

    suspend fun signUp(username: String, email: String, password: String) {
        // Never treat a signup response as a successful password login.
        request("/signup", body = JSONObject().put("email", email.trim()).put("password", password)
            .put("data", JSONObject().put("username", username.trim())))
    }

    suspend fun requestRecovery(email: String) {
        request("/recover", body = JSONObject().put("email", email.trim()))
    }

    suspend fun resendSignup(email: String) {
        request("/resend", body = JSONObject().put("email", email.trim()).put("type", "signup"))
    }

    suspend fun verifyRecovery(email: String, code: String): AuthSession {
        val response = request("/verify", body = JSONObject().put("email", email.trim())
            .put("token", code.trim()).put("type", "recovery"))
        return verifiedSession(response)
    }

    suspend fun verifySignup(email: String, code: String) {
        val response = request("/verify", body = JSONObject().put("email", email.trim())
            .put("token", code.trim()).put("type", "signup"))
        val session = verifiedSession(response)
        // Email confirmation returns a token, but the user must still log in with a password.
        signOut(session)
    }

    suspend fun updatePassword(session: AuthSession, password: String) {
        request("/user", method = "PUT", token = session.accessToken,
            body = JSONObject().put("password", password))
    }

    suspend fun signOut(session: AuthSession) {
        request("/logout?scope=local", token = session.accessToken)
    }

    private suspend fun verifiedSession(response: JSONObject): AuthSession {
        val token = response.optString("access_token")
        if (token.isBlank()) throw AuthException("Authentication did not return a valid session. Please log in again.")
        // Ask Auth to validate the issued bearer token and retrieve the real account.
        val user = request("/user", method = "GET", token = token)
        val id = user.optString("id")
        val expires = response.optLong("expires_in", 0)
        if (id.isBlank() || expires <= 0 || user.optBoolean("is_anonymous", false)) {
            throw AuthException("Unable to verify your account. Please log in again.")
        }
        if (user.optString("email_confirmed_at").let { it.isBlank() || it == "null" }) {
            throw AuthException("Confirm your email before logging in.")
        }
        return AuthSession(token, id, expires)
    }

    private suspend fun request(
        path: String, method: String = "POST", token: String? = null, body: JSONObject? = null,
        usernameLogin: Boolean = false
    ): JSONObject = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank() || publicKey.isBlank()) throw AuthException("Authentication is not configured. Contact the app administrator.")
        require(baseUrl.startsWith("https://")) { "Authentication requires HTTPS." }
        val connection = openConnection(URL(baseUrl.trimEnd('/') + (if (usernameLogin) "/functions/v1" else "/auth/v1") + path))
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey", publicKey)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
            if (status !in 200..299) {
                val code = json.optString("error_code", json.optString("code"))
                throw AuthException(when {
                    code == "invalid_credentials" || (usernameLogin && status in listOf(400, 401) && code != "email_not_confirmed") -> "Username or password is incorrect."
                    code == "email_not_confirmed" -> "Confirm your email before logging in."
                    code == "otp_expired" -> "This code is invalid or expired. Request a new code."
                    code == "weak_password" -> "Choose a stronger password with at least 8 characters."
                    status == 429 -> "Too many requests. Wait before trying again."
                    code.startsWith("hook_") || code == "unexpected_failure" -> "Email could not be sent. Wait before retrying, or contact support."
                    path == "/verify" -> "This code could not be verified. Check it or request another code."
                    else -> "Unable to complete this request. Please try again later."
                })
            }
            json
        } catch (e: AuthException) {
            throw e
        } catch (e: IOException) {
            throw AuthException("Could not reach the authentication server. Check your connection and try again.")
        } finally {
            connection.disconnect()
        }
    }
}
