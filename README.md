# Hermes App — HermesBro Profile Lab

App che genera profili **Hermes Agent** personalizzati (CLI/Telegram) e gli
script di installazione reali. Due facce dello stesso motore:

- **`app/`** — app Android (Kotlin + Jetpack Compose). Wizard a 3 step:
  Catalogo profili → Provider+API key → Deploy (CLI/Telegram/SSH).
  Compilata → `app/build/outputs/apk/debug/app-debug.apk`.
- **`ai-studio/`** — agente conversazionale per Google AI Studio. Stesse
  regole, stessa generazione di `config.yaml` + script `hermes`.

Regola d'acciaio: **mai dati simulati, mai metriche fittizie.** I file generati
sono reali e usabili.

## Struttura
```
app/                      progetto Android (wizard nativo)
ai-studio/
  system_instruction.md   istruzioni di sistema dell'agente (da incollare in AI Studio)
  profiles/<id>/SOUL.md   8 profili reali (frank, h2bb, study, study-glm,
                          trader, cuoco, devops, content)
.gitignore                esclude build/, *.jks, debug.keystore, local.properties
```

## Lavoro condiviso (Hermes + te + AI Studio)
La fonte di verità è questo repo. AI Studio non fa sync automatico da GitHub:
quando Hermes aggiorna `ai-studio/`, tu re-importi in AI Studio (10 secondi).

### Re-import manuale (scelta attuale)
1. Apri l'app su https://ai.studio/apps
2. **System instructions** → incolla/aggiorna con `ai-studio/system_instruction.md`
3. **Context / Files** → carica/ricarica i `ai-studio/profiles/<id>/SOUL.md` che servono
4. Salva. Testa: *"profilo h2bb, provider deepseek, deploy ssh su 194.146.12.219"*

Quando Hermes modifica l'agente, lo segnala nel commit e tu ripeti i punti 2-3.

## Build Android (locale, no admin)
Toolchain in `C:\Users\pc\android-build` (Gradle 9.3.1 + Android SDK platform 36).
```bash
cd app
export ANDROID_HOME="$HOME/android-build/android-sdk"
./gradlew assembleDebug --no-daemon
# APK -> app/build/outputs/apk/debug/app-debug.apk
```
Il `debug.keystore` è già generato (password `android`) ma NON è nel repo.

## Generazione profili (formato)
L'agente e l'app producono sempre:
- `config.yaml` con `model.max_tokens`, `providers`, `fallback_providers`.
- script di deploy: `hermes -p <profile>` (CLI), `hermes gateway ...` (Telegram),
  `ssh ...` (server). Mai endpoint/token inventati.

## Sicurezza
Repo pubblico → **zero secret**: api_key va nei file generati solo se l'utente
la fornisce, mai hardcodata nei template. `*.jks`, `debug.keystore`,
`local.properties` sono in `.gitignore`.
