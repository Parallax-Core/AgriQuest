package com.agriquest.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.agriquest.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to the AgriQuest Supabase project over its REST API (paper Table 3.7: "REST API over HTTPS").
 *
 * - Auth: `/auth/v1/...` for sign-up, log-in, token refresh and log-out. The session is kept in
 *   private app storage so the player stays logged in between launches.
 * - Data: `/rest/v1/<table>` and `/rest/v1/rpc/<function>` with the player's access token, so the
 *   database's Row Level Security only returns that player's rows.
 *
 * Uses the publishable (anon) key from local.properties. Never put the secret key in the app.
 * Note: HttpURLConnection can't send PATCH, so updates go through database functions (rpc).
 */
class SupabaseClient private constructor(
    private val prefs: SharedPreferences,
    private val baseUrl: String = BuildConfig.SUPABASE_URL.trimEnd('/'),
    private val apiKey: String = BuildConfig.SUPABASE_KEY,
) {

    /** A logged-in player's tokens. [expiresAtSec] is when [accessToken] stops working (Unix seconds). */
    data class Session(
        val accessToken: String,
        val refreshToken: String,
        val expiresAtSec: Long,
        val userId: String,
        val email: String?,
    )

    /** Supabase answered with an error. [message] is already readable for the player. */
    class SupabaseException(val statusCode: Int, message: String) : IOException(message)

    /** False when local.properties is missing the Supabase URL or key. */
    val isConfigured: Boolean get() = baseUrl.startsWith("https://") && apiKey.isNotBlank()

    private val _session = MutableStateFlow(loadSession())
    /** The current login; null when logged out. */
    val session: StateFlow<Session?> = _session.asStateFlow()

    private val refreshMutex = Mutex()

    // ------------------------------------------------------------------ auth

    suspend fun signIn(email: String, password: String): Session = withContext(Dispatchers.IO) {
        val body = JSONObject().put("email", email.trim()).put("password", password)
        val json = request("POST", "/auth/v1/token?grant_type=password", body.toString())
        saveSession(parseSession(JSONObject(json)))
    }

    /**
     * Creates an account. The database then creates the player's profile and first plant.
     * Needs "Confirm email" turned off in Supabase; otherwise the player must confirm first.
     */
    suspend fun signUp(email: String, password: String, displayName: String): Session = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .put("data", JSONObject().put("display_name", displayName.trim()))
        val json = JSONObject(request("POST", "/auth/v1/signup", body.toString()))
        if (!json.has("access_token")) {
            throw SupabaseException(200, "Account created. Check your email to confirm it, then log in.")
        }
        saveSession(parseSession(json))
    }

    /** Logs out on this phone right away, then tells Supabase to end the session. */
    suspend fun signOut() {
        val old = _session.value ?: return
        clearSession()
        withContext(Dispatchers.IO) {
            try {
                request("POST", "/auth/v1/logout", null, token = old.accessToken)
            } catch (e: IOException) {
                // Already logged out locally; nothing else to do.
            }
        }
    }

    // ------------------------------------------------------------------ data

    /** GET `/rest/v1/<pathAndQuery>`, e.g. `plants?select=id,name`. Returns the JSON text. */
    suspend fun get(pathAndQuery: String): String = authorized("GET", "/rest/v1/$pathAndQuery", null)

    /** POST `/rest/v1/<table>` to insert rows; returns the inserted rows as JSON. */
    suspend fun insert(table: String, row: JSONObject): String =
        authorized("POST", "/rest/v1/$table", row.toString(), prefer = "return=representation")

    /** Calls a database function, e.g. `request_command`. Returns the JSON result. */
    suspend fun rpc(function: String, args: JSONObject): String =
        authorized("POST", "/rest/v1/rpc/$function", args.toString())

    private suspend fun authorized(method: String, path: String, body: String?, prefer: String? = null): String {
        val current = validSession()
            ?: throw SupabaseException(401, "You are logged out. Please log in again.")
        return try {
            withContext(Dispatchers.IO) { request(method, path, body, current.accessToken, prefer) }
        } catch (e: SupabaseException) {
            if (e.statusCode != 401) throw e
            // The token may have expired early: refresh once and retry.
            val fresh = refresh(staleToken = current.accessToken) ?: throw e
            withContext(Dispatchers.IO) { request(method, path, body, fresh.accessToken, prefer) }
        }
    }

    /** The session, refreshed first if its access token expires within a minute. */
    private suspend fun validSession(): Session? {
        val current = _session.value ?: return null
        return if (current.expiresAtSec - nowSec() > 60) current else refresh(staleToken = current.accessToken)
    }

    /** Swaps the refresh token for new tokens. Logs out if Supabase no longer accepts it. */
    private suspend fun refresh(staleToken: String): Session? = refreshMutex.withLock {
        val current = _session.value ?: return null
        if (current.accessToken != staleToken) return current // another request already refreshed it
        try {
            val body = JSONObject().put("refresh_token", current.refreshToken)
            val json = withContext(Dispatchers.IO) {
                request("POST", "/auth/v1/token?grant_type=refresh_token", body.toString())
            }
            saveSession(parseSession(JSONObject(json)))
        } catch (e: SupabaseException) {
            if (e.statusCode in 400..499) {
                clearSession()
                null
            } else {
                throw e
            }
        }
    }

    // ------------------------------------------------------------------ HTTP

    private fun request(
        method: String,
        path: String,
        body: String?,
        token: String? = null,
        prefer: String? = null,
    ): String {
        if (!isConfigured) {
            throw SupabaseException(0, "Supabase is not set up. Add agriquest.supabaseUrl and agriquest.supabaseKey to local.properties.")
        }
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("apikey", apiKey)
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            if (prefer != null) setRequestProperty("Prefer", prefer)
        }
        try {
            if (body != null) {
                val bytes = body.toByteArray(Charsets.UTF_8)
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw SupabaseException(status, errorMessage(text, status))
            return text
        } finally {
            connection.disconnect()
        }
    }

    /** Auth errors use "msg" or "error_description"; database errors use "message". */
    private fun errorMessage(body: String, status: Int): String {
        val json = try {
            JSONObject(body)
        } catch (e: Exception) {
            null
        }
        val raw = listOf("msg", "error_description", "message", "error")
            .firstNotNullOfOrNull { key -> json?.optString(key)?.takeIf { it.isNotBlank() } }
            ?: return "Server error $status"
        return when {
            raw.contains("Invalid login credentials", ignoreCase = true) -> "Wrong email or password."
            raw.contains("already registered", ignoreCase = true) -> "An account with this email already exists."
            raw.contains("Email not confirmed", ignoreCase = true) -> "Confirm your email first, then log in."
            else -> raw
        }
    }

    // ------------------------------------------------------------------ session storage

    private fun parseSession(json: JSONObject): Session {
        val user = json.getJSONObject("user")
        val expiresAt = if (json.has("expires_at")) json.getLong("expires_at")
            else nowSec() + json.optLong("expires_in", 3600)
        return Session(
            accessToken = json.getString("access_token"),
            refreshToken = json.getString("refresh_token"),
            expiresAtSec = expiresAt,
            userId = user.getString("id"),
            email = user.optString("email").takeIf { it.isNotBlank() },
        )
    }

    private fun saveSession(session: Session): Session {
        prefs.edit()
            .putString(KEY_ACCESS, session.accessToken)
            .putString(KEY_REFRESH, session.refreshToken)
            .putLong(KEY_EXPIRES, session.expiresAtSec)
            .putString(KEY_USER, session.userId)
            .putString(KEY_EMAIL, session.email)
            .apply()
        _session.value = session
        return session
    }

    private fun loadSession(): Session? {
        val access = prefs.getString(KEY_ACCESS, null) ?: return null
        val refresh = prefs.getString(KEY_REFRESH, null) ?: return null
        val user = prefs.getString(KEY_USER, null) ?: return null
        return Session(access, refresh, prefs.getLong(KEY_EXPIRES, 0L), user, prefs.getString(KEY_EMAIL, null))
    }

    private fun clearSession() {
        prefs.edit().clear().apply()
        _session.value = null
    }

    private fun nowSec(): Long = System.currentTimeMillis() / 1000

    companion object {
        private const val PREFS = "agriquest_session"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_EXPIRES = "expires_at"
        private const val KEY_USER = "user_id"
        private const val KEY_EMAIL = "email"

        @Volatile
        private var INSTANCE: SupabaseClient? = null

        fun getInstance(context: Context): SupabaseClient =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SupabaseClient(
                    context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                ).also { INSTANCE = it }
            }
    }
}
