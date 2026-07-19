# Carica HermesBro Profile Lab su Google AI Studio

L'app è definita da due parti: le **istruzioni di sistema** (persona + regole + catalogo)
e i **profili SOUL.md** (il contenuto reale dei profili). AI Studio non importa progetti
Android: si costruisce un agente Gemini e si esporta ad Android da lì.

## Passi
1. Apri **https://ai.studio/apps** e crea una nuova app (o apri l'app esistente).
2. Incollа il contenuto di `system_instruction.md` nel campo **System instructions**.
3. Imposta il modello su `gemini-2.5-flash` (o `gemini-2.5-pro` se vuoi più qualità).
4. Carica i profili: nella sezione **Context** / **Files**, allega le cartelle in
   `../app/src/main/assets/profiles/<id>/SOUL.md` (frank, h2bb, study, study-glm,
   trader, cuoco, devops, content). L'agente li userà come contesto per scrivere i SOUL.
5. Salva. Testa: "voglio un profilo h2bb con deepseek, deploy ssh su 1.2.3.4".
6. Quando è ok, premi **Export** → **Android (Kotlin + Jetpack Compose)** per rigenerare
   l'APK direttamente da AI Studio (stavolta senza il layer di simulazione che avevamo
   rimosso).

## Nota onesta
L'APK che abbiamo già compilato (`app/build/outputs/apk/debug/app-debug.apk`) resta valido
ed è il fallback se l'export di AI Studio non ti convince. La differenza: lì il wizard è
UI nativa scritta da noi; in AI Studio il wizard è conversazionale (chat). Stesso motore
di generazione, interfaccia diversa.

## Sicurezza
- L'API key resta nella conversazione/sul file generato, mai hardcodata nei template.
- I SOUL.md allegati sono già **sanitizzati** (niente password/token/chat_id reali).
