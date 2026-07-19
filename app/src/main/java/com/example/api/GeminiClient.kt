package com.example.api

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Call Gemini to refine a soul prompt based on user request.
     */
    suspend fun refineSoulPrompt(
        originalPrompt: String,
        refinementRequest: String,
        customKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = if (!customKey.isNullOrBlank()) customKey else BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API key is missing or is the default placeholder!")
            return@withContext "Error: Gemini API Key is not set. Please enter your API key in the settings or configure it in the AI Studio Secrets panel."
        }

        val systemInstruction = "You are an expert AI prompt architect specializing in Hermes Agent profiles. " +
                "You refine a profile's SOUL.md file according to the user's customization request. " +
                "Maintain the core structure, tone, rules, and signature of the original soul. " +
                "Return ONLY the refined SOUL.md text. DO NOT wrap the output in markdown backticks (such as ```markdown or ```). Just return the raw text."

        val promptText = "Original SOUL.md:\n\n$originalPrompt\n\nCustomization request: $refinementRequest\n\nGenerate the refined SOUL.md now."

        try {
            // Build request JSON
            val requestJson = JSONObject()
            
            // Contents
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", promptText)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            // System Instruction
            val sysInstObj = JSONObject()
            val sysPartsArray = JSONArray()
            val sysPartObj = JSONObject()
            sysPartObj.put("text", systemInstruction)
            sysPartsArray.put(sysPartObj)
            sysInstObj.put("parts", sysPartsArray)
            requestJson.put("systemInstruction", sysInstObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string()

            if (!response.isSuccessful || bodyString == null) {
                Log.e(TAG, "API error: ${response.code} - $bodyString")
                return@withContext "Error calling Gemini: HTTP ${response.code}. Please verify your API Key and connection."
            }

            val responseJson = JSONObject(bodyString)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    if (text.isNotBlank()) {
                        return@withContext text.trim()
                    }
                }
            }
            return@withContext "Error: Unable to parse response from Gemini."
        } catch (e: Exception) {
            Log.e(TAG, "Exception during prompt refinement", e)
            return@withContext "Error: ${e.message}"
        }
    }
}
