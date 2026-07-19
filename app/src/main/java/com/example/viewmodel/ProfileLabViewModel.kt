package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.data.AppDatabase
import com.example.data.ProfileTemplate
import com.example.data.ProfileTemplates
import com.example.data.SavedProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileLabViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.savedProfileDao()

    // Screen navigation state: "templates", "workspace", "history"
    private val _currentTab = MutableStateFlow("templates")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Saved profiles list from Room
    private val _savedProfiles = MutableStateFlow<List<SavedProfile>>(emptyList())
    val savedProfiles: StateFlow<List<SavedProfile>> = _savedProfiles.asStateFlow()

    // Configurator state
    private val _selectedTemplate = MutableStateFlow<ProfileTemplate?>(null)
    val selectedTemplate: StateFlow<ProfileTemplate?> = _selectedTemplate.asStateFlow()

    private val _provider = MutableStateFlow("OpenAI")
    val provider: StateFlow<String> = _provider.asStateFlow()

    private val _model = MutableStateFlow("openai/gpt-4o")
    val model: StateFlow<String> = _model.asStateFlow()

    private val _maxTokens = MutableStateFlow(8192)
    val maxTokens: StateFlow<Int> = _maxTokens.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _deployMode = MutableStateFlow("cli") // "cli", "telegram", "ssh"
    val deployMode: StateFlow<String> = _deployMode.asStateFlow()

    private val _telegramToken = MutableStateFlow("")
    val telegramToken: StateFlow<String> = _telegramToken.asStateFlow()

    private val _vpsUser = MutableStateFlow("root")
    val vpsUser: StateFlow<String> = _vpsUser.asStateFlow()

    private val _vpsHost = MutableStateFlow("192.168.1.1")
    val vpsHost: StateFlow<String> = _vpsHost.asStateFlow()

    private val _vpsPath = MutableStateFlow("/root/.hermes/profiles")
    val vpsPath: StateFlow<String> = _vpsPath.asStateFlow()

    // Manual or Gemini-customized prompts
    private val _customSoulPrompt = MutableStateFlow("")
    val customSoulPrompt: StateFlow<String> = _customSoulPrompt.asStateFlow()

    // Generated outputs
    private val _generatedConfigYaml = MutableStateFlow("")
    val generatedConfigYaml: StateFlow<String> = _generatedConfigYaml.asStateFlow()

    private val _generatedInstallScript = MutableStateFlow("")
    val generatedInstallScript: StateFlow<String> = _generatedInstallScript.asStateFlow()

    // Refinement playground state
    private val _refinementRequest = MutableStateFlow("")
    val refinementRequest: StateFlow<String> = _refinementRequest.asStateFlow()

    // Status and Loading indicators
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        // Load saved deployments from database
        loadSavedProfiles()
        
        // Select Frank as default
        selectTemplate(ProfileTemplates.templates.first())
    }

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    private fun loadSavedProfiles() {
        viewModelScope.launch {
            dao.getAllSavedProfiles().collect { list ->
                _savedProfiles.value = list
            }
        }
    }

    fun selectTemplate(template: ProfileTemplate) {
        _selectedTemplate.value = template
        _customSoulPrompt.value = template.soulPrompt
        
        // Load default model and provider recommendations
        _provider.value = template.recommendedProvider
        _model.value = template.recommendedModel
        
        // Adjust default VPS path
        _vpsPath.value = "/root/.hermes/profiles/${template.key}"
        
        generateFiles()
    }

    fun setProvider(p: String) {
        _provider.value = p
        // Update recommended models depending on provider
        when (p) {
            "OpenAI" -> _model.value = "openai/gpt-4o-mini"
            "Anthropic" -> _model.value = "anthropic/claude-3-5-sonnet"
            "DeepSeek" -> _model.value = "deepseek-chat"
            "Gemini" -> _model.value = "google/gemini-2.5-pro"
            "Venice/GLM" -> _model.value = "venice/glm-4"
            else -> _model.value = "custom-model"
        }
        generateFiles()
    }

    fun setModel(m: String) {
        _model.value = m
        generateFiles()
    }

    fun setMaxTokens(tokens: Int) {
        _maxTokens.value = tokens
        generateFiles()
    }

    fun setApiKey(key: String) {
        _apiKey.value = key
        generateFiles()
    }

    fun setDeployMode(mode: String) {
        _deployMode.value = mode
        generateFiles()
    }

    fun setTelegramToken(token: String) {
        _telegramToken.value = token
        generateFiles()
    }

    fun setVpsUser(user: String) {
        _vpsUser.value = user
        generateFiles()
    }

    fun setVpsHost(host: String) {
        _vpsHost.value = host
        generateFiles()
    }

    fun setVpsPath(path: String) {
        _vpsPath.value = path
        generateFiles()
    }

    fun setRefinementRequest(req: String) {
        _refinementRequest.value = req
    }

    fun updateSoulPromptManually(prompt: String) {
        _customSoulPrompt.value = prompt
        generateFiles()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * Generate `config.yaml` and installation scripts based on inputs.
     */
    fun generateFiles() {
        val template = _selectedTemplate.value ?: return
        val key = template.key
        val provName = "${key}-provider"
        
        // Build config.yaml string
        val apiKeyLine = if (_apiKey.value.isNotBlank()) {
            "api_key: \"${_apiKey.value}\""
        } else {
            "# api_key: inserisci la tua API key qui"
        }

        val baseUrlLine = if (_provider.value == "Venice/GLM") {
            "\n    base_url: \"https://api.venice.ai/public/v1\""
        } else if (_provider.value == "DeepSeek") {
            "\n    base_url: \"https://api.deepseek.com\""
        } else if (_provider.value == "Custom") {
            "\n    base_url: \"https://api.custom-endpoint.com/v1\""
        } else ""

        val configYaml = """
# Hermes profile: $key
# Generato da HermesBro Profile Lab
model:
  default: ${_model.value}
  provider: $provName
  api_mode: openai
  max_tokens: ${_maxTokens.value}

providers:
  $provName:
    type: openai
    $apiKeyLine
    model: ${_model.value}
    api_mode: openai
    max_tokens: ${_maxTokens.value}
    priority: 1$baseUrlLine

fallback_providers:
  - $provName
""".trimIndent()

        _generatedConfigYaml.value = configYaml

        // Build install.sh script
        val script = when (_deployMode.value) {
            "cli" -> {
                """
#!/usr/bin/env bash
set -euo pipefail

PROFILE_DIR="${'$'}HOME/.hermes/profiles/$key"
mkdir -p "${'$'}PROFILE_DIR"

echo "Scrittura di SOUL.md..."
cat > "${'$'}PROFILE_DIR/SOUL.md" <<'SOUL_EOF'
${_customSoulPrompt.value}
SOUL_EOF

echo "Scrittura di config.yaml..."
cat > "${'$'}PROFILE_DIR/config.yaml" <<'CFG_EOF'
$configYaml
CFG_EOF

echo "----------------------------------------"
echo "Profilo $key creato con successo in: ${'$'}PROFILE_DIR"
echo "Per avviarlo via CLI, esegui:"
echo "  hermes -p $key"
""".trimIndent()
            }
            "telegram" -> {
                val tok = if (_telegramToken.value.isNotBlank()) _telegramToken.value else "TUO_TELEGRAM_TOKEN_QUI"
                """
#!/usr/bin/env bash
set -euo pipefail

PROFILE_DIR="${'$'}HOME/.hermes/profiles/$key"
mkdir -p "${'$'}PROFILE_DIR"

echo "Scrittura di SOUL.md..."
cat > "${'$'}PROFILE_DIR/SOUL.md" <<'SOUL_EOF'
${_customSoulPrompt.value}
SOUL_EOF

echo "Scrittura di config.yaml..."
cat > "${'$'}PROFILE_DIR/config.yaml" <<'CFG_EOF'
$configYaml
CFG_EOF

echo "Configurazione del gateway Telegram..."
hermes gateway setup --platform telegram --token "$tok"

echo "----------------------------------------"
echo "Profilo $key creato in ${'$'}PROFILE_DIR ed associato a Telegram!"
echo "Per avviare il gateway in background, esegui:"
echo "  hermes gateway run --profile $key"
""".trimIndent()
            }
            "ssh" -> {
                val user = _vpsUser.value
                val host = _vpsHost.value
                val path = _vpsPath.value
                """
#!/usr/bin/env bash
set -euo pipefail

REMOTE_USER="$user"
REMOTE_HOST="$host"
REMOTE_PATH="$path"

echo "Creazione della directory sul server remoto $host..."
ssh "${'$'}REMOTE_USER@${'$'}REMOTE_HOST" "mkdir -p \"${'$'}REMOTE_PATH\""

echo "Scrittura di SOUL.md sul server remoto..."
ssh "${'$'}REMOTE_USER@${'$'}REMOTE_HOST" "cat > \"${'$'}REMOTE_PATH/SOUL.md\"" <<'SOUL_EOF'
${_customSoulPrompt.value}
SOUL_EOF

echo "Scrittura di config.yaml sul server remoto..."
ssh "${'$'}REMOTE_USER@${'$'}REMOTE_HOST" "cat > \"${'$'}REMOTE_PATH/config.yaml\"" <<'CFG_EOF'
$configYaml
CFG_EOF

echo "----------------------------------------"
echo "Profilo $key caricato su ${'$'}REMOTE_USER@${'$'}REMOTE_HOST:${'$'}REMOTE_PATH"
echo "Puoi avviarlo sul server eseguendo:"
echo "  ssh ${'$'}REMOTE_USER@${'$'}REMOTE_HOST 'hermes -p $key'"
""".trimIndent()
            }
            else -> ""
        }

        _generatedInstallScript.value = script
    }

    /**
     * Call Gemini to refine the current soul prompt.
     */
    fun refineSoulWithGemini() {
        val req = _refinementRequest.value
        if (req.isBlank()) {
            _statusMessage.value = "Per favore inserisci una richiesta di personalizzazione!"
            return
        }

        _isLoading.value = true
        _statusMessage.value = "Chiamata a Gemini per personalizzare l'anima..."

        viewModelScope.launch {
            try {
                val result = GeminiClient.refineSoulPrompt(
                    originalPrompt = _customSoulPrompt.value,
                    refinementRequest = req,
                    customKey = if (_apiKey.value.startsWith("AI_") || _apiKey.value.length > 20) _apiKey.value else null
                )

                if (result.startsWith("Error")) {
                    _statusMessage.value = result
                } else {
                    _customSoulPrompt.value = result
                    generateFiles()
                    _refinementRequest.value = ""
                    _statusMessage.value = "Anima personalizzata con successo con l'IA!"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Errore durante la raffinazione: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Save the compiled profile deployment to local database.
     */
    fun saveDeploymentToHistory() {
        val template = _selectedTemplate.value ?: return
        
        _isLoading.value = true
        _statusMessage.value = "Salvataggio nel database locale..."

        viewModelScope.launch {
            try {
                val profile = SavedProfile(
                    name = "${template.name} - ${_deployMode.value.uppercase()}",
                    profileKey = template.key,
                    provider = _provider.value,
                    model = _model.value,
                    maxTokens = _maxTokens.value,
                    apiKey = _apiKey.value,
                    deployMode = _deployMode.value,
                    telegramToken = _telegramToken.value,
                    vpsUser = _vpsUser.value,
                    vpsHost = _vpsHost.value,
                    vpsPath = _vpsPath.value,
                    soulPrompt = _customSoulPrompt.value,
                    configYaml = _generatedConfigYaml.value,
                    installScript = _generatedInstallScript.value
                )

                dao.insertProfile(profile)
                _statusMessage.value = "Configurazione salvata con successo in Deployments!"
                _currentTab.value = "history" // Redirect to history tab to view saved profile
            } catch (e: Exception) {
                _statusMessage.value = "Errore durante il salvataggio: ${e.message}"
            } finally {
                _isLoading.value = false
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
