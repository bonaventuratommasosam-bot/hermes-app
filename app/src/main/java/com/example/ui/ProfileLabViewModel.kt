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

data class SavedConfig(
    val id: String,
    val name: String,
    val timestamp: Long,
    val selections: Map<String, ProfileSelection>,
    val deploy: DeployConfig,
    val generatedOutput: String
)

class ProfileLabViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val prefs = application.getSharedPreferences("hermes_profile_lab_prefs", android.content.Context.MODE_PRIVATE)

    val savedConfigs: MutableStateFlow<List<SavedConfig>> = MutableStateFlow(emptyList())

    private val _catalog = MutableStateFlow<List<ProfileMeta>>(emptyList())
    val catalog: StateFlow<List<ProfileMeta>> = _catalog

    val selected: MutableStateFlow<MutableMap<String, ProfileSelection>> = MutableStateFlow(mutableMapOf())
    val step: MutableStateFlow<Int> = MutableStateFlow(0)
    val deploy: MutableStateFlow<DeployConfig> = MutableStateFlow(DeployConfig())
    val generatedOutput: MutableStateFlow<String> = MutableStateFlow("")
    val copied: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val isGeneratingCustomSoul = MutableStateFlow(false)
    val customSoulGenerationError = MutableStateFlow<String?>(null)

    init {
        loadCatalog()
        loadPersistedState()
    }

    private fun loadCatalog() {
        val defaultCatalog = ProfileCatalog.load(app)
        val customProfilesJson = prefs.getString("custom_profiles", null)
        val customList = mutableListOf<ProfileMeta>()
        if (customProfilesJson != null) {
            try {
                val arr = org.json.JSONArray(customProfilesJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.getString("id")
                    val displayName = obj.getString("displayName")
                    val tagline = obj.getString("tagline")
                    val emoji = obj.getString("emoji")
                    val accent = obj.getString("accent")
                    val defaultProvider = obj.getString("defaultProvider")
                    val defaultModel = obj.getString("defaultModel")
                    val customAvatar = if (obj.has("customAvatar")) obj.getString("customAvatar") else null
                    val tagsArr = obj.optJSONArray("tags")
                    val tags = if (tagsArr != null) {
                        (0 until tagsArr.length()).map { tagsArr.getString(it) }
                    } else emptyList()
                    
                    customList.add(
                        ProfileMeta(
                            id = id,
                            displayName = displayName,
                            tagline = tagline,
                            emoji = emoji,
                            accent = accent,
                            defaultProvider = defaultProvider,
                            defaultModel = defaultModel,
                            tags = tags,
                            customAvatar = customAvatar
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _catalog.value = defaultCatalog + customList
    }

    fun addCustomProfile(meta: ProfileMeta, soulText: String) {
        // Save soul
        prefs.edit().putString("custom_soul_${meta.id}", soulText).apply()
        
        // Load existing custom profiles
        val customProfilesJson = prefs.getString("custom_profiles", "[]") ?: "[]"
        try {
            val arr = org.json.JSONArray(customProfilesJson)
            
            // Check if already exists to prevent duplicates
            var exists = false
            for (i in 0 until arr.length()) {
                if (arr.getJSONObject(i).getString("id") == meta.id) {
                    exists = true
                    break
                }
            }
            if (!exists) {
                val obj = org.json.JSONObject()
                obj.put("id", meta.id)
                obj.put("displayName", meta.displayName)
                obj.put("tagline", meta.tagline)
                obj.put("emoji", meta.emoji)
                obj.put("accent", meta.accent)
                obj.put("defaultProvider", meta.defaultProvider)
                obj.put("defaultModel", meta.defaultModel)
                if (meta.customAvatar != null) {
                    obj.put("customAvatar", meta.customAvatar)
                }
                val tagsArr = org.json.JSONArray()
                meta.tags.forEach { tagsArr.put(it) }
                obj.put("tags", tagsArr)
                
                arr.put(obj)
                prefs.edit().putString("custom_profiles", arr.toString()).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Reload catalog
        loadCatalog()
        
        // Automatically select the new profile
        toggleProfile(meta)
    }

    fun generateCustomSoulProfile(
        name: String,
        goal: String,
        traits: String,
        domain: String,
        tone: String,
        signature: String,
        emoji: String,
        customKey: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isGeneratingCustomSoul.value = true
            customSoulGenerationError.value = null
            try {
                val soulText = com.example.api.GeminiClient.generateCustomSoul(
                    name = name,
                    goal = goal,
                    traits = traits,
                    domain = domain,
                    tone = tone,
                    signature = signature,
                    customKey = customKey
                )
                if (soulText.startsWith("Error")) {
                    customSoulGenerationError.value = soulText
                } else {
                    // Create dynamic profile meta
                    val cleanId = name.lowercase().replace(Regex("[^a-z0-9_]"), "_").trim('_')
                    val id = "custom_${cleanId}_${System.currentTimeMillis() % 100000}"
                    val tagline = "Agente personalizzato: " + traits.take(40) + "..."
                    
                    // Pick a deterministically unique custom avatar index based on name hash
                    val avatarIndex = (Math.abs(name.hashCode()) % 4) + 1
                    val customAvatar = "img_custom_avatar_$avatarIndex"
                    
                    val meta = ProfileMeta(
                        id = id,
                        displayName = name,
                        tagline = tagline,
                        emoji = emoji.ifBlank { "✨" },
                        accent = "ElectricCyan",
                        defaultProvider = "gemini",
                        defaultModel = "gemini-2.5-flash",
                        tags = listOf("custom", "ai-generated"),
                        customAvatar = customAvatar
                    )
                    addCustomProfile(meta, soulText)
                    onSuccess()
                }
            } catch (e: Exception) {
                customSoulGenerationError.value = "Error: ${e.message}"
            } finally {
                isGeneratingCustomSoul.value = false
            }
        }
    }

    fun getSoul(id: String): String {
        val customSoul = prefs.getString("custom_soul_$id", null)
        if (customSoul != null) return customSoul
        return ProfileCatalog.soul(app, id)
    }

    private fun saveSelected() {
        val jsonArray = org.json.JSONArray()
        selected.value.values.forEach { sel ->
            val obj = org.json.JSONObject()
            obj.put("id", sel.id)
            obj.put("displayName", sel.displayName)
            obj.put("provider", sel.provider)
            obj.put("model", sel.model)
            obj.put("apiKey", sel.apiKey)
            obj.put("baseUrl", sel.baseUrl)
            jsonArray.put(obj)
        }
        prefs.edit().putString("selected_profiles", jsonArray.toString()).apply()
    }

    private fun loadPersistedState() {
        step.value = prefs.getInt("active_step", 0)

        deploy.value = DeployConfig(
            mode = prefs.getString("deploy_mode", "cli") ?: "cli",
            botToken = prefs.getString("deploy_bot_token", "") ?: "",
            sshHost = prefs.getString("deploy_ssh_host", "") ?: "",
            sshUser = prefs.getString("deploy_ssh_user", "") ?: ""
        )

        val selJson = prefs.getString("selected_profiles", null)
        val map = mutableMapOf<String, ProfileSelection>()
        if (selJson != null) {
            try {
                val jsonArray = org.json.JSONArray(selJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.getString("id")
                    val sel = ProfileSelection(
                        id = id,
                        displayName = obj.getString("displayName"),
                        provider = obj.getString("provider"),
                        model = obj.getString("model"),
                        apiKey = obj.getString("apiKey"),
                        baseUrl = obj.getString("baseUrl")
                    )
                    map[id] = sel
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        selected.value = map
        generatedOutput.value = prefs.getString("generated_output", "") ?: ""
        loadSavedConfigs()
    }

    private fun loadSavedConfigs() {
        val savedJson = prefs.getString("saved_configs", null)
        val list = mutableListOf<SavedConfig>()
        if (savedJson != null) {
            try {
                val arr = org.json.JSONArray(savedJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.getString("id")
                    val name = obj.getString("name")
                    val timestamp = obj.getLong("timestamp")
                    val generatedOutput = obj.optString("generatedOutput", "")
                    
                    val depObj = obj.optJSONObject("deploy")
                    val deploy = if (depObj != null) {
                        DeployConfig(
                            mode = depObj.optString("mode", "cli"),
                            botToken = depObj.optString("botToken", ""),
                            sshHost = depObj.optString("sshHost", ""),
                            sshUser = depObj.optString("sshUser", "")
                        )
                    } else DeployConfig()

                    val selArr = obj.getJSONArray("selections")
                    val selections = mutableMapOf<String, ProfileSelection>()
                    for (j in 0 until selArr.length()) {
                        val sObj = selArr.getJSONObject(j)
                        val selId = sObj.getString("id")
                        selections[selId] = ProfileSelection(
                            id = selId,
                            displayName = sObj.getString("displayName"),
                            provider = sObj.getString("provider"),
                            model = sObj.getString("model"),
                            apiKey = sObj.getString("apiKey"),
                            baseUrl = sObj.getString("baseUrl")
                        )
                    }
                    list.add(SavedConfig(id, name, timestamp, selections, deploy, generatedOutput))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        savedConfigs.value = list
    }

    private fun saveSavedConfigs() {
        val arr = org.json.JSONArray()
        savedConfigs.value.forEach { config ->
            val obj = org.json.JSONObject()
            obj.put("id", config.id)
            obj.put("name", config.name)
            obj.put("timestamp", config.timestamp)
            obj.put("generatedOutput", config.generatedOutput)
            
            val depObj = org.json.JSONObject()
            depObj.put("mode", config.deploy.mode)
            depObj.put("botToken", config.deploy.botToken)
            depObj.put("sshHost", config.deploy.sshHost)
            depObj.put("sshUser", config.deploy.sshUser)
            obj.put("deploy", depObj)

            val selArr = org.json.JSONArray()
            config.selections.values.forEach { sel ->
                val sObj = org.json.JSONObject()
                sObj.put("id", sel.id)
                sObj.put("displayName", sel.displayName)
                sObj.put("provider", sel.provider)
                sObj.put("model", sel.model)
                sObj.put("apiKey", sel.apiKey)
                sObj.put("baseUrl", sel.baseUrl)
                selArr.put(sObj)
            }
            obj.put("selections", selArr)
            arr.put(obj)
        }
        prefs.edit().putString("saved_configs", arr.toString()).apply()
    }

    fun addCurrentConfigToSaved(customName: String? = null) {
        val sel = selected.value
        if (sel.isEmpty()) return
        
        val timestamp = System.currentTimeMillis()
        val defaultName = "Config " + sel.values.joinToString(", ") { it.displayName } + " - " + 
                java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
        val name = customName?.ifBlank { null } ?: defaultName
        
        val newConfig = SavedConfig(
            id = "config_${timestamp}",
            name = name,
            timestamp = timestamp,
            selections = sel.toMap(),
            deploy = deploy.value,
            generatedOutput = generatedOutput.value
        )
        
        val curList = savedConfigs.value.toMutableList()
        val alreadyExists = curList.firstOrNull()?.let {
            it.selections == newConfig.selections && it.deploy == newConfig.deploy && it.generatedOutput == newConfig.generatedOutput
        } ?: false
        if (!alreadyExists) {
            curList.add(0, newConfig)
            savedConfigs.value = curList
            saveSavedConfigs()
        }
    }

    fun deleteSavedConfig(id: String) {
        val curList = savedConfigs.value.filter { it.id != id }
        savedConfigs.value = curList
        saveSavedConfigs()
    }

    fun loadSavedConfig(config: SavedConfig) {
        selected.value = config.selections.toMutableMap()
        saveSelected()
        deploy.value = config.deploy
        prefs.edit().apply {
            putString("deploy_mode", config.deploy.mode)
            putString("deploy_bot_token", config.deploy.botToken)
            putString("deploy_ssh_host", config.deploy.sshHost)
            putString("deploy_ssh_user", config.deploy.sshUser)
        }.apply()
        generatedOutput.value = config.generatedOutput
        prefs.edit().putString("generated_output", config.generatedOutput).apply()
        step.value = 2
        prefs.edit().putInt("active_step", 2).apply()
    }

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
        saveSelected()
    }

    fun updateSelection(id: String, provider: String, model: String, apiKey: String, baseUrl: String) {
        val cur = selected.value.toMutableMap()
        val existing = cur[id] ?: return
        cur[id] = existing.copy(provider = provider, model = model, apiKey = apiKey, baseUrl = baseUrl)
        selected.value = cur
        saveSelected()
    }

    fun setStep(s: Int) { 
        step.value = s 
        prefs.edit().putInt("active_step", s).apply()
    }

    fun setDeploy(d: DeployConfig) { 
        deploy.value = d 
        prefs.edit().apply {
            putString("deploy_mode", d.mode)
            putString("deploy_bot_token", d.botToken)
            putString("deploy_ssh_host", d.sshHost)
            putString("deploy_ssh_user", d.sshUser)
        }.apply()
    }

    fun markCopied() { copied.value = true }

    /** Assembla l'output reale: per ogni profilo scelto genera SOUL + config + deploy script. */
    fun generate() {
        viewModelScope.launch(Dispatchers.Default) {
            val sel = selected.value.values.toList()
            val sb = StringBuilder()
            sel.forEach { s ->
                val soul = getSoul(s.id)
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
            prefs.edit().putString("generated_output", sb.toString()).apply()
            withContext(Dispatchers.Main) {
                addCurrentConfigToSaved()
            }
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
