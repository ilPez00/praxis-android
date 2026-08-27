package com.praxis.android.data.api

import android.content.Context
import com.praxis.android.auth.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

/**
 * Direct uploads to Supabase Storage — the same `notebook-files` bucket the
 * web app writes to, same path convention, so a capture taken on the phone
 * renders everywhere.
 *
 * Auth is the user's own JWT (the storage policies are per-user paths); the
 * anon key rides along as `apikey` because every Supabase request carries it,
 * even an authenticated one.
 */
object SupabaseStorage {

    private const val BUCKET = "notebook-files"
    private val client = OkHttpClient()

    data class UploadResult(val publicUrl: String)

    /**
     * Upload [file] to [storagePath] inside the bucket. Throws on any failure —
     * callers decide whether that means "queue for later" or "tell the user".
     */
    suspend fun upload(context: Context, file: File, storagePath: String, mimeType: String): UploadResult =
        withContext(Dispatchers.IO) {
            val token = AuthManager.getToken(context)
                ?: throw IllegalStateException("Not signed in")

            val url = "${app.praxisweb.xyz.BuildConfig.SUPABASE_URL}/storage/v1/object/$BUCKET/$storagePath"
            val body = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("apikey", app.praxisweb.xyz.BuildConfig.SUPABASE_ANON_KEY)
                .header("Content-Type", mimeType)
                .header("x-upsert", "true")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Upload failed: HTTP ${response.code}")
                }
            }

            UploadResult(
                publicUrl = "${app.praxisweb.xyz.BuildConfig.SUPABASE_URL}/storage/v1/object/public/$BUCKET/$storagePath"
            )
        }

    /** Standard key layout shared by web and app, so files stay browsable. */
    fun storagePath(userId: String, fileName: String): String =
        "notebook/${userId}/${System.currentTimeMillis()}-$fileName"

    /** Small helper for building attachment JSON in entry payloads. */
    fun attachmentJson(url: String, name: String, mimeType: String): JSONObject =
        JSONObject().put("type", "file").put("url", url).put("name", name).put("mimeType", mimeType)
}
