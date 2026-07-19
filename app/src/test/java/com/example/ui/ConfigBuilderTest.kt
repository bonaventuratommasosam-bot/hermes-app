package com.example.ui

import org.junit.Assert.*
import org.junit.Test

/**
 * Test FUNZIONALE del core di HermesBro Profile Lab.
 * Verifica che la generazione di config.yaml e degli script di deploy
 * produca output REALE (nessun dato simulato), coerente con le regole
 * di hermes-profile-authoring.
 */
class ConfigBuilderTest {

    @Test
    fun `gemini preset uses active model and openai-compatible base url`() {
        val pc = ConfigBuilder.preset("gemini")
        assertEquals("gemini-2.5-flash", pc.model)
        assertTrue(pc.baseUrl.contains("generativelanguage.googleapis.com"))
        assertEquals("openai", pc.apiMode)
        assertEquals(8192, pc.maxTokens)
    }

    @Test
    fun `config yaml contains mandatory max_tokens and fallback_providers`() {
        val pc = ProviderConfig(
            provider = "openrouter",
            model = "anthropic/claude-3.5-sonnet",
            apiKey = "",
            apiMode = "openai",
            maxTokens = 8192
        )
        val yaml = ConfigBuilder.buildConfigYaml("frank", pc)
        // max_tokens presente sia a livello model che provider (regola obbligatoria)
        assertTrue("manca max_tokens nel model", yaml.contains("  max_tokens: 8192"))
        assertTrue("manca max_tokens nel provider", yaml.contains("    max_tokens: 8192"))
        // fallback_providers presente
        assertTrue("manca fallback_providers", yaml.contains("fallback_providers:"))
        assertTrue("fallback non punta al provider", yaml.contains("frank-provider"))
        // api_key NON hardcodata quando vuota
        assertTrue("api_key non deve essere hardcodata", yaml.contains("# api_key: inserisci la tua key qui"))
        // header onesto
        assertTrue(yaml.contains("Generato da HermesBro Profile Lab"))
    }

    @Test
    fun `cli deploy script contains real hermes commands`() {
        val pc = ConfigBuilder.preset("openrouter")
        val soul = "# FRANK — SOUL PROMPT\nTest soul"
        val cfg = ConfigBuilder.buildConfigYaml("frank", pc)
        val script = ConfigBuilder.buildCliDeployScript("frank", soul, cfg)
        // comandi reali, non finti
        assertTrue("manca mkdir profilo", script.contains(".hermes/profiles/frank"))
        assertTrue("manca scrittura SOUL.md", script.contains("SOUL.md"))
        assertTrue("manca scrittura config.yaml", script.contains("config.yaml"))
        assertTrue("manca lancio hermes -p", script.contains("hermes -p frank"))
        // lo soul finisce davvero nel file
        assertTrue("soul non incluso nello script", script.contains("Test soul"))
        // set -euo pipefail => script rigoroso
        assertTrue(script.contains("set -euo pipefail"))
    }

    @Test
    fun `ssh deploy script targets remote host with real commands`() {
        val pc = ConfigBuilder.preset("gemini")
        val cfg = ConfigBuilder.buildConfigYaml("study", pc)
        val script = ConfigBuilder.buildSshDeploy(
            host = "vmi3305875", user = "hermes-pc", profileId = "study",
            soul = "soul", configYaml = cfg
        )
        assertTrue(script.contains("hermes-pc@vmi3305875"))
        assertTrue(script.contains("ssh"))
        assertTrue(script.contains("hermes -p study"))
    }

    @Test
    fun `telegram step uses real gateway setup`() {
        val step = ConfigBuilder.buildTelegramStep("12345:ABCDE", "frank")
        assertTrue(step.contains("TELEGRAM_BOT_TOKEN=\"12345:ABCDE\""))
        assertTrue(step.contains("hermes gateway setup --platform telegram"))
        assertTrue(step.contains("hermes gateway run --profile frank"))
    }
}
