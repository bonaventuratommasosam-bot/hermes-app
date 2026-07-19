package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.ProfileCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Stato del wizard HermesBro Profile Lab.
 * Step 0: catalogo profili (multi-select)
 * Step 1: provider + API key (per profilo o globale)
 * Step 2: output → CLI / Telegram / SSH
 * Nessun dato simulato: qui vivono solo scelte utente e file generati.
 */
data class ProfileSelection(
    val id: String,
    val displayName: String,
    val provider: String = "openrouter",
    val model: String = "",
    val apiKey: String = "",
    val baseUrl: String = ""
)

data class DeployConfig(
    val mode: String = "cli",         // cli | telegram | ssh
    val botToken: String = "",
    val sshHost: String = "",
    val sshUser: String = ""
)

class ProfileLabViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application

    val catalog: StateFlow<List<ProfileMeta>> = MutableStateFlow(ProfileCatalog.load(application))
    val selected: MutableStateFlow<MutableMap<String, ProfileSelection>> = MutableStateFlow(mutableMapOf())
    val step: MutableStateFlow<Int> = MutableStateFlow(0)
    val deploy: MutableStateFlow<DeployConfig> = MutableStateFlow(DeployConfig())
    val generatedOutput: MutableStateFlow<String> = MutableStateFlow("")
    val copied: MutableStateFlow<Boolean> = MutableStateFlow(false)

    fun toggleProfile(meta: ProfileMeta) {
        val cur = selected.value.toMutableMap()
        if (cur.containsKey(meta.id)) {
            cur.remove(meta.id)
        } else {
            val p = ConfigBuilder.preset(meta.defaultProvider)
            cur[meta.id] = ProfileSelection(
                id = meta.id,
                displayName = meta.displayName,
                provider = meta.defaultProvider,
                model = meta.defaultModel
            )
        }
        selected.value = cur
    }

    fun updateSelection(id: String, provider: String, model: String, apiKey: String, baseUrl: String) {
        val cur = selected.value.toMutableMap()
        val existing = cur[id] ?: return
        cur[id] = existing.copy(provider = provider, model = model, apiKey = apiKey, baseUrl = baseUrl)
        selected.value = cur
    }

    fun setStep(s: Int) { step.value = s }

    fun setDeploy(d: DeployConfig) { deploy.value = d }

    fun markCopied() { copied.value = true }

    /** Assembla l'output reale: per ogni profilo scelto genera SOUL + config + deploy script. */
    fun generate() {
        viewModelScope.launch(Dispatchers.Default) {
            val sel = selected.value.values.toList()
            val sb = StringBuilder()
            sel.forEach { s ->
                val soul = ProfileCatalog.soul(app, s.id)
                val pc = ProviderConfig(
                    provider = s.provider,
                    model = s.model.ifBlank { ConfigBuilder.preset(s.provider).model },
                    apiKey = s.apiKey,
                    baseUrl = s.baseUrl,
                    apiMode = if (s.provider == "anthropic") "anthropic" else "openai"
                )
                val cfg = ConfigBuilder.buildConfigYaml(s.id, pc)
                sb.appendLine("================ PROFILO: ${s.displayName} (${s.id}) ================")
                sb.appendLine()
                sb.appendLine("----- SOUL.md -----")
                sb.appendLine(soul)
                sb.appendLine()
                sb.appendLine("----- config.yaml -----")
                sb.appendLine(cfg)
                sb.appendLine()
                when (deploy.value.mode) {
                    "telegram" -> {
                        sb.appendLine("----- DEPLOY TELEGRAM -----")
                        sb.appendLine(ConfigBuilder.buildTelegramStep(deploy.value.botToken, s.id))
                    }
                    "ssh" -> {
                        sb.appendLine("----- DEPLOY SSH (${deploy.value.sshUser}@${deploy.value.sshHost}) -----")
                        sb.appendLine(ConfigBuilder.buildSshDeploy(deploy.value.sshHost, deploy.value.sshUser, s.id, soul, cfg))
                    }
                    else -> {
                        sb.appendLine("----- DEPLOY CLI -----")
                        sb.appendLine(ConfigBuilder.buildCliDeployScript(s.id, soul, cfg))
                    }
                }
                sb.appendLine()
            }
            generatedOutput.value = sb.toString()
        }
    }

    /** Health check onesto: ping reale a hermesbro.cloud (solo diagnostica rete). */
    fun pingSite() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://hermesbro.cloud")
                    val c = url.openConnection() as HttpURLConnection
                    c.requestMethod = "GET"; c.connectTimeout = 4000; c.readTimeout = 4000
                    c.connect(); c.responseCode; c.disconnect()
                } catch (_: Exception) { /* silent — diagnostica opzionale */ }
            }
        }
    }
}
