package com.example.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * HermesBro Profile Lab — wizard a 3 step.
 * Step 0: Catalogo (multi-select)
 * Step 1: Provider + API key per profilo
 * Step 2: Output (CLI / Telegram / SSH) con comandi reali
 * Nessuna metrica simulata, nessun "reboot finto".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileLabScreen(
    viewModel: ProfileLabViewModel,
    modifier: Modifier = Modifier
) {
    val catalog by viewModel.catalog.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val step by viewModel.step.collectAsState()
    val deploy by viewModel.deploy.collectAsState()
    val output by viewModel.generatedOutput.collectAsState()
    val savedConfigs by viewModel.savedConfigs.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CharcoalGray,
                drawerContentColor = PureWhite,
                modifier = Modifier.width(320.dp).fillMaxHeight()
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "⚙️ Configurazioni Salvate",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ElectricCyan,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                Text(
                    text = "Passa rapidamente tra le configurazioni generate in precedenza.",
                    fontSize = 12.sp,
                    color = MutedBlueGray,
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp)
                )
                HorizontalDivider(color = ObsidianBlack.copy(alpha = 0.5f), thickness = 1.dp)
                
                if (savedConfigs.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Nessun salvataggio.\nGenera una configurazione per salvarla qui.",
                            color = MutedBlueGray,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedConfigs) { config ->
                            Card(
                                onClick = {
                                    viewModel.loadSavedConfig(config)
                                    scope.launch { drawerState.close() }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = ObsidianBlack.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = config.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = PureWhite,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        val agentCount = config.selections.size
                                        val modeLabel = config.deploy.mode.uppercase()
                                        Text(
                                            text = "$agentCount agent${if (agentCount == 1) "e" else "i"} · $modeLabel",
                                            fontSize = 11.sp,
                                            color = MutedBlueGray
                                        )
                                        Text(
                                            text = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                                                .format(java.util.Date(config.timestamp)),
                                            fontSize = 10.sp,
                                            color = DarkMetal
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteSavedConfig(config.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Elimina",
                                            tint = CyberPink.copy(alpha = 0.8f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Column(modifier.fillMaxSize().background(ObsidianBlack)) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("HermesBro", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(" · Profile Lab", color = MutedBlueGray, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack)
            )

            // Stepper
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("1 Catalogo", "2 Provider", "3 Deploy").forEachIndexed { i, label ->
                    val active = step == i
                    val clickable = selected.isNotEmpty() || i == 0
                    Surface(
                        modifier = Modifier
                            .clickable(enabled = clickable) { viewModel.setStep(i) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) ElectricCyan.copy(alpha = 0.2f) else CharcoalGray
                    ) {
                        Text(
                            text = label,
                            color = if (active) ElectricCyan else if (clickable) PureWhite else MutedBlueGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            when (step) {
                0 -> CatalogStep(viewModel, catalog, selected)
                1 -> ProviderStep(viewModel, selected)
                2 -> DeployStep(viewModel, deploy, output, selected)
            }
        }
    }
}

@Composable
private fun CatalogStep(
    vm: ProfileLabViewModel,
    catalog: List<ProfileMeta>,
    selected: Map<String, ProfileSelection>
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Catalogo, 1: Crea con AI

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = ObsidianBlack,
            contentColor = ElectricCyan
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("📁 Catalogo", fontWeight = FontWeight.Bold, color = if (activeTab == 0) ElectricCyan else MutedBlueGray) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("✨ Crea con AI", fontWeight = FontWeight.Bold, color = if (activeTab == 1) ElectricCyan else MutedBlueGray) }
            )
        }

        if (activeTab == 0) {
            LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(catalog) { meta ->
                    val isSel = selected.containsKey(meta.id)
                    Card(
                        Modifier.fillMaxWidth().clickable { vm.toggleProfile(meta) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isSel) ElectricCyan.copy(alpha = 0.18f) else CharcoalGray)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProfileAvatar(meta)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(meta.displayName, color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(meta.tagline, color = MutedBlueGray, fontSize = 11.sp)
                                Row(Modifier.padding(top = 4.dp)) {
                                    meta.tags.forEach { t ->
                                        Text("#$t ", color = BrightTeal, fontSize = 10.sp)
                                    }
                                }
                            }
                            Checkbox(checked = isSel, onCheckedChange = { vm.toggleProfile(meta) }, colors = CheckboxDefaults.colors(checkedColor = ElectricCyan))
                        }
                    }
                }
                item {
                    Button(
                        onClick = { if (selected.isNotEmpty()) vm.setStep(1) },
                        enabled = selected.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) { Text("Avanti (${selected.size} selezionati)", fontWeight = FontWeight.Bold) }
                }
            }
        } else {
            CustomSoulForm(vm) {
                activeTab = 0
            }
        }
    }
}

@Composable
private fun CustomSoulForm(vm: ProfileLabViewModel, onGenerated: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🤖") }
    var goal by remember { mutableStateOf("") }
    var traits by remember { mutableStateOf("") }
    var domains by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf("") }
    var signature by remember { mutableStateOf("") }
    var customKey by remember { mutableStateOf("") }

    val isGenerating by vm.isGeneratingCustomSoul.collectAsState()
    val error by vm.customSoulGenerationError.collectAsState()

    val canGenerate = name.isNotBlank() && goal.isNotBlank() && traits.isNotBlank()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Plasma una nuova Anima",
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "Definisci i tratti salienti dell'Agente e lascia che Gemini modelli un SOUL.md completo in italiano secondo i rigorosi standard Hermes.",
                color = MutedBlueGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Agente *", color = MutedBlueGray) },
                    placeholder = { Text("es. Jarvis", color = DarkMetal) },
                    singleLine = true,
                    colors = fieldColors(),
                    modifier = Modifier.weight(3f)
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = { Text("Emoji", color = MutedBlueGray) },
                    placeholder = { Text("🤖", color = DarkMetal) },
                    singleLine = true,
                    colors = fieldColors(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                label = { Text("Obiettivo / Missione Principale *", color = MutedBlueGray) },
                placeholder = { Text("Cosa deve fare? es. Gestire la mia VPS, anticipare incidenti...", color = DarkMetal) },
                minLines = 2,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = traits,
                onValueChange = { traits = it },
                label = { Text("Tratti della Personalità *", color = MutedBlueGray) },
                placeholder = { Text("es. Sarcasmo alla Groucho Marx, logica fredda Mickey Malka...", color = DarkMetal) },
                minLines = 2,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = domains,
                onValueChange = { domains = it },
                label = { Text("Domini Operativi (Expertise)", color = MutedBlueGray) },
                placeholder = { Text("es. Crypto, Server monitoring, Copywriting (opzionale)", color = DarkMetal) },
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = tone,
                onValueChange = { tone = it },
                label = { Text("Registro / Stile di Risposta", color = MutedBlueGray) },
                placeholder = { Text("es. Veloce, diretto, risposte brevi per default (opzionale)", color = DarkMetal) },
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = signature,
                onValueChange = { signature = it },
                label = { Text("Firma / Motto", color = MutedBlueGray) },
                placeholder = { Text("es. Suit up. (opzionale)", color = DarkMetal) },
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = customKey,
                onValueChange = { customKey = it },
                label = { Text("Gemini API Key (Opzionale)", color = MutedBlueGray) },
                placeholder = { Text("Usa Secrets se vuoto", color = DarkMetal) },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Verrà usata la API Key iniettata da AI Studio se lasciata vuota.",
                color = MutedBlueGray,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }

        if (error != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberPink.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Errore nella generazione", color = CyberPink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(error ?: "", color = PureWhite, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }

        item {
            if (isGenerating) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = ElectricCyan)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Gemini sta forgiando il SOUL del tuo Agente...",
                        color = ElectricCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Button(
                    onClick = {
                        vm.generateCustomSoulProfile(
                            name = name,
                            goal = goal,
                            traits = traits,
                            domain = domains,
                            tone = tone,
                            signature = signature,
                            emoji = emoji,
                            customKey = customKey.ifBlank { null }
                        ) {
                            onGenerated()
                        }
                    },
                    enabled = canGenerate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = ObsidianBlack,
                        disabledContainerColor = SlateSteel,
                        disabledContentColor = MutedBlueGray
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Genera Profilo con AI ✨", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProviderStep(vm: ProfileLabViewModel, selected: Map<String, ProfileSelection>) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(selected.values.toList()) { sel ->
            var prov by remember { mutableStateOf(sel.provider) }
            var model by remember { mutableStateOf(sel.model) }
            var key by remember { mutableStateOf(sel.apiKey) }
            var baseUrl by remember { mutableStateOf(sel.baseUrl) }
            val catalogList by vm.catalog.collectAsState()
            val meta = catalogList.firstOrNull { it.id == sel.id }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CharcoalGray)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (meta != null) {
                            ProfileAvatar(meta, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(sel.displayName, color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prov, onValueChange = { prov = it; vm.updateSelection(sel.id, prov, model, key, baseUrl) },
                        label = { Text("Provider", color = MutedBlueGray) }, singleLine = true,
                        colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = model, onValueChange = { model = it; vm.updateSelection(sel.id, prov, model, key, baseUrl) },
                        label = { Text("Model (es. gemini-2.5-flash)", color = MutedBlueGray) }, singleLine = true,
                        colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = key, onValueChange = { key = it; vm.updateSelection(sel.id, prov, model, key, baseUrl) },
                        label = { Text("API Key (resta locale)", color = MutedBlueGray) }, singleLine = true,
                        colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = baseUrl, onValueChange = { baseUrl = it; vm.updateSelection(sel.id, prov, model, key, baseUrl) },
                        label = { Text("Base URL (solo custom)", color = MutedBlueGray) }, singleLine = true,
                        colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { vm.setStep(0) },
                    border = BorderStroke(1.dp, SlateSteel),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Indietro")
                }
                Button(
                    onClick = { vm.setStep(2) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Avanti", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DeployStep(
    vm: ProfileLabViewModel,
    deploy: DeployConfig,
    output: String,
    selected: Map<String, ProfileSelection>
) {
    var mode by remember { mutableStateOf(deploy.mode) }
    var botToken by remember { mutableStateOf(deploy.botToken) }
    var sshHost by remember { mutableStateOf(deploy.sshHost) }
    var sshUser by remember { mutableStateOf(deploy.sshUser) }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current

    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Metodo di installazione", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("cli" to "CLI locale", "telegram" to "Telegram", "ssh" to "SSH server").forEach { (m, label) ->
                    val active = mode == m
                    Surface(
                        onClick = { mode = m; vm.setDeploy(DeployConfig(mode, botToken, sshHost, sshUser)) },
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) ElectricCyan.copy(alpha = 0.2f) else CharcoalGray
                    ) {
                        Text(label, color = if (active) ElectricCyan else MutedBlueGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (mode == "telegram") {
            item {
                OutlinedTextField(value = botToken, onValueChange = { botToken = it; vm.setDeploy(DeployConfig(mode, botToken, sshHost, sshUser)) },
                    label = { Text("Bot Token", color = MutedBlueGray) }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
            }
        }
        if (mode == "ssh") {
            item {
                OutlinedTextField(value = sshHost, onValueChange = { sshHost = it; vm.setDeploy(DeployConfig(mode, botToken, sshHost, sshUser)) },
                    label = { Text("Host (es. 1.2.3.4)", color = MutedBlueGray) }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = sshUser, onValueChange = { sshUser = it; vm.setDeploy(DeployConfig(mode, botToken, sshHost, sshUser)) },
                    label = { Text("User (es. hermes-pc)", color = MutedBlueGray) }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { vm.setStep(1) },
                    border = BorderStroke(1.dp, SlateSteel),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text("Indietro")
                }
                Button(
                    onClick = { vm.generate() },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack),
                    modifier = Modifier.weight(2f)
                ) {
                    Text("Genera file", fontWeight = FontWeight.Bold)
                }
            }
        }
        if (output.isNotBlank()) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Output generato", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Button(onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(output))
                        vm.markCopied()
                    }, colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack)) {
                        Text(if (vm.copied.value) "Copiato ✓" else "Copia")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ObsidianBlack)) {
                    Text(output, color = NeonGreen, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ElectricCyan, unfocusedBorderColor = SlateSteel,
    focusedContainerColor = CharcoalGray, unfocusedContainerColor = CharcoalGray,
    focusedTextColor = PureWhite, unfocusedTextColor = PureWhite
)

@Composable
fun ProfileAvatar(meta: ProfileMeta, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    // 1. Check if it's a custom profile with a generated drawable avatar
    if (meta.customAvatar != null) {
        val drawableId = remember(meta.customAvatar) {
            context.resources.getIdentifier(meta.customAvatar, "drawable", context.packageName)
        }
        if (drawableId != 0) {
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = meta.displayName,
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, ElectricCyan, CircleShape)
            )
            return
        }
    }

    // 2. Check if it's a default profile with an asset image PFP
    val assetBitmap = remember(meta.id) {
        val extensions = listOf("png", "jpg", "jpeg")
        var bitmap: android.graphics.Bitmap? = null
        for (ext in extensions) {
            try {
                context.assets.open("pfp/${meta.id}.$ext").use { input ->
                    bitmap = BitmapFactory.decodeStream(input)
                }
                if (bitmap != null) break
            } catch (e: Exception) {
                // Try next extension
            }
        }
        // Special mapping for h2bb / study-glm
        if (bitmap == null) {
            val specialNames = mapOf(
                "study_glm" to "study-glm.png",
                "h2bb" to "h2bb.jpg"
            )
            specialNames[meta.id]?.let { filename ->
                try {
                    context.assets.open("pfp/$filename").use { input ->
                        bitmap = BitmapFactory.decodeStream(input)
                    }
                } catch (e: Exception) {}
            }
        }
        bitmap?.asImageBitmap()
    }

    if (assetBitmap != null) {
        Image(
            bitmap = assetBitmap,
            contentDescription = meta.displayName,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(1.5.dp, MutedBlueGray, CircleShape)
        )
    } else {
        // 3. Fallback to emoji with a glowing background circle
        Box(
            modifier = modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CharcoalGray)
                .border(1.dp, MutedBlueGray.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(meta.emoji, fontSize = 24.sp)
        }
    }
}
