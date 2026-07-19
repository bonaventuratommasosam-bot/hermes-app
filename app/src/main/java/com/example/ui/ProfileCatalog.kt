package com.example.ui

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Catalogo profili HermesBroker — sorgente unica di verità per il wizard.
 * I SOUL.md sono asset statici (sanitizzati, nessun segreto). Il manifest.json
 * descrive metadata + provider di default. Nessun dato fake, nessuna metrica simulata.
 */
data class ProfileMeta(
    val id: String,
    val displayName: String,
    val tagline: String,
    val emoji: String,
    val accent: String,
    val defaultProvider: String,
    val defaultModel: String,
    val tags: List<String>,
    val customAvatar: String? = null
)

object ProfileCatalog {

    private const val MANIFEST = "profiles/manifest.json"

    fun load(context: Context): List<ProfileMeta> {
        val json = readAsset(context, MANIFEST) ?: return emptyList()
        val root = JSONObject(json)
        val arr: JSONArray = root.getJSONArray("profiles")
        val out = mutableListOf<ProfileMeta>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                ProfileMeta(
                    id = o.getString("id"),
                    displayName = o.getString("displayName"),
                    tagline = o.getString("tagline"),
                    emoji = o.getString("emoji"),
                    accent = o.getString("accent"),
                    defaultProvider = o.getString("defaultProvider"),
                    defaultModel = o.getString("defaultModel"),
                    tags = o.optJSONArray("tags")?.let { tags ->
                        (0 until tags.length()).map { tags.getString(it) }
                    } ?: emptyList()
                )
            )
        }
        return out
    }

    /** SOUL.md reale del profilo, letto dagli asset. Resta vuoto se manca. */
    fun soul(context: Context, id: String): String {
        return readAsset(context, "profiles/$id/SOUL.md") ?: "# $id\n(SOUL non trovato)"
    }

    private fun readAsset(context: Context, path: String): String? {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            null
        }
    }
}
