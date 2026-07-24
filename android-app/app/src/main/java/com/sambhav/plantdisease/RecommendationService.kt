package com.sambhav.plantdisease

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Fetches AI-generated treatment recommendations from OpenRouter
 * (https://openrouter.ai), which exposes an OpenAI-compatible chat API.
 *
 * The API key and model are injected at build time from local.properties via
 * BuildConfig, so no secrets live in source code. If no key is configured, or
 * the request fails, callers fall back to the bundled offline [DiseaseInfo].
 */
object RecommendationService {

    private const val ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** True when an API key has been supplied in local.properties. */
    fun isConfigured(): Boolean = BuildConfig.OPENROUTER_API_KEY.isNotBlank()

    /**
     * Asynchronously request advice for [info].
     * [onResult] is invoked on a background thread with the advice text, or
     * null on any failure (no key, network error, bad response).
     */
    fun fetch(info: DiseaseInfo.Info, onResult: (String?) -> Unit) {
        if (!isConfigured()) {
            onResult(null)
            return
        }

        val payload = JSONObject().apply {
            put("model", BuildConfig.OPENROUTER_MODEL)
            put("temperature", 0.4)
            // Generous budget so reasoning models still have room for the answer.
            put("max_tokens", 1024)
            // Strip chain-of-thought so message.content is just the advice
            // (ignored by non-reasoning models).
            put("reasoning", JSONObject().put("exclude", true))
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an expert agronomist advising farmers. " +
                            "Be concise, practical, and safety-conscious.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", buildPrompt(info))
                })
            })
        }.toString()

        val request = Request.Builder()
            .url(ENDPOINT)
            .addHeader("Authorization", "Bearer ${BuildConfig.OPENROUTER_API_KEY}")
            .addHeader("Content-Type", "application/json")
            // Optional OpenRouter attribution headers.
            .addHeader("X-Title", "E-Mali")
            .post(payload.toRequestBody(JSON))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onResult(null)

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    val advice = runCatching {
                        if (!resp.isSuccessful) return@runCatching null
                        val raw = resp.body?.string() ?: return@runCatching null
                        JSONObject(raw)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .trim()
                            .ifBlank { null }
                    }.getOrNull()
                    onResult(advice)
                }
            }
        })
    }

    private fun buildPrompt(info: DiseaseInfo.Info): String =
        if (info.healthy) {
            "A leaf-disease model classified a ${info.crop} plant as healthy. " +
                "Give 3-4 short bullet-point tips to keep it healthy " +
                "(watering, nutrition, spacing, monitoring). Keep it under 100 words."
        } else {
            "A leaf-disease model identified \"${info.condition}\" on a ${info.crop} plant. " +
                "Give practical management and treatment recommendations for a farmer as " +
                "3-4 short bullet points. Mention both organic and chemical options where " +
                "relevant. Keep it under 120 words."
        }
}
