package com.example.ui

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * Builder di config.yaml Hermes — ONESTO.
 * Regole (da hermes-profile-authoring):
 *  - max_tokens OBBLIGATORIO sia in model che in providers
 *  - fallback_providers presente
 *  - l'API key va nel file dell'utente, MAI nel template distribuito
 *  - i provider custom richiedono base_url esplicito
 *
 * NOTA: negli script shell i "$" letterali sono scritti come ${'$'} perche'
 * dentro le raw string Kotlin altrimenti verrebbero interpretati come
 * interpolazione di variabili Kotlin (errore di compilazione).
 */
data class ProviderConfig(
    val provider: String,   // openrouter | gemini | deepseek | nous | custom
    val model: String,
    val apiKey: String,     // inserita dall'utente a runtime, non hardcodata
    val baseUrl: String = "",
    val apiMode: String = "openai",
    val maxTokens: Int = 8192
)

object ConfigBuilder {

    /** Preset provider → (apiMode, maxTokens, baseUrlHint). */
    fun preset(provider: String): ProviderConfig {
        return when (provider) {
            "openrouter" -> ProviderConfig("openrouter", "anthropic/claude-3.5-sonnet", "", apiMode = "openai", maxTokens = 8192)
            "gemini"     -> ProviderConfig("gemini", "gemini-2.5-flash", "", apiMode = "openai",
                                baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai", maxTokens = 8192)
            "deepseek"   -> ProviderConfig("deepseek", "deepseek-chat", "", apiMode = "openai",
                                baseUrl = "https://api.deepseek.com", maxTokens = 8192)
            "nous"       -> ProviderConfig("nous", "nvidia/nemotron-3-ultra:free", "", apiMode = "openai", maxTokens = 8192)
            "custom"     -> ProviderConfig("custom", "", "", apiMode = "openai", maxTokens = 8192)
            else         -> ProviderConfig(provider, "", "", apiMode = "openai", maxTokens = 8192)
        }
    }

    /** Genera il config.yaml completo per un singolo profilo. */
    fun buildConfigYaml(profileId: String, pc: ProviderConfig): String {
        val provName = "${profileId}-provider"
        val keyLine = if (pc.apiKey.isNotBlank()) "    api_key: ${pc.apiKey}\n" else "    # api_key: inserisci la tua key qui (o in .env)\n"
        val baseLine = if (pc.baseUrl.isNotBlank()) "    base_url: ${pc.baseUrl}\n" else ""
        val modelLine = pc.model.ifBlank { "TODO_MODEL" }
        val baseUrlModel = if (pc.baseUrl.isNotBlank()) "  base_url: ${pc.baseUrl}\n" else ""
        return """
# Hermes profile: $profileId
# Generato da HermesBro Profile Lab — config reale, nessun dato simulato.
model:
  default: $modelLine
  provider: $provName
  api_mode: ${pc.apiMode}
$baseUrlModel  max_tokens: ${pc.maxTokens}

providers:
  $provName:
    type: ${pc.apiMode}
$keyLine$baseLine    model: $modelLine
    api_mode: ${pc.apiMode}
    max_tokens: ${pc.maxTokens}
    priority: 1

fallback_providers:
  - $provName
""".trimIndent()
    }

    /**
     * Script di deploy CLI — comandi REALI per installare e lanciare il profilo.
     * Crea la cartella profilo, ci scrive SOUL.md + config.yaml, lancia hermes -p.
     */
    fun buildCliDeployScript(profileId: String, soul: String, configYaml: String): String {
        val soulEsc = soul.replace("`", "\\`")
        val cfgEsc = configYaml.replace("`", "\\`")
        val D = "\${'$'}"   // dollaro letterale per lo shell
        return """
#!/usr/bin/env bash
# Deploy Hermes profile: $profileId
# Generato da HermesBro Profile Lab
set -euo pipefail

PROFILE_DIR="${D}HOME/.hermes/profiles/$profileId"
mkdir -p "${D}PROFILE_DIR"

cat > "${D}PROFILE_DIR/SOUL.md" <<'SOUL_EOF'
$soulEsc
SOUL_EOF

cat > "${D}PROFILE_DIR/config.yaml" <<'CFG_EOF'
$cfgEsc
CFG_EOF

echo "Profilo $profileId creato in ${D}PROFILE_DIR"
echo "Avvialo con:  hermes -p $profileId"
""".trimIndent()
    }

    /** Step Telegram opzionale: collega il bot e lancia il gateway col profilo. */
    fun buildTelegramStep(botToken: String, profileId: String): String {
        val D = "\${'$'}"
        return """
# Step Telegram per il profilo $profileId
# 1) Salva il token (lato utente, mai nel repo)
export TELEGRAM_BOT_TOKEN="$botToken"

# 2) Configura la piattaforma Telegram
hermes gateway setup --platform telegram --token "$botToken"

# 3) Avvia il gateway vincolato al profilo
hermes gateway run --profile $profileId

# Verifica: scrivi /start al bot su Telegram.
""".trimIndent()
    }

    /** Deploy via SSH: trasferisce i file sul server e li installa remotamente. */
    fun buildSshDeploy(host: String, user: String, profileId: String, soul: String, configYaml: String): String {
        val soulEsc = soul.replace("`", "\\`")
        val cfgEsc = configYaml.replace("`", "\\`")
        val D = "\${'$'}"
        return """
#!/usr/bin/env bash
# Deploy remoto via SSH per il profilo $profileId
set -euo pipefail
REMOTE="$user@$host"
PROFILE_DIR="${D}HOME/.hermes/profiles/$profileId"

ssh "${D}REMOTE" "mkdir -p ${D}PROFILE_DIR"

ssh "${D}REMOTE" "cat > ${D}PROFILE_DIR/SOUL.md" <<'SOUL_EOF'
$soulEsc
SOUL_EOF

ssh "${D}REMOTE" "cat > ${D}PROFILE_DIR/config.yaml" <<'CFG_EOF'
$cfgEsc
CFG_EOF

echo "Profilo $profileId installato su ${D}REMOTE"
echo "Su quel server lancia:  hermes -p $profileId"
""".trimIndent()
    }

    /**
     * Corpo JSON per il Launcher VPS (endpoint /api/bot-launch/launch).
     * Nessun secret nell'app: il X-Launcher-Secret è iniettato da nginx lato server.
     * Se soul+config sono forniti, il VPS genera un profilo CUSTOM (es. "Crea con AI").
     */
    fun buildLaunchJson(
        profileId: String,
        botToken: String,
        userId: String,
        soul: String = "",
        configYaml: String = ""
    ): String {
        val obj = JSONObject()
        obj.put("profile", profileId)
        obj.put("telegram_token", botToken)
        obj.put("user_id", userId.ifBlank { "app" })
        if (soul.isNotBlank() && configYaml.isNotBlank()) {
            obj.put("soul", soul)
            obj.put("config", configYaml)
        }
        return obj.toString()
    }

    /**
     * Client reale verso il Launcher VPS.
     * POST {baseUrl}/api/bot-launch/launch  con il JSON di buildLaunchJson.
     * Ritorna la risposta (es. {"instance":"frank_app","status":"launched"}) o lancia eccezione con il messaggio di errore.
     */
    object LaunchClient {
        fun launch(
            baseUrl: String,
            profileId: String,
            botToken: String,
            userId: String,
            soul: String = "",
            configYaml: String = ""
        ): String {
            val url = "${baseUrl.trimEnd('/')}/api/bot-launch/launch"
            val body = buildLaunchJson(profileId, botToken, userId, soul, configYaml)
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 30000
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val resp = if (code in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
            }
            if (code !in 200..299) {
                // prova a estrarre il messaggio FastAPI {"detail":"..."}
                val msg = try {
                    JSONObject(resp).optString("detail", resp)
                } catch (_: Exception) { resp }
                throw Exception("Launcher HTTP $code: $msg")
            }
            return resp
        }
    }
}
