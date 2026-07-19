package com.example.ui

object ConfigBuilder {

    fun buildConfigYaml(
        profileKey: String,
        providerName: String, // e.g. "OpenAI", "Anthropic", "DeepSeek", "Gemini", "Venice/GLM", "Custom"
        modelName: String,    // nudo, e.g. "gpt-4o-mini", "claude-3-5-sonnet", etc.
        maxTokens: Int,
        apiKey: String
    ): String {
        val providerId = "${profileKey}-provider"
        
        // Ensure type is "anthropic" for anthropic provider, otherwise "openai"
        val type = if (providerName.equals("Anthropic", ignoreCase = true)) "anthropic" else "openai"

        // Rule: model.default and providers.*.model must use the format "provider/model"
        // E.g., frank-provider/anthropic/claude-3.5-sonnet or h2bb-provider/deepseek-chat
        val modelValue = "$providerId/$modelName"

        // Rule: api_key: only if provided by user; otherwise comment placeholder
        val apiKeyLine = if (apiKey.isNotBlank()) {
            "api_key: \"$apiKey\""
        } else {
            "# api_key: inserisci la tua key qui (o in .env)"
        }

        // Base URL configuration for known providers
        val baseUrlLine = when (providerName) {
            "DeepSeek" -> "\n    base_url: \"https://api.deepseek.com\""
            "Gemini" -> "\n    base_url: \"https://generativelanguage.googleapis.com/v1beta/openai\""
            "Venice/GLM" -> "\n    base_url: \"https://api.venice.ai/public/v1\""
            "Custom" -> "\n    base_url: \"https://api.custom-endpoint.com/v1\"" // Placeholder if custom, ViewModel will handle custom inputs
            else -> ""
        }

        return """
# Hermes profile: $profileKey
# Generato da HermesBro Profile Lab
model:
  default: $modelValue
  provider: $providerId
  api_mode: openai
  max_tokens: $maxTokens

providers:
  $providerId:
    type: $type
    $apiKeyLine
    model: $modelValue
    api_mode: openai
    max_tokens: $maxTokens
    priority: 1$baseUrlLine

fallback_providers:
  - $providerId
""".trimIndent()
    }

    fun buildInstallScript(
        profileKey: String,
        soulPrompt: String,
        configYaml: String,
        deployMode: String, // "cli", "telegram", "ssh"
        telegramToken: String = "",
        vpsUser: String = "root",
        vpsHost: String = "192.168.1.1",
        vpsPath: String = ""
    ): String {
        return when (deployMode.lowercase()) {
            "cli" -> {
                """
#!/usr/bin/env bash
set -euo pipefail

PROFILE_DIR="${'$'}HOME/.hermes/profiles/$profileKey"
mkdir -p "${'$'}PROFILE_DIR"

echo "Scrittura di SOUL.md..."
cat > "${'$'}PROFILE_DIR/SOUL.md" <<'SOUL_EOF'
$soulPrompt
SOUL_EOF

echo "Scrittura di config.yaml..."
cat > "${'$'}PROFILE_DIR/config.yaml" <<'CFG_EOF'
$configYaml
CFG_EOF

echo "----------------------------------------"
echo "Profilo $profileKey creato con successo in: ${'$'}PROFILE_DIR"
echo "Per avviarlo via CLI, esegui:"
echo "  hermes -p $profileKey"
""".trimIndent()
            }
            "telegram" -> {
                val tok = if (telegramToken.isNotBlank()) telegramToken else "<YOUR_TELEGRAM_TOKEN>"
                """
#!/usr/bin/env bash
set -euo pipefail

PROFILE_DIR="${'$'}HOME/.hermes/profiles/$profileKey"
mkdir -p "${'$'}PROFILE_DIR"

echo "Scrittura di SOUL.md..."
cat > "${'$'}PROFILE_DIR/SOUL.md" <<'SOUL_EOF'
$soulPrompt
SOUL_EOF

echo "Scrittura di config.yaml..."
cat > "${'$'}PROFILE_DIR/config.yaml" <<'CFG_EOF'
$configYaml
CFG_EOF

echo "Configurazione del gateway Telegram..."
hermes gateway setup --platform telegram --token "$tok"

echo "----------------------------------------"
echo "Profilo $profileKey creato in ${'$'}PROFILE_DIR ed associato a Telegram!"
echo "Per avviare il gateway in background, esegui:"
echo "  hermes gateway run --profile $profileKey"
""".trimIndent()
            }
            "ssh" -> {
                val targetPath = if (vpsPath.isNotBlank()) vpsPath else "${'$'}HOME/.hermes/profiles/$profileKey"
                """
#!/usr/bin/env bash
set -euo pipefail

REMOTE_USER="$vpsUser"
REMOTE_HOST="$vpsHost"
REMOTE_PATH="$targetPath"

echo "Creazione della directory sul server remoto $vpsHost..."
ssh "${'$'}REMOTE_USER@${'$'}REMOTE_HOST" "mkdir -p \"${'$'}REMOTE_PATH\""

echo "Scrittura di SOUL.md sul server remoto..."
ssh "${'$'}REMOTE_USER@${'$'}REMOTE_HOST" "cat > \"${'$'}REMOTE_PATH/SOUL.md\"" <<'SOUL_EOF'
$soulPrompt
SOUL_EOF

echo "Scrittura di config.yaml sul server remoto..."
ssh "${'$'}REMOTE_USER@${'$'}REMOTE_HOST" "cat > \"${'$'}REMOTE_PATH/config.yaml\"" <<'CFG_EOF'
$configYaml
CFG_EOF

echo "----------------------------------------"
echo "Profilo $profileKey caricato su ${'$'}REMOTE_USER@${'$'}REMOTE_HOST:${'$'}REMOTE_PATH"
echo "Puoi avviarlo sul server eseguendo:"
echo "  ssh ${'$'}REMOTE_USER@${'$'}REMOTE_HOST 'hermes -p $profileKey'"
""".trimIndent()
            }
            else -> ""
        }
    }
}
