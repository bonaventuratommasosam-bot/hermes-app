package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SavedProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileLabScreen(
    viewModel: ProfileLabViewModel,
    modifier: Modifier = Modifier
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val savedProfiles by viewModel.savedProfiles.collectAsState()

    // ClipboardManager defined here (outside click lambdas) to avoid composability errors
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "HERMESBRO",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "PROFILE LAB",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
        bottomBar = {
            // Wizard controller at the bottom
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    if (currentStep > 1) {
                        TextButton(
                            onClick = { viewModel.setStep(currentStep - 1) },
                            modifier = Modifier.testTag("btn_back")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Indietro")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    // Step Indicator
                    Text(
                        text = "Step $currentStep di 3",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Next button
                    if (currentStep < 3) {
                        Button(
                            onClick = { viewModel.setStep(currentStep + 1) },
                            modifier = Modifier.testTag("btn_next")
                        ) {
                            Text("Avanti")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Avanti")
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.saveDeploymentToHistory()
                            },
                            modifier = Modifier.testTag("btn_save_deployment")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Salva")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salva in History")
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Status bar alert / notification
            statusMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearStatusMessage() }) {
                            Icon(Icons.Default.Close, contentDescription = "Chiudi")
                        }
                    }
                }
            }

            // Wizard Step Navigation Tab row
            TabRow(selectedTabIndex = currentStep - 1) {
                Tab(
                    selected = currentStep == 1,
                    onClick = { viewModel.setStep(1) },
                    text = { Text("1. Catalogo", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) }
                )
                Tab(
                    selected = currentStep == 2,
                    onClick = { viewModel.setStep(2) },
                    text = { Text("2. Provider", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Key, contentDescription = null) }
                )
                Tab(
                    selected = currentStep == 3,
                    onClick = { viewModel.setStep(3) },
                    text = { Text("3. Deploy", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Send, contentDescription = null) }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentStep) {
                    1 -> StepCatalog(viewModel)
                    2 -> StepCredentials(viewModel)
                    3 -> StepDeploy(viewModel, clipboardManager)
                }
            }
        }
    }
}

@Composable
fun StepCatalog(viewModel: ProfileLabViewModel) {
    val templates by viewModel.templates.collectAsState()
    val selectedKeys by viewModel.selectedProfileKeys.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Seleziona le Anime per l'Agente",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Puoi selezionare più anime contemporaneamente per generare pacchetti di configurazione personalizzati.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Categories selector
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == "All",
                    onClick = { selectedCategory = "All" },
                    label = { Text("Tutti") }
                )
            }
            items(ProfileCatalog.categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) }
                )
            }
        }

        val filteredTemplates = if (selectedCategory == "All") {
            templates
        } else {
            templates.filter { it.category == selectedCategory }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize().weight(1f)
        ) {
            items(filteredTemplates) { template ->
                val isSelected = selectedKeys.contains(template.key)
                
                // Visual feedback only via selection container background color (No BorderStroke)
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    animationSpec = tween(150), label = ""
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = containerColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleProfileSelected(template.key) }
                        .testTag("catalog_item_${template.key}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = template.emoji,
                                fontSize = 28.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = template.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = template.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleProfileSelected(template.key) }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = template.tagline,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = template.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepCredentials(viewModel: ProfileLabViewModel) {
    val provider by viewModel.provider.collectAsState()
    val customBaseUrl by viewModel.customBaseUrl.collectAsState()
    val model by viewModel.model.collectAsState()
    val maxTokens by viewModel.maxTokens.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()

    var showApiKey by remember { mutableStateOf(false) }

    val providersList = listOf("OpenAI", "Anthropic", "DeepSeek", "Gemini", "Venice/GLM", "Custom")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Configurazione Provider & Modello",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Inserisci i parametri di connessione API. Non memorizziamo le chiavi sui nostri server.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        // Provider select row
        item {
            Text(
                text = "Seleziona Provider",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(providersList) { prov ->
                    val isSelected = provider == prov
                    val chipColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        label = ""
                    )
                    Surface(
                        color = chipColor,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .clickable { viewModel.setProvider(prov) }
                            .padding(4.dp)
                    ) {
                        Text(
                            text = prov,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Custom base URL for Venice/GLM or Custom provider
        if (provider == "Custom" || provider == "Venice/GLM") {
            item {
                OutlinedTextField(
                    value = customBaseUrl,
                    onValueChange = { viewModel.setCustomBaseUrl(it) },
                    label = { Text("Base URL Esplicito (Obbligatorio)") },
                    placeholder = { Text("https://api.your-endpoint.com/v1") },
                    singleLine = true,
                    isError = customBaseUrl.isBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("input_base_url")
                )
                if (customBaseUrl.isBlank()) {
                    Text(
                        text = "Il Base URL è richiesto esplicitamente per i provider personalizzati.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }
        }

        // Model field
        item {
            OutlinedTextField(
                value = model,
                onValueChange = { viewModel.setModel(it) },
                label = { Text("Nome Modello (Nudo)") },
                placeholder = { Text("e.g. gpt-4o-mini") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_model_name")
            )
        }

        // API Key input field
        item {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { viewModel.setApiKey(it) },
                label = { Text("API Key (Opzionale)") },
                placeholder = { Text("Lascia vuoto per usare le variabili d'ambiente sul server") },
                singleLine = true,
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showApiKey) "Nascondi" else "Mostra"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("input_api_key")
            )
        }

        // Max Tokens
        item {
            Text(
                text = "Max Tokens: $maxTokens",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = maxTokens.toFloat(),
                onValueChange = { viewModel.setMaxTokens(it.toInt()) },
                valueRange = 1024f..16384f,
                steps = 15,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun StepDeploy(
    viewModel: ProfileLabViewModel,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    val selectedKeys by viewModel.selectedProfileKeys.collectAsState()
    val activePreviewKey by viewModel.activePreviewKey.collectAsState()
    val deployMode by viewModel.deployMode.collectAsState()
    val telegramToken by viewModel.telegramToken.collectAsState()
    val vpsUser by viewModel.vpsUser.collectAsState()
    val vpsHost by viewModel.vpsHost.collectAsState()
    val vpsPath by viewModel.vpsPath.collectAsState()

    val generatedYaml by viewModel.generatedConfigYaml.collectAsState()
    val generatedScript by viewModel.generatedInstallScript.collectAsState()
    val savedProfiles by viewModel.savedProfiles.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Metodo di Installazione & Deploy",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Scegli come avviare l'agente Hermes sul tuo computer o VPS.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        // Selection of deploy mode
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("cli" to "CLI", "telegram" to "Telegram", "ssh" to "SSH / VPS").forEach { (mode, label) ->
                    val isSelected = deployMode == mode
                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        label = ""
                    )
                    Button(
                        onClick = { viewModel.setDeployMode(mode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = containerColor,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f).testTag("btn_deploy_mode_$mode")
                    ) {
                        Text(label, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Specific fields based on mode
        when (deployMode) {
            "telegram" -> {
                item {
                    OutlinedTextField(
                        value = telegramToken,
                        onValueChange = { viewModel.setTelegramToken(it) },
                        label = { Text("Telegram Bot Token") },
                        placeholder = { Text("Inserisci il token da BotFather") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_tg_token")
                    )
                }
            }
            "ssh" -> {
                item {
                    OutlinedTextField(
                        value = vpsHost,
                        onValueChange = { viewModel.setVpsHost(it) },
                        label = { Text("Host Server (IP o Dominio)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_vps_host")
                    )
                }
                item {
                    OutlinedTextField(
                        value = vpsUser,
                        onValueChange = { viewModel.setVpsUser(it) },
                        label = { Text("Utente SSH") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_vps_user")
                    )
                }
                item {
                    OutlinedTextField(
                        value = vpsPath,
                        onValueChange = { viewModel.setVpsPath(it) },
                        label = { Text("Percorso Remoto di Destinazione") },
                        placeholder = { Text("default: \$HOME/.hermes/profiles/$activePreviewKey") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_vps_path")
                    )
                }
            }
        }

        // Multi-select preview tab selector (only if user selected multiple profiles)
        if (selectedKeys.size > 1) {
            item {
                Text(
                    text = "Seleziona Profilo in Anteprima",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedKeys.toList()) { key ->
                        val isSelected = activePreviewKey == key
                        val containerColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                            label = ""
                        )
                        Surface(
                            color = containerColor,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clickable { viewModel.setActivePreviewKey(key) }
                                .padding(2.dp)
                        ) {
                            Text(
                                text = key.uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // YAML Configuration Display block
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "config.yaml",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Copy button utilizing localClipboardManager stored outside lambda onClick
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(generatedYaml))
                            viewModel.showStatus("Configurazione YAML copiata negli appunti!")
                        },
                        modifier = Modifier.testTag("btn_copy_yaml")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copia YAML")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copia")
                    }
                }
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = generatedYaml,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.Green,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }

        // Install Script Display block
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "script d'installazione (bash)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Copy button utilizing localClipboardManager stored outside lambda onClick
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(generatedScript))
                            viewModel.showStatus("Script di installazione copiata negli appunti!")
                        },
                        modifier = Modifier.testTag("btn_copy_script")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copia Script")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copia")
                    }
                }
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = generatedScript,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.Green,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }

        // Local SQLite History Section
        if (savedProfiles.isNotEmpty()) {
            item {
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = "History & Deployments Locali",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Le tue configurazioni salvate localmente nel database Room.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            items(savedProfiles) { profile ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Model: ${profile.model}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(profile.configYaml))
                                        viewModel.showStatus("YAML copiato dalla history!")
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copia YAML salvato", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.deleteSavedProfile(profile.id)
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
