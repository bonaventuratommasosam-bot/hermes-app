package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SavedProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileLabViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.savedProfileDao()

    // 1. Templates list loaded from Assets
    private val _templates = MutableStateFlow<List<ProfileTemplate>>(emptyList())
    val templates: StateFlow<List<ProfileTemplate>> = _templates.asStateFlow()

    // 2. Wizard navigation step: 1, 2, 3
    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    // 3. Multi-select: Selected template keys
    private val _selectedProfileKeys = MutableStateFlow<Set<String>>(setOf("frank"))
    val selectedProfileKeys: StateFlow<Set<String>> = _selectedProfileKeys.asStateFlow()

    // Active profile in preview (Step 3)
    private val _activePreviewKey = MutableStateFlow("frank")
    val activePreviewKey: StateFlow<String> = _activePreviewKey.asStateFlow()

    // 4. Provider and Credentials State
    private val _provider = MutableStateFlow("OpenAI")
    val provider: StateFlow<String> = _provider.asStateFlow()

    private val _customBaseUrl = MutableStateFlow("")
    val customBaseUrl: StateFlow<String> = _customBaseUrl.asStateFlow()

    private val _model = MutableStateFlow("gpt-4o-mini")
    val model: StateFlow<String> = _model.asStateFlow()

    private val _maxTokens = MutableStateFlow(8192)
    val maxTokens: StateFlow<Int> = _maxTokens.asStateFlow()

    // Standard string, non-corrupted apiKey
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    // 5. Deploy mode State: "cli", "telegram", "ssh"
    private val _deployMode = MutableStateFlow("cli")
    val deployMode: StateFlow<String> = _deployMode.asStateFlow()

    private val _telegramToken = MutableStateFlow("")
    val telegramToken: StateFlow<String> = _telegramToken.asStateFlow()

    private val _vpsUser = MutableStateFlow("root")
    val vpsUser: StateFlow<String> = _vpsUser.asStateFlow()

    private val _vpsHost = MutableStateFlow("192.168.1.1")
    val vpsHost: StateFlow<String> = _vpsHost.asStateFlow()

    private val _vpsPath = MutableStateFlow("")
    val vpsPath: StateFlow<String> = _vpsPath.asStateFlow()

    // Generated files state
    private val _generatedConfigYaml = MutableStateFlow("")
    val generatedConfigYaml: StateFlow<String> = _generatedConfigYaml.asStateFlow()

    private val _generatedInstallScript = MutableStateFlow("")
    val generatedInstallScript: StateFlow<String> = _generatedInstallScript.asStateFlow()

    // History list from database
    private val _savedProfiles = MutableStateFlow<List<SavedProfile>>(emptyList())
    val savedProfiles: StateFlow<List<SavedProfile>> = _savedProfiles.asStateFlow()

    // Status / feedback channel
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        // Load the catalog templates
        val loaded = ProfileCatalog.loadTemplates(application)
        _templates.value = loaded
        
        // Load history from local Room database
        loadHistory()
        
        // Trigger initial configuration file updates
        updateGeneratedOutputs()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            dao.getAllSavedProfiles().collect { list ->
                _savedProfiles.value = list
            }
        }
    }

    fun setStep(step: Int) {
        if (step in 1..3) {
            _currentStep.value = step
            updateGeneratedOutputs()
        }
    }

    fun toggleProfileSelected(key: String) {
        val current = _selectedProfileKeys.value.toMutableSet()
        if (current.contains(key)) {
            // Keep at least one selected so preview/configuration works
            if (current.size > 1) {
                current.remove(key)
            }
        } else {
            current.add(key)
        }
        _selectedProfileKeys.value = current
        
        // Ensure active preview key is valid
        if (!current.contains(_activePreviewKey.value)) {
            _activePreviewKey.value = current.firstOrNull() ?: ""
        }
        updateGeneratedOutputs()
    }

    fun setActivePreviewKey(key: String) {
        if (_selectedProfileKeys.value.contains(key)) {
            _activePreviewKey.value = key
            updateGeneratedOutputs()
        }
    }

    fun setProvider(p: String) {
        _provider.value = p
        // Update default model depending on provider
        when (p) {
            "OpenAI" -> {
                _model.value = "gpt-4o-mini"
                _customBaseUrl.value = ""
            }
            "Anthropic" -> {
                _model.value = "claude-3-5-sonnet"
                _customBaseUrl.value = ""
            }
            "DeepSeek" -> {
                _model.value = "deepseek-chat"
                _customBaseUrl.value = "https://api.deepseek.com"
            }
            "Gemini" -> {
                _model.value = "gemini-2.5-pro"
                _customBaseUrl.value = "https://generativelanguage.googleapis.com/v1beta/openai"
            }
            "Venice/GLM" -> {
                _model.value = "glm-4"
                _customBaseUrl.value = "https://api.venice.ai/public/v1"
            }
            "Custom" -> {
                _model.value = "custom-model"
                // Keep previous base URL or empty to force input
            }
        }
        updateGeneratedOutputs()
    }

    fun setCustomBaseUrl(url: String) {
        _customBaseUrl.value = url
        updateGeneratedOutputs()
    }

    fun setModel(m: String) {
        _model.value = m
        updateGeneratedOutputs()
    }

    fun setMaxTokens(tokens: Int) {
        _maxTokens.value = tokens
        updateGeneratedOutputs()
    }

    fun setApiKey(key: String) {
        _apiKey.value = key
        updateGeneratedOutputs()
    }

    fun setDeployMode(mode: String) {
        _deployMode.value = mode
        updateGeneratedOutputs()
    }

    fun setTelegramToken(token: String) {
        _telegramToken.value = token
        updateGeneratedOutputs()
    }

    fun setVpsUser(user: String) {
        _vpsUser.value = user
        updateGeneratedOutputs()
    }

    fun setVpsHost(host: String) {
        _vpsHost.value = host
        updateGeneratedOutputs()
    }

    fun setVpsPath(path: String) {
        _vpsPath.value = path
        updateGeneratedOutputs()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showStatus(msg: String) {
        _statusMessage.value = msg
    }

    /**
     * Re-build generated config and installation scripts based on active selected profile and setup details.
     */
    fun updateGeneratedOutputs() {
        val activeKey = _activePreviewKey.value
        val template = _templates.value.find { it.key == activeKey } ?: return
        
        // Use custom base URL if Custom, or fallback to default rules in ConfigBuilder
        val finalYaml = ConfigBuilder.buildConfigYaml(
            profileKey = activeKey,
            providerName = _provider.value,
            modelName = _model.value,
            maxTokens = _maxTokens.value,
            apiKey = _apiKey.value
        )
        
        // If it's a custom/special provider with explicit customBaseUrl, append/override inside yaml if custom
        val processedYaml = if (_provider.value == "Custom" && _customBaseUrl.value.isNotBlank()) {
            // Append base_url to the active provider configuration in YAML
            if (!finalYaml.contains("base_url:")) {
                finalYaml.replace("priority: 1", "priority: 1\n    base_url: \"${_customBaseUrl.value}\"")
            } else {
                finalYaml
            }
        } else {
            finalYaml
        }

        _generatedConfigYaml.value = processedYaml

        val path = if (_vpsPath.value.isBlank()) "/root/.hermes/profiles/$activeKey" else _vpsPath.value

        _generatedInstallScript.value = ConfigBuilder.buildInstallScript(
            profileKey = activeKey,
            soulPrompt = template.soulPrompt,
            configYaml = processedYaml,
            deployMode = _deployMode.value,
            telegramToken = _telegramToken.value,
            vpsUser = _vpsUser.value,
            vpsHost = _vpsHost.value,
            vpsPath = path
        )
    }

    /**
     * Save the active deployment to history database.
     */
    fun saveDeploymentToHistory() {
        val activeKey = _activePreviewKey.value
        val template = _templates.value.find { it.key == activeKey } ?: return

        viewModelScope.launch {
            try {
                val profile = SavedProfile(
                    name = "${template.name} - ${_deployMode.value.uppercase()}",
                    profileKey = activeKey,
                    provider = _provider.value,
                    model = _model.value,
                    maxTokens = _maxTokens.value,
                    apiKey = _apiKey.value,
                    deployMode = _deployMode.value,
                    telegramToken = _telegramToken.value,
                    vpsUser = _vpsUser.value,
                    vpsHost = _vpsHost.value,
                    vpsPath = _vpsPath.value,
                    soulPrompt = template.soulPrompt,
                    configYaml = _generatedConfigYaml.value,
                    installScript = _generatedInstallScript.value
                )

                dao.insertProfile(profile)
                _statusMessage.value = "Configurazione salvata con successo in Deployments!"
            } catch (e: Exception) {
                _statusMessage.value = "Errore durante il salvataggio: ${e.message}"
            }
        }
    }

    /**
     * Delete a saved profile from database.
     */
    fun deleteSavedProfile(id: Int) {
        viewModelScope.launch {
            try {
                dao.deleteProfileById(id)
                _statusMessage.value = "Deployment rimosso con successo."
            } catch (e: Exception) {
                _statusMessage.value = "Errore durante l'eliminazione: ${e.message}"
            }
        }
    }
}
