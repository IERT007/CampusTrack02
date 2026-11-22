package com.example.cloud

import android.content.Context
import com.example.BuildConfig
import com.example.data.DataStoreManager
import com.example.data.SupabaseSession
import com.example.data.repository.CampusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Lightweight Supabase Auth + PostgREST Cloud Sync Manager.
 * Implemented strictly with OkHttp + org.json (zero heavy Ktor/Supabase BOM SDKs).
 * Safely guards against unconfigured SUPABASE_URL / SUPABASE_ANON_KEY so the app
 * remains 100% offline-first and crash-free when cloud credentials are omitted.
 */
class SupabaseSyncManager(
    context: Context,
    private val dataStoreManager: DataStoreManager = DataStoreManager(context.applicationContext)
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val supabaseUrl: String
        get() = BuildConfig.SUPABASE_URL.trim().trimEnd('/')

    private val supabaseAnonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY.trim()

    fun isConfigured(): Boolean {
        val url = supabaseUrl
        val key = supabaseAnonKey
        return url.startsWith("https://") &&
            url.length > "https://".length + 3 &&
            key.isNotBlank() &&
            !key.startsWith("YOUR_", ignoreCase = true)
    }

    suspend fun signUp(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets.")
            )
        }
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Enter a valid email address."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        try {
            val payload = JSONObject().apply {
                put("email", cleanEmail)
                put("password", password)
            }
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/signup")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception(parseErrorMessage(bodyStr, "Sign up failed (${response.code})")))
                }
                val json = JSONObject(bodyStr.ifBlank { "{}" })
                val accessToken = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", "")
                val userObj = json.optJSONObject("user")
                val userId = userObj?.optString("id", "").orEmpty()
                val userEmail = userObj?.optString("email", cleanEmail).orEmpty().ifBlank { cleanEmail }

                if (accessToken.isNotBlank() && userId.isNotBlank()) {
                    dataStoreManager.saveSupabaseSession(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        userId = userId,
                        email = userEmail
                    )
                    Result.success("Signed up and connected as $userEmail")
                } else {
                    Result.success("Account created for $userEmail. Verify your email, then Sign In.")
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error during sign up: ${e.message ?: "Check internet connection"}"))
        }
    }

    suspend fun signIn(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets.")
            )
        }
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Email and password are required."))
        }

        try {
            val payload = JSONObject().apply {
                put("email", cleanEmail)
                put("password", password)
            }
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=password")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception(parseErrorMessage(bodyStr, "Sign in failed (${response.code})")))
                }
                val json = JSONObject(bodyStr)
                val accessToken = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", "")
                val userObj = json.optJSONObject("user")
                val userId = userObj?.optString("id", "").orEmpty()
                val userEmail = userObj?.optString("email", cleanEmail).orEmpty().ifBlank { cleanEmail }

                if (accessToken.isBlank() || userId.isBlank()) {
                    return@withContext Result.failure(Exception("Invalid auth response from Supabase."))
                }

                dataStoreManager.saveSupabaseSession(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userId = userId,
                    email = userEmail
                )
                Result.success("Signed in as $userEmail")
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error during sign in: ${e.message ?: "Check internet connection"}"))
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = dataStoreManager.getSupabaseSessionDirect()
            if (isConfigured() && session != null && session.accessToken.isNotBlank()) {
                val request = Request.Builder()
                    .url("$supabaseUrl/auth/v1/logout")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer ${session.accessToken}")
                    .post("{}".toRequestBody(jsonMediaType))
                    .build()
                runCatching { client.newCall(request).execute().close() }
            }
        } finally {
            dataStoreManager.clearSupabaseSession()
        }
        Result.success(Unit)
    }

    suspend fun pushBackupToCloud(repository: CampusRepository): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Supabase credentials are not configured."))
        }
        var session = dataStoreManager.getSupabaseSessionDirect()
            ?: return@withContext Result.failure(IllegalStateException("Sign in to enable Supabase Cloud Sync."))

        try {
            val dbSnapshotJson = repository.exportToJson()
            val nowIso = Instant.now().toString()
            val upsertBody = JSONObject().apply {
                put("user_id", session.userId)
                put("payload", JSONObject(dbSnapshotJson))
                put("updated_at", nowIso)
            }.toString()

            var responseCode = executeUpsert(upsertBody, session.accessToken)
            if (responseCode == 401 && session.refreshToken.isNotBlank()) {
                val refreshed = refreshSession(session)
                if (refreshed != null) {
                    session = refreshed
                    responseCode = executeUpsert(upsertBody, session.accessToken)
                }
            }

            if (responseCode in 200..299) {
                dataStoreManager.setSupabaseLastSyncedAt(nowIso)
                Result.success(nowIso)
            } else {
                Result.failure(Exception("Cloud upload failed (HTTP $responseCode). Verify `caliper_backups` table & RLS policy."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cloud upload error: ${e.message ?: "Offline"}"))
        }
    }

    suspend fun restoreBackupFromCloud(repository: CampusRepository): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Supabase credentials are not configured."))
        }
        var session = dataStoreManager.getSupabaseSessionDirect()
            ?: return@withContext Result.failure(IllegalStateException("Sign in to restore from Supabase Cloud."))

        try {
            val encodedUserId = URLEncoder.encode(session.userId, "UTF-8")
            val endpoint = "$supabaseUrl/rest/v1/caliper_backups?user_id=eq.$encodedUserId&select=payload,updated_at"

            var pair = executeFetch(endpoint, session.accessToken)
            if (pair.first == 401 && session.refreshToken.isNotBlank()) {
                val refreshed = refreshSession(session)
                if (refreshed != null) {
                    session = refreshed
                    pair = executeFetch(endpoint, session.accessToken)
                }
            }

            val (code, bodyStr) = pair
            if (code !in 200..299) {
                return@withContext Result.failure(Exception("Cloud restore failed (HTTP $code)."))
            }

            val rows = JSONArray(bodyStr.ifBlank { "[]" })
            if (rows.length() == 0) {
                return@withContext Result.failure(Exception("No cloud backup found for ${session.email}."))
            }

            val row = rows.getJSONObject(0)
            val payloadAny = row.opt("payload")
            val payloadJsonString = when (payloadAny) {
                is JSONObject -> payloadAny.toString()
                is String -> payloadAny
                else -> ""
            }
            if (payloadJsonString.isBlank()) {
                return@withContext Result.failure(Exception("Cloud backup payload is empty."))
            }

            val imported = repository.importFromJson(payloadJsonString, triggerCloudSync = false)
            if (!imported) {
                return@withContext Result.failure(Exception("Failed to deserialize cloud backup into local database."))
            }

            val updatedAt = row.optString("updated_at", Instant.now().toString())
            dataStoreManager.setSupabaseLastSyncedAt(updatedAt)
            Result.success(updatedAt)
        } catch (e: Exception) {
            Result.failure(Exception("Cloud restore error: ${e.message ?: "Check internet connection"}"))
        }
    }

    private fun executeUpsert(jsonBody: String, accessToken: String): Int {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/caliper_backups?on_conflict=user_id")
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            return response.code
        }
    }

    private fun executeFetch(url: String, accessToken: String): Pair<Int, String> {
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            return Pair(response.code, response.body?.string().orEmpty())
        }
    }

    private suspend fun refreshSession(current: SupabaseSession): SupabaseSession? {
        return try {
            val payload = JSONObject().apply {
                put("refresh_token", current.refreshToken)
            }
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=refresh_token")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val json = JSONObject(response.body?.string().orEmpty())
                val newAccess = json.optString("access_token", "")
                val newRefresh = json.optString("refresh_token", current.refreshToken)
                if (newAccess.isBlank()) return null

                dataStoreManager.saveSupabaseSession(
                    accessToken = newAccess,
                    refreshToken = newRefresh,
                    userId = current.userId,
                    email = current.email
                )
                current.copy(accessToken = newAccess, refreshToken = newRefresh)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseErrorMessage(bodyStr: String, fallback: String): String {
        return try {
            val json = JSONObject(bodyStr)
            json.optString("error_description")
                .ifBlank { json.optString("msg") }
                .ifBlank { json.optString("message") }
                .ifBlank { fallback }
        } catch (_: Exception) {
            fallback
        }
    }
}
