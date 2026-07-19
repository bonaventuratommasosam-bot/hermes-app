package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ProfileTemplate
import com.example.data.ProfileTemplates
import com.example.data.SavedProfile
import com.example.viewmodel.ProfileLabViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: ProfileLabViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val savedProfiles by viewModel.savedProfiles.collectAsState()
    val selectedTemplate by viewModel.selectedTemplate.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // Handle toast messages on status change
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == "templates",
                    onClick = { viewModel.selectTab("templates") },
                    icon = { Icon(Icons.Default.List, contentDescription = "Templates") },
                    label = { Text("Profili") },
                    modifier = Modifier.testTag("tab_templates")
                )
                NavigationBarItem(
                    selected = currentTab == "workspace",
                    onClick = { viewModel.selectTab("workspace") },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Laboratorio") },
                    label = { Text("Laboratorio") },
                    modifier = Modifier.testTag("tab_workspace")
                )
                NavigationBarItem(
                    selected = currentTab == "history",
                    onClick = { viewModel.selectTab("history") },
                    icon = { Icon(Icons.Default.History, contentDescription = "Deployments") },
                    label = { Text("Archivio") },
                    modifier = Modifier.testTag("tab_history")
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "templates" -> BrowseTemplatesTab(
                    viewModel = viewModel,
                    onTemplateSelected = {
                        viewModel.selectTemplate(it)
                        viewModel.selectTab("workspace")
                    }
                )
                "workspace" -> WorkspaceTab(
                    viewModel = viewModel
                )
                "history" -> SavedDeploymentsTab(
                    viewModel = viewModel,
                    savedProfiles = savedProfiles
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseTemplatesTab(
    viewModel: ProfileLabViewModel,
    onTemplateSelected: (ProfileTemplate) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val filteredTemplates = remember(selectedCategory) {
        if (selectedCategory == "All") {
            ProfileTemplates.templates
        } else {
            ProfileTemplates.templates.filter { it.category == selectedCategory }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // App title with tech badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Text(
                text = "HERMESBRO",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "PROFILE LAB",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Text(
            text = "Seleziona un'anima per il tuo agente Hermes. Configura il provider e deploya il file YAML ed installatori Bash reali.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Horizontal scrolling category chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == "All",
                    onClick = { selectedCategory = "All" },
                    label = { Text("Tutti") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            items(ProfileTemplates.categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Templates List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredTemplates) { template ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTemplateSelected(template) }
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            CardDefaults.shape
                        )
                        .testTag("template_card_${template.key}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = template.emoji,
                                fontSize = 32.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = template.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = template.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Customize",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = template.tagline,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = template.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceTab(
    viewModel: ProfileLabViewModel
) {
    val template by viewModel.selectedTemplate.collectAsState()
    val provider by viewModel.provider.collectAsState()
    val model by viewModel.model.collectAsState()
    val maxTokens by viewModel.maxTokens.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val deployMode by viewModel.deployMode.collectAsState()
    val telegramToken by viewModel.telegramToken.collectAsState()
    val vpsUser by viewModel.vpsUser.collectAsState()
    val vpsHost by viewModel.vpsHost.collectAsState()
    val vpsPath by viewModel.vpsPath.collectAsState()
    val customSoulPrompt by viewModel.customSoulPrompt.collectAsState()
    val generatedConfigYaml by viewModel.generatedConfigYaml.collectAsState()
    val generatedInstallScript by viewModel.generatedInstallScript.collectAsState()
    val refinementRequest by viewModel.refinementRequest.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showApiKey by remember { mutableStateOf(false) }
    var activeOutputTab by remember { mutableStateOf("yaml") } // "yaml" or "script"
    var showManualPromptEditor by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (template == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Seleziona prima un profilo nel catalogo!")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Visual Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(CardDefaults.shape)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        CardDefaults.shape
                    )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hermes_banner),
                        contentDescription = "Hermes Laboratory Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Cyberpunk dark transparent overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    // Profile context labels
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = template!!.emoji,
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = template!!.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = template!!.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Section: Provider & AI settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Configurazione AI Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Provider Choices
                    Text(
                        text = "AI PROVIDER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val providers = listOf("OpenAI", "Anthropic", "DeepSeek", "Gemini", "Venice/GLM", "Custom")
                        items(providers) { p ->
                            FilterChip(
                                selected = provider == p,
                                onClick = { viewModel.setProvider(p) },
                                label = { Text(p) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Model Name Input
                    OutlinedTextField(
                        value = model,
                        onValueChange = { viewModel.setModel(it) },
                        label = { Text("Model Name") },
                        modifier = Modifier.fillMaxWidth().testTag("model_input"),
                        leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Max Tokens Slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MAX TOKENS (MODEL & PROVIDER)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$maxTokens tokens",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = maxTokens.toFloat(),
                            onValueChange = { viewModel.setMaxTokens(it.toInt()) },
                            valueRange = 1000f..32000f,
                            steps = 31,
                            modifier = Modifier.weight(1.5f).testTag("token_slider")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // API Key Input
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { viewModel.setApiKey(it) },
                        label = { Text("API Key (Opzionale)") },
                        placeholder = { Text("Usa BuildConfig se vuoto") },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Key"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("api_key_input")
                    )
                    Text(
                        text = "Verrà memorizzata in locale o esportata nel file config.yaml.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Section: Deployment Target Setup
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. Target di Deployment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Deploy Mode Selector (CLI, Telegram, SSH)
                    Text(
                        text = "METODO DI DEPLOY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val modes = listOf("cli", "telegram", "ssh")
                        modes.forEach { m ->
                            val isSelected = deployMode == m
                            Button(
                                onClick = { viewModel.setDeployMode(m) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("deploy_mode_$m")
                            ) {
                                val icon = when (m) {
                                    "cli" -> Icons.Default.Terminal
                                    "telegram" -> Icons.Default.Send
                                    else -> Icons.Default.Dns
                                }
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(m.uppercase(), fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Conditional inputs depending on deployMode
                    when (deployMode) {
                        "telegram" -> {
                            OutlinedTextField(
                                value = telegramToken,
                                onValueChange = { viewModel.setTelegramToken(it) },
                                label = { Text("Telegram Bot Token") },
                                placeholder = { Text("1234567890:ABC-DEF1234...") },
                                modifier = Modifier.fillMaxWidth().testTag("tg_token_input"),
                                leadingIcon = { Icon(Icons.Default.Send, contentDescription = null) }
                            )
                        }
                        "ssh" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = vpsUser,
                                    onValueChange = { viewModel.setVpsUser(it) },
                                    label = { Text("VPS SSH User") },
                                    modifier = Modifier.fillMaxWidth().testTag("vps_user_input")
                                )
                                OutlinedTextField(
                                    value = vpsHost,
                                    onValueChange = { viewModel.setVpsHost(it) },
                                    label = { Text("VPS Host / IP Address") },
                                    modifier = Modifier.fillMaxWidth().testTag("vps_host_input")
                                )
                                OutlinedTextField(
                                    value = vpsPath,
                                    onValueChange = { viewModel.setVpsPath(it) },
                                    label = { Text("Profile Target Folder") },
                                    modifier = Modifier.fillMaxWidth().testTag("vps_path_input")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Custom Soul Prompts & Gemini AI playground
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "3. Personalizzazione Anima (IA)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Raffina la personalità, aggiungi skill o cambia il comportamento scrivendo un comando naturale. Gemini adatterà il file SOUL.md mantenendo le regole.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // AI Refinement Request Input
                    OutlinedTextField(
                        value = refinementRequest,
                        onValueChange = { viewModel.setRefinementRequest(it) },
                        placeholder = { Text("Es: Rendi il tono più sarcastico e aggiungi nozioni su Kubernetes") },
                        modifier = Modifier.fillMaxWidth().testTag("refinement_input"),
                        minLines = 2,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Gemini Trigger Button
                    Button(
                        onClick = { viewModel.refineSoulWithGemini() },
                        enabled = !isLoading && refinementRequest.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("refine_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PERSONALIZZA CON GEMINI IA 🤖")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Expansion control for manual prompt preview
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showManualPromptEditor = !showManualPromptEditor }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = if (showManualPromptEditor) "Nascondi Editor Anima (SOUL.md)" else "Mostra Editor Anima (SOUL.md)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (showManualPromptEditor) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(
                        visible = showManualPromptEditor,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customSoulPrompt,
                                onValueChange = { viewModel.updateSoulPromptManually(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .testTag("manual_prompt_editor"),
                                label = { Text("SOUL.md prompt") },
                                textStyle = LocalTextStyle.current.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section: Generated file viewers (config.yaml and install.sh)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "4. File Generati Real-Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab selector between config.yaml and install.sh
                    TabRow(
                        selectedTabIndex = if (activeOutputTab == "yaml") 0 else 1,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = activeOutputTab == "yaml",
                            onClick = { activeOutputTab = "yaml" },
                            text = { Text("config.yaml", style = MaterialTheme.typography.labelMedium) }
                        )
                        Tab(
                            selected = activeOutputTab == "script",
                            onClick = { activeOutputTab = "script" },
                            text = { Text("install.sh", style = MaterialTheme.typography.labelMedium) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Code Viewer Box
                    val fileContent = if (activeOutputTab == "yaml") generatedConfigYaml else generatedInstallScript
                    val labelText = if (activeOutputTab == "yaml") "config.yaml" else "install.sh"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(10.dp)
                    ) {
                        val scrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                text = fileContent,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Copy to Clipboard & Local Room Save Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText(labelText, fileContent)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "$labelText copiato negli appunti!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("copy_code_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copia Codice")
                        }

                        Button(
                            onClick = { viewModel.saveDeploymentToHistory() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("save_deployment_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salva Deploy")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedDeploymentsTab(
    viewModel: ProfileLabViewModel,
    savedProfiles: List<SavedProfile>
) {
    val context = LocalContext.current
    var viewingProfileDialog by remember { mutableStateOf<SavedProfile?>(null) }
    var dialogTab by remember { mutableStateOf("yaml") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text(
                text = "ARCHIVIO CONFIGS",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }

        Text(
            text = "Trova qui tutte le configurazioni e gli installatori salvati nel database Room del dispositivo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (savedProfiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "Nessun deployment salvato",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Configura un profilo nel Laboratorio e clicca 'Salva Deploy' per archiviare i file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Button(
                        onClick = { viewModel.selectTab("templates") }
                    ) {
                        Text("Sfoglia Profili")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(savedProfiles) { profile ->
                    val dateFormatted = remember(profile.timestamp) {
                        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        sdf.format(Date(profile.timestamp))
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                CardDefaults.shape
                            )
                            .testTag("saved_profile_card_${profile.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = getProfileEmoji(profile.profileKey),
                                    fontSize = 28.sp,
                                    modifier = Modifier.padding(end = 10.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = dateFormatted,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSavedProfile(profile.id) },
                                    modifier = Modifier.testTag("delete_profile_${profile.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Rimuovi",
                                        tint = Color(0xFFE57373)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Details block
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Provider: ${profile.provider} • Modello: ${profile.model}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Deploy: ${profile.deployMode.uppercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewingProfileDialog = profile
                                        dialogTab = "yaml"
                                    },
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Visualizza", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("install.sh", profile.installScript)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Script install.sh copiato!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1.8f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copia Installatore", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue for viewing the files fully
    viewingProfileDialog?.let { profile ->
        AlertDialog(
            onDismissRequest = { viewingProfileDialog = null },
            title = {
                Text(
                    text = "${profile.name} Files",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TabRow(
                        selectedTabIndex = if (dialogTab == "yaml") 0 else if (dialogTab == "soul") 1 else 2,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                    ) {
                        Tab(
                            selected = dialogTab == "yaml",
                            onClick = { dialogTab = "yaml" },
                            text = { Text("config.yaml", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = dialogTab == "soul",
                            onClick = { dialogTab = "soul" },
                            text = { Text("SOUL.md", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = dialogTab == "script",
                            onClick = { dialogTab = "script" },
                            text = { Text("install.sh", fontSize = 11.sp) }
                        )
                    }

                    val codeToShow = when (dialogTab) {
                        "yaml" -> profile.configYaml
                        "soul" -> profile.soulPrompt
                        else -> profile.installScript
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        val scrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                text = codeToShow,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val codeToCopy = when (dialogTab) {
                                "yaml" -> profile.configYaml
                                "soul" -> profile.soulPrompt
                                else -> profile.installScript
                            }
                            val clipName = when (dialogTab) {
                                "yaml" -> "config.yaml"
                                "soul" -> "SOUL.md"
                                else -> "install.sh"
                            }
                            val clip = ClipData.newPlainText(clipName, codeToCopy)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "$clipName copiato!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Copia Attivo")
                    }
                    Button(
                        onClick = { viewingProfileDialog = null }
                    ) {
                        Text("Chiudi")
                    }
                }
            }
        )
    }
}

// Utility to fetch emoji from key
private fun getProfileEmoji(key: String): String {
    return ProfileTemplates.templates.find { it.key == key }?.emoji ?: "🤖"
}

// Scroll states handled directly in layout blocks
