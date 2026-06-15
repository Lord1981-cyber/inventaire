package com.example.ui.screens

import android.Manifest
import android.util.Log
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.content.Context
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import com.example.data.ControleDraft
import coil.compose.rememberAsyncImagePainter
import com.example.data.models.*
import com.example.ui.viewmodels.AppScreen
import com.example.ui.viewmodels.InventoryViewModel
import com.example.ui.viewmodels.CriticalAlert
import com.example.ui.theme.*
import java.util.UUID
import android.speech.RecognizerIntent
import android.graphics.Paint as AndroidPaint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.FileProvider
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.zIndex

// Harmonized M3 colors
val DarkBlueGrad = Color(0xFF6750A4)
val DarkMedGrad = Color(0xFFEADDFF)
val DarkCyanGrad = Color(0xFF21005D)
val TechCyan = Color(0xFF6750A4)
val TechGreen = Color(0xFF4F6354)
val TechRed = Color(0xFFB3261E)
val TechYellow = Color(0xFFE0A800)


@Composable
fun AppNavigator(viewModel: InventoryViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearSyncMessage()
        }
    }

    val criticalAlert by viewModel.activeCriticalAlert.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val screen = currentScreen) {
            is AppScreen.Login -> LoginScreen(viewModel)
            is AppScreen.Dashboard -> DashboardScreen(viewModel)
            is AppScreen.Scan -> ScanScreen(viewModel)
            is AppScreen.Detail -> DetailScreen(viewModel, screen.equipmentId)
            is AppScreen.Controle -> ControleScreen(viewModel, screen.equipmentId)
            is AppScreen.Anomalie -> AnomalieScreen(viewModel, screen.equipmentId)
            is AppScreen.Historique -> HistoriqueScreen(viewModel)
        }

        AnimatedVisibility(
            visible = criticalAlert != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).zIndex(99f)
        ) {
            criticalAlert?.let { alert ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .clickable {
                            viewModel.navigateTo(AppScreen.Detail(alert.equipmentId))
                            viewModel.clearCriticalAlert()
                        },
                    colors = CardDefaults.cardColors(containerColor = SleekErrorBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, SleekErrorText),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SleekErrorBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alert",
                                tint = SleekErrorText,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ALERTE ANOMALIE CRITIQUE !",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = SleekErrorText,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Équipement ${alert.equipmentCode} : ${alert.description}",
                                fontSize = 13.sp,
                                color = SleekTextDark,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearCriticalAlert() }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = SleekTextMuted)
                        }
                    }
                }
            }
        }

        // Real-time connection status indicator (on all screens/activities)
        val isOnline by viewModel.isOnline.collectAsState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 12.dp, start = 12.dp)
                .zIndex(100f),
            contentAlignment = Alignment.BottomStart
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isOnline) Color(0xFFC8E6C9) else Color(0xFFFFCDD2)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.testTag("connection_status_indicator")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = if (isOnline) "En ligne" else "Hors-ligne",
                        tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (isOnline) "En ligne" else "Hors-ligne",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
        }

        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = TechCyan)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Traitement en cours...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: InventoryViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val apiBaseUrl by viewModel.apiBaseUrl.collectAsState()
    val loginError by viewModel.loginError.collectAsState()
    var editApiUrl by remember { mutableStateOf(apiBaseUrl) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(SleekPurpleBg, SleekPurpleSurface)
                    )
                )
            }
            .systemBarsPadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Logo Setup (Matching Sleek HTML specifications)
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(SleekPurpleLight, CircleShape)
                    .border(1.dp, SleekBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory,
                    contentDescription = "Logo",
                    tint = SleekPurpleDark,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Inventaire Institut",
                fontSize = 26.sp,
                color = SleekTextDark,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "Technique & Centre de Calcul",
                fontSize = 14.sp,
                color = SleekTextMuted,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Login Container Card (Sleek light design)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CONNEXION",
                        fontSize = 15.sp,
                        color = SleekPurple,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input"),
                        label = { Text("Adresse Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "EmailIcon", tint = SleekPurple) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SleekPurple,
                            unfocusedBorderColor = SleekBorder,
                            focusedLabelColor = SleekPurple,
                            unfocusedLabelColor = SleekTextMuted,
                            focusedTextColor = SleekTextDark,
                            unfocusedTextColor = SleekTextDark,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        label = { Text("Mot de passe") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "LockIcon", tint = SleekPurple) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "TogglePassword",
                                    tint = SleekTextMuted
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SleekPurple,
                            unfocusedBorderColor = SleekBorder,
                            focusedLabelColor = SleekPurple,
                            unfocusedLabelColor = SleekTextMuted,
                            focusedTextColor = SleekTextDark,
                            unfocusedTextColor = SleekTextDark,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    loginError?.let { err ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = err,
                            color = SleekErrorText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Main Action Button (In sleek violet)
                    Button(
                        onClick = { viewModel.performLogin(email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("login_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPurple, contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "SE CONNECTER",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Settings button to config PHP Endpoint IP
            TextButton(
                onClick = { showSettings = !showSettings },
                colors = ButtonDefaults.textButtonColors(contentColor = SleekPurple)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Configuration Serveur PHP")
            }

            if (showSettings) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekPurpleSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Adresse de l'API (Back-End PHP)",
                            color = SleekTextDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editApiUrl,
                            onValueChange = { editApiUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SleekTextDark,
                                unfocusedTextColor = SleekTextDark,
                                focusedBorderColor = SleekPurple,
                                unfocusedBorderColor = SleekBorder,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            placeholder = { Text("http://192.168.1.50/inventaire/", color = SleekTextMuted) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    editApiUrl = apiBaseUrl
                                    showSettings = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray, contentColor = Color.White)
                            ) {
                                Text("Annuler")
                            }
                            Button(
                                onClick = {
                                    viewModel.setApiUrl(editApiUrl)
                                    showSettings = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPurple, contentColor = Color.White)
                            ) {
                                Text("Enregistrer")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConformityPieChart(conformeCount: Int, nonConformeCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("conformity_pie_chart"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Draw Pie Chart in Canvas
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val total = (conformeCount + nonConformeCount).toFloat()
                    if (total == 0f) {
                        drawCircle(color = SleekBorder)
                    } else {
                        val conformeAngle = (conformeCount / total) * 360f
                        val nonConformeAngle = (nonConformeCount / total) * 360f
                        
                        drawArc(
                            color = SleekSuccessText,
                            startAngle = -90f,
                            sweepAngle = conformeAngle,
                            useCenter = true
                        )
                        drawArc(
                            color = SleekErrorText,
                            startAngle = -90f + conformeAngle,
                            sweepAngle = nonConformeAngle,
                            useCenter = true
                        )
                    }
                }
                
                // Donut inner hole
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color.White, CircleShape)
                )
            }
            
            // Legend Info description
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Ratio de Conformité",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextDark
                )
                Text(
                    text = "État sanitaire général des équipements",
                    fontSize = 11.sp,
                    color = SleekTextMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 1.dp)
                ) {
                    Box(modifier = Modifier.size(10.dp).background(SleekSuccessText, RoundedCornerShape(3.dp)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Conformes: $conformeCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SleekSuccessText
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 1.dp)
                ) {
                    Box(modifier = Modifier.size(10.dp).background(SleekErrorText, RoundedCornerShape(3.dp)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Non Conformes: $nonConformeCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SleekErrorText
                    )
                }
                
                if (conformeCount + nonConformeCount > 0) {
                    val percentage = (conformeCount.toFloat() / (conformeCount + nonConformeCount) * 100).toInt()
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Taux de conformité: $percentage%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekPurple
                    )
                }
            }
        }
    }
}

@Composable
fun AnomaliesByCategoryBarChart(equipements: List<Equipement>, anomalies: List<Anomalie>) {
    val eqMap = remember(equipements) { equipements.associateBy { it.id } }
    val data = remember(anomalies, eqMap) {
        anomalies
            .filter { it.statut != "résolue" }
            .groupBy { anom -> eqMap[anom.equipement_id]?.type ?: "Autre" }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("anomalies_by_category_chart"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Anomalies par Catégorie d'Équipement",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SleekTextDark
            )
            Text(
                text = "Dispersion des points critiques",
                fontSize = 11.sp,
                color = SleekTextMuted,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune anomalie active répertoriée 🎉",
                        fontSize = 12.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                val maxCount = data.maxOfOrNull { it.second } ?: 1
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.take(5).forEach { (category, count) ->
                        val barHeightFactor = count.toFloat() / maxCount.toFloat()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = count.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekPurple
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight(0.7f * barHeightFactor)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                SleekPurple,
                                                SleekPurple.copy(alpha = 0.4f)
                                            )
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (category.length > 8) category.take(7) + ".." else category,
                                fontSize = 10.sp,
                                color = SleekTextDark,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DashboardScreen(viewModel: InventoryViewModel) {
    val user by viewModel.currentUser.collectAsState()
    val salles by viewModel.salles.collectAsState()
    val equipements by viewModel.equipements.collectAsState()
    val pendingControles by viewModel.controles.collectAsState()
    val pendingAnoms by viewModel.anomalies.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSalleId by remember { mutableStateOf<Int?>(null) }

    // Counts info
    val totalEquips = equipements.size
    val operationalCount = equipements.count { it.statut == "opérationnel" }
    val brokenCount = equipements.count { it.statut == "en panne" }
    val maintenanceCount = equipements.count { it.statut == "en maintenance" }
    val toCheckCount = equipements.count { it.statut == "à contrôler" }

    // Offline elements count
    val unsyncedConts = pendingControles.count { !it.is_synced }
    val unsyncedAnom = pendingAnoms.count { !it.is_synced }
    val unsyncedCount = unsyncedConts + unsyncedAnom

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekPurpleBg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SleekPurpleLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = "Inventory Logo",
                                tint = SleekPurpleDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Inventaire",
                                fontSize = 16.sp,
                                color = SleekTextDark,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Institut Technique",
                                fontSize = 11.sp,
                                color = SleekTextMuted,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.navigateTo(AppScreen.Historique) }) {
                            Icon(Icons.Default.History, contentDescription = "History", tint = SleekTextDark)
                        }
                        // Beautiful Avatar border frame / logout wrapper
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SleekPurpleLight)
                                .border(1.dp, SleekBorder, CircleShape)
                                .clickable { viewModel.performLogout() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Logout",
                                tint = SleekErrorText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SleekPurpleBg)
                .padding(top = innerPadding.calculateTopPadding(), bottom = 20.dp)
        ) {
            // Unsynced banner header with alert container styling
            if (unsyncedCount > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SleekPurpleLight),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Cloud", tint = SleekPurpleDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$unsyncedCount rapports hors-ligne en cours",
                                fontSize = 13.sp,
                                color = SleekPurpleDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = { viewModel.performSync() },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPurple, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Synchroniser", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Scrollable view containers
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Status Greeting section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = androidx.compose.ui.text.buildAnnotatedString {
                            append("Bonjour, ")
                            pushStyle(androidx.compose.ui.text.SpanStyle(color = SleekPurple, fontWeight = FontWeight.SemiBold))
                            append(user?.prenom ?: "Inspecteur")
                            pop()
                            append(" 👋")
                        },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        color = SleekTextDark
                    )
                    Text(
                        text = "Votre session expire dans 4h 20m",
                        fontSize = 13.sp,
                        color = SleekTextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Quick Stats Bento Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bento 1: Equipments Total
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekPurpleLight),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.ViewCozy, contentDescription = null, tint = SleekPurpleDark, modifier = Modifier.size(24.dp))
                            Column {
                                Text(
                                    text = totalEquips.toString(),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekPurpleDark
                                )
                                Text(
                                    text = "Équipements",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SleekPurpleDark.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Bento 2: Anomalies Actives / En Panne
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekPurpleSurface),
                        border = BorderStroke(1.dp, SleekBorder),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = SleekErrorText, modifier = Modifier.size(24.dp))
                            Column {
                                Text(
                                    text = (brokenCount + toCheckCount).toString().padStart(2, '0'),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekErrorText
                                )
                                Text(
                                    text = "Anomalies actives",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SleekTextMuted
                                )
                            }
                        }
                    }
                }

                // Custom Visual Donut Pie Chart for Equipment Conformity Status
                ConformityPieChart(
                    conformeCount = operationalCount,
                    nonConformeCount = brokenCount + toCheckCount + maintenanceCount
                )

                // Custom Bar Chart displaying anomalies by category
                AnomaliesByCategoryBarChart(
                    equipements = equipements,
                    anomalies = pendingAnoms
                )

                // Primary Scan Action Card (linked to camera scanner layout)
                Card(
                    onClick = { viewModel.navigateTo(AppScreen.Scan) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekPurple),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Scanner QR Code",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                "Vérifier un équipement ou une salle",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = "Scan icon",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Recent inspections / stats label row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Derniers contrôles",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextMuted
                    )
                    TextButton(onClick = { viewModel.navigateTo(AppScreen.Historique) }) {
                        Text("Voir tout", fontSize = 11.sp, color = SleekPurple, fontWeight = FontWeight.Bold)
                    }
                }

                // Search Bar & Filter options input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher par nom, modèle ou n° de série...", fontSize = 13.sp, color = SleekTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("search_equipment_input"),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "SearchIcon", tint = SleekTextMuted) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = SleekPurple,
                        unfocusedBorderColor = SleekBorder,
                        focusedTextColor = SleekTextDark,
                        unfocusedTextColor = SleekTextDark
                    ),
                    singleLine = true
                )

                if (searchQuery.isNotEmpty()) {
                    val matchingSuggestions = equipements.filter {
                        it.code_inventaire.contains(searchQuery, ignoreCase = true) ||
                        it.type.contains(searchQuery, ignoreCase = true) ||
                        it.const_modele.contains(searchQuery, ignoreCase = true) ||
                        it.const_fabricant.contains(searchQuery, ignoreCase = true)
                    }.take(4)

                    if (matchingSuggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("search_autocomplete_card"),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, SleekPurple.copy(alpha = 0.4f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, top = 10.dp, end = 14.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AUTO-COMPLÉTION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekPurple,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Appuyez pour ouvrir",
                                        fontSize = 9.sp,
                                        color = SleekTextMuted
                                    )
                                }
                                matchingSuggestions.forEach { eq ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                searchQuery = "" // clear search on select
                                                viewModel.navigateTo(AppScreen.Detail(eq.id))
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory,
                                            contentDescription = "equip",
                                            tint = SleekPurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = eq.code_inventaire,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SleekTextDark
                                            )
                                            Text(
                                                text = "${eq.type} • ${eq.nom_salle}",
                                                fontSize = 11.sp,
                                                color = SleekTextMuted
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = SleekBorder, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }

                // Salles (Rooms) list chips horizontal title
                Text(
                    text = "Filtrer par Salles de machines",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextMuted,
                    modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSalleId == null,
                        onClick = { selectedSalleId = null },
                        label = { Text("TOUT") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SleekPurpleLight,
                            selectedLabelColor = SleekPurpleDark
                        )
                    )
                    for (salle in salles) {
                        FilterChip(
                            selected = selectedSalleId == salle.id,
                            onClick = { selectedSalleId = salle.id },
                            label = { Text(salle.nom_salle) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SleekPurpleLight,
                                selectedLabelColor = SleekPurpleDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Inventory List of Equipment cards
                val filteredEquipments = equipements.filter {
                    (selectedSalleId == null || it.salle_id == selectedSalleId) &&
                            (searchQuery.isEmpty() ||
                                    it.code_inventaire.contains(searchQuery, ignoreCase = true) ||
                                    it.type.contains(searchQuery, ignoreCase = true) ||
                                    it.nom_salle.contains(searchQuery, ignoreCase = true) ||
                                    it.const_modele.contains(searchQuery, ignoreCase = true) ||
                                    it.const_fabricant.contains(searchQuery, ignoreCase = true))
                }

                // Row for Title and CSV Export option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ÉQUIPEMENTS (${filteredEquipments.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextMuted,
                        letterSpacing = 1.sp
                    )
                    
                    val context = LocalContext.current
                    TextButton(
                        onClick = {
                            exportEquipmentListCsv(context, filteredEquipments)
                        },
                        modifier = Modifier.testTag("export_csv_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "csv",
                            tint = SleekPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "EXPORTER CSV",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekPurple
                        )
                    }
                }

                if (filteredEquipments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = "Empty",
                                tint = SleekBorder,
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aucun équipement ne correspond aux filtres",
                                color = SleekTextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (eq in filteredEquipments) {
                            EquipmentItemCard(eq) {
                                viewModel.navigateTo(AppScreen.Detail(eq.id))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, count: String, indicatorColor: Color) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(105.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(indicatorColor, CircleShape)
                    .align(Alignment.End)
            )
            Text(count, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkBlueGrad)
            Text(label, fontSize = 11.sp, color = Color.Gray, overflow = TextOverflow.Ellipsis, maxLines = 1)
        }
    }
}

@Composable
fun EquipmentItemCard(eq: Equipement, onClick: () -> Unit) {
    val statusLower = eq.statut.lowercase()
    
    // Status-driven color configurations (Matching HTML designs)
    val (bgTint, iconTint, badgeBg, badgeText) = remember(statusLower) {
        when {
            statusLower == "opérationnel" -> {
                Quadruple(SleekSuccessBg, SleekSuccessText, SleekSuccessBg, SleekSuccessText)
            }
            statusLower == "en panne" || statusLower == "non conforme" -> {
                Quadruple(SleekErrorBg, SleekErrorText, SleekErrorBg, SleekErrorText)
            }
            statusLower == "en maintenance" -> {
                Quadruple(Color(0xFFFFE0B2), Color(0xFFE65100), Color(0xFFFFE0B2), Color(0xFFE65100))
            }
            else -> { // à contrôler, etc
                Quadruple(SleekPurpleLight, SleekPurpleDark, SleekPurpleLight, SleekPurpleDark)
            }
        }
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, SleekBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgTint),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (eq.type.lowercase()) {
                    "serveur web", "serveur" -> Icons.Default.Dns
                    "routeur core", "routeur", "switch" -> Icons.Default.Router
                    "baie stockage", "stockage" -> Icons.Default.Storage
                    "unité climatisation", "climatisation" -> Icons.Default.AcUnit
                    "onduleur central", "onduleur" -> Icons.Default.BatteryChargingFull
                    else -> Icons.Default.Cable
                }
                Icon(icon, contentDescription = eq.type, tint = iconTint, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eq.code_inventaire,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextDark
                )
                Text(
                    text = "${eq.nom_salle} • Étage 1",
                    fontSize = 11.sp,
                    color = SleekTextMuted
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = eq.type,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SleekTextDark.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${eq.const_fabricant})",
                        fontSize = 11.sp,
                        color = SleekTextMuted
                    )
                }
            }

            // High-fidelity Pill state badge
            Box(
                modifier = Modifier
                    .background(badgeBg, RoundedCornerShape(100.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (eq.statut.lowercase() == "opérationnel") "CONFORME" else eq.statut.uppercase(),
                    color = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Simple quad holder to replace Pair/Triple
data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)


// Simple haptic feedback helper
fun triggerLightHaptic(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(60)
            }
        }
    } catch (e: Exception) {
        Log.e("Haptics", "Haptic feedback helper failed: ${e.message}")
    }
}

// Distinct success haptic feedback vibration (double pulse)
fun triggerSuccessHaptic(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 80, 80, 150)
                val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }
        }
    } catch (e: Exception) {
        Log.e("Haptics", "Success haptic feedback helper failed: ${e.message}")
        triggerLightHaptic(context)
    }
}

// Camera scanner overlay framework + test override dropdown selector
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(viewModel: InventoryViewModel) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isContinuousScan by remember { mutableStateOf(false) }
    var inventoriedEquipements by remember { mutableStateOf(listOf<Equipement>()) }

    var isTorchOn by remember { mutableStateOf(false) }
    var cameraRef by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    LaunchedEffect(isTorchOn, cameraRef) {
        try {
            cameraRef?.cameraControl?.enableTorch(isTorchOn)
        } catch (e: Exception) {
            Log.e("ScanScreen", "Failed to toggle torch: ${e.message}", e)
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission asker result
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Capture equipment seed presets for easy debug clicking
    val equipementOptions by viewModel.equipements.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanner Code Équipement", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isTorchOn = !isTorchOn },
                        modifier = Modifier.testTag("torch_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flashlight",
                            tint = if (isTorchOn) SleekPurple else SleekTextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SleekPurpleBg,
                    titleContentColor = SleekTextDark,
                    navigationIconContentColor = SleekTextDark
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                // Live View CameraX View Finder
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().apply {
                                setSurfaceProvider(previewView.surfaceProvider)
                             }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                val cam = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview
                                )
                                cameraRef = cam
                            } catch (exc: Exception) {
                                Log.e("ScanScreen", "Use case binding failed", exc)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Autorisation caméra requise pour le scan QR",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Dark semi-transparent screen mask overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val measuredWidth = size.width
                val measuredHeight = size.height
                val centerOffset = Offset(measuredWidth / 2f, measuredHeight / 2f)
                val rectSize = 250.dp.toPx()

                // Cut a hole out for the scanner box
                val scanFramePath = Path().apply {
                    addRect(androidx.compose.ui.geometry.Rect(0f, 0f, measuredWidth, measuredHeight))
                }
                val scannerHolePath = Path().apply {
                    addRect(
                        androidx.compose.ui.geometry.Rect(
                            centerOffset.x - rectSize/2f,
                            centerOffset.y - rectSize/2f,
                            centerOffset.x + rectSize/2f,
                            centerOffset.y + rectSize/2f
                        )
                    )
                }

                val finalOverlay = Path.combine(
                    operation = PathOperation.Difference,
                    path1 = scanFramePath,
                    path2 = scannerHolePath
                )

                drawPath(finalOverlay, Color.Black.copy(alpha = 0.65f))

                // Draw SleekPurple scan frame outline
                drawRect(
                    color = SleekPurple,
                    topLeft = Offset(centerOffset.x - rectSize/2f, centerOffset.y - rectSize/2f),
                    size = androidx.compose.ui.geometry.Size(rectSize, rectSize),
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Visual details description
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Scannez le QR code de l'équipement",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Le capteur cherche le symbole encadré",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Continuous scan configuration row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mode Scan en continu",
                            color = SleekTextDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Valide automatiquement sans quitter l'appareil",
                            color = SleekTextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = isContinuousScan,
                        onCheckedChange = { isContinuousScan = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SleekPurple,
                            uncheckedThumbColor = SleekTextMuted,
                            uncheckedTrackColor = SleekBorder
                        ),
                        modifier = Modifier.testTag("continuous_scan_switch")
                    )
                }

                if (inventoriedEquipements.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                        border = BorderStroke(1.dp, SleekBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ÉQUIPEMENTS CONTRÔLÉS (${inventoriedEquipements.size})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekPurple
                                )
                                TextButton(
                                    onClick = { inventoriedEquipements = emptyList() },
                                    modifier = Modifier.height(24.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Effacer", fontSize = 10.sp, color = SleekTextMuted)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(inventoriedEquipements) { eqItem ->
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFE8F5E9), RoundedCornerShape(100.dp))
                                            .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(100.dp))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "ok",
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = eqItem.code_inventaire,
                                                fontSize = 11.sp,
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // EMULATOR DEBUG TOOL: Simulates QR Scanner click! (Sleek layout styled)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BugReport, contentDescription = "Emulator Sim", tint = SleekPurple)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Simulateur pour Émulateur Web",
                                color = SleekTextDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Dans l'émulateur AI Studio, cliquez sur l'un des équipements ci-dessous pour simuler son scan immédiat :",
                            color = SleekTextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (eq in equipementOptions) {
                                OutlinedButton(
                                    onClick = {
                                        triggerLightHaptic(context)
                                        try {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } catch (e: Exception) {}
                                        
                                        if (isContinuousScan) {
                                            viewModel.handleScannedCodeContinuous(eq.qr_code) { scannedEq ->
                                                if (scannedEq != null) {
                                                    triggerSuccessHaptic(context)
                                                    Toast.makeText(context, "Scanné & validé ! ${scannedEq.code_inventaire}", Toast.LENGTH_SHORT).show()
                                                    if (!inventoriedEquipements.any { it.id == scannedEq.id }) {
                                                        inventoriedEquipements = inventoriedEquipements + scannedEq
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Élément invalide", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            viewModel.handleScannedCode(eq.qr_code) { ok ->
                                                if (ok) {
                                                    triggerSuccessHaptic(context)
                                                } else {
                                                    Toast.makeText(context, "Élément invalide", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    border = BorderStroke(1.dp, SleekPurple),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekPurple),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(eq.code_inventaire, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(viewModel: InventoryViewModel, equipmentId: Int) {
    val equipementList by viewModel.equipements.collectAsState()
    val eq = equipementList.find { it.id == equipmentId }
    val controlesList by viewModel.controles.collectAsState()
    val anomaliesList by viewModel.anomalies.collectAsState()

    val eqControles = controlesList.filter { it.equipement_id == equipmentId }
    val eqAnoms = anomaliesList.filter { it.equipement_id == equipmentId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(eq?.code_inventaire ?: "Détails Équipement", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Dashboard) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SleekPurpleBg,
                    titleContentColor = SleekTextDark,
                    navigationIconContentColor = SleekTextDark
                )
            )
        }
    ) { innerPadding ->
        if (eq == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding), contentAlignment = Alignment.Center
            ) {
                Text("Équipement introuvable.", color = SleekTextDark)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(SleekPurpleBg)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Visual Card containing details (Refactored to high-contrast sleek white)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = eq.code_inventaire,
                                color = SleekPurple,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Status badge
                            val bColor = when (eq.statut) {
                                "opérationnel" -> SleekSuccessText
                                "à contrôler" -> SleekPurple
                                "en maintenance" -> Color(0xFFE65100)
                                else -> SleekErrorText
                            }
                            val bBg = when (eq.statut) {
                                "opérationnel" -> SleekSuccessBg
                                "à contrôler" -> SleekPurpleLight
                                "en maintenance" -> Color(0xFFFFE0B2)
                                else -> SleekErrorBg
                            }
                            Box(
                                modifier = Modifier
                                    .background(bBg, RoundedCornerShape(100.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (eq.statut.lowercase() == "opérationnel") "CONFORME" else eq.statut.uppercase(),
                                    color = bColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        InfoRow("Catégorie / Type", eq.type)
                        InfoRow("Constructeur", eq.const_fabricant)
                        InfoRow("Modèle précis", eq.const_modele)
                        InfoRow("Salle / Local", eq.nom_salle)
                        InfoRow("Dernier Contrôle", eq.date_dernier_controle ?: "Aucune date enregistrée")
                        InfoRow("QR Code / Tag ID", eq.qr_code)

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = SleekBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Rôle & Renseignements techniques :",
                            color = SleekTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = eq.description,
                            color = SleekTextDark,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Row action buttons (matching beautiful Sleek styles)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.Controle(eq.id)) },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPurple, contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(52.dp)
                            .testTag("controle_btn")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "check", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("FAIRE CONTRÔLE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.Anomalie(eq.id)) },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekErrorBg, contentColor = SleekErrorText),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SleekBorder),
                        modifier = Modifier
                            .weight(0.8f)
                            .height(52.dp)
                            .testTag("anomalie_btn")
                    ) {
                        Icon(Icons.Default.AddAlert, contentDescription = "alert", tint = SleekErrorText, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ANOMALIE", color = SleekErrorText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // PDF Exporter button
                val context = LocalContext.current
                Button(
                    onClick = {
                        exportCompliancePdf(context, eq, eqControles)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPurpleSurface, contentColor = SleekPurple),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SleekBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .height(52.dp)
                        .testTag("export_pdf_btn")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "pdf", tint = SleekPurple, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXPORTER RAPPORT PDF (SIGNÉ)", color = SleekPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Share button
                Button(
                    onClick = {
                        shareEquipmentDetails(context, eq, eqAnoms)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPurpleLight, contentColor = SleekPurple),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SleekBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .height(52.dp)
                        .testTag("share_equipment_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "share", tint = SleekPurple, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PARTAGER LA FICHE / RAPPORT", color = SleekPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Compliance Evolution Chart
                ComplianceEvolutionChart(eqControles)

                // History title
                Text(
                    text = "HISTORIQUE D'INSPECTIONS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextMuted,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )

                if (eqControles.isEmpty() && eqAnoms.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp), contentAlignment = Alignment.Center
                    ) {
                        Text("Aucun élément répertorié.", color = SleekTextMuted, fontSize = 13.sp)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (ctrl in eqControles) {
                            HistoryInspectionItem(ctrl)
                        }
                        for (anom in eqAnoms) {
                            HistoryAnomalieItem(anom)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = SleekTextMuted, fontSize = 13.sp)
        Text(text = value, color = SleekTextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun HistoryInspectionItem(ctrl: Controle) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isConforme = ctrl.statut.lowercase() == "conforme"
                    val badgeColor = if (isConforme) SleekSuccessText else SleekErrorText
                    val badgeBg = if (isConforme) SleekSuccessBg else SleekErrorBg
                    
                    Icon(
                        imageVector = Icons.Default.FactCheck,
                        contentDescription = "Test",
                        tint = badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    Box(
                        modifier = Modifier
                            .background(badgeBg, RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CONTRÔLE: ${ctrl.statut.uppercase()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = badgeColor
                        )
                    }
                }

                Text(ctrl.date_controle, fontSize = 11.sp, color = SleekTextMuted)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("Inspecteur: ${ctrl.inspecteur_nom}", fontSize = 12.sp, color = SleekTextDark)

            if (ctrl.notes.isNotEmpty()) {
                Text(
                    text = "Notes: ${ctrl.notes}",
                    fontSize = 11.sp,
                    color = SleekTextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Draw small simplified signature visualization
            if (ctrl.signature_path.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = SleekBorder)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Gesture, contentDescription = "signature", tint = SleekTextMuted, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Signature authentifiée localement (Enregistrée)", fontSize = 10.sp, color = SleekTextMuted)
                }
            }
        }
    }
}

@Composable
fun HistoryAnomalieItem(anom: Anomalie) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SleekErrorBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = SleekErrorText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Anomalie Signalée",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SleekErrorText
                    )
                }

                Text(anom.date_creation, fontSize = 11.sp, color = SleekTextMuted)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = anom.description,
                fontSize = 12.sp,
                color = SleekTextDark
            )

            // Dynamic photo if captured
            anom.photo_path?.let { uriPath ->
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(uriPath),
                    contentDescription = "Photo anomalie",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .border(1.dp, SleekBorder, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Statut: ${anom.statut.uppercase()}",
                    fontSize = 9.sp,
                    color = SleekErrorText,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControleScreen(viewModel: InventoryViewModel, equipmentId: Int) {
    val list by viewModel.equipements.collectAsState()
    val eq = list.find { it.id == equipmentId }
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    val draft = remember { viewModel.getControleDraft(equipmentId) }

    var step1Ok by remember { mutableStateOf(draft?.step1Ok ?: false) }
    var step2Ok by remember { mutableStateOf(draft?.step2Ok ?: false) }
    var step3Ok by remember { mutableStateOf(draft?.step3Ok ?: false) }
    var notes by remember { mutableStateOf(draft?.notes ?: "") }
    var isConforme by remember { mutableStateOf(draft?.isConforme ?: true) }

    // Touch event coordinates collection for custom Signature pad
    val signaturePath = remember {
        val pathList = mutableStateListOf<Offset>()
        draft?.signaturePath?.let { pathStr ->
            if (pathStr.isNotEmpty()) {
                val dots = pathStr.split(";")
                for (dot in dots) {
                    val parts = dot.split(",")
                    if (parts.size == 2) {
                        val px = parts[0].toFloatOrNull()
                        val py = parts[1].toFloatOrNull()
                        if (px != null && py != null) {
                            pathList.add(Offset(px, py))
                        }
                    }
                }
            }
        }
        pathList
    }

    // Auto-save draft on change to survive unexpected closures
    LaunchedEffect(step1Ok, step2Ok, step3Ok, isConforme, notes, signaturePath.size) {
        val signStr = signaturePath.joinToString(";") { "${it.x},${it.y}" }
        viewModel.saveControleDraft(
            equipmentId = equipmentId,
            step1Ok = step1Ok,
            step2Ok = step2Ok,
            step3Ok = step3Ok,
            isConforme = isConforme,
            notes = notes,
            signaturePath = signStr
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contrôle de Conformité", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SleekPurpleBg,
                    titleContentColor = SleekTextDark,
                    navigationIconContentColor = SleekTextDark
                )
            )
        }
    ) { innerPadding ->
        if (eq == null) {
            Box(modifier = Modifier.fillMaxSize()) { Text("Erreur", color = SleekTextDark) }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(SleekPurpleBg)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = "Vérifications de base - ${eq.code_inventaire}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextDark
                )
                Text(
                    text = "Cochez les composants critiques confirmés conformes.",
                    fontSize = 12.sp,
                    color = SleekTextMuted,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Checklist Items (Refactored to high-fidelity white cards)
                ChecklistRow(
                    text = "Vérification électrique (tension, câblage principal, disjoncteur)",
                    checked = step1Ok,
                    onCheckedChange = { step1Ok = it }
                )
                ChecklistRow(
                    text = "Vérification physique (absence d'usure anormale, propreté, filtre)",
                    checked = step2Ok,
                    onCheckedChange = { step2Ok = it }
                )
                ChecklistRow(
                    text = "Performance / Tests de fonctionnement (bruits internes, voyants LED conformes)",
                    checked = step3Ok,
                    onCheckedChange = { step3Ok = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Conformity selector radio
                Text("Verdict de conformité :", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isConforme,
                            onClick = { isConforme = true },
                            colors = RadioButtonDefaults.colors(selectedColor = SleekPurple)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Conforme (Opérationnel)", fontSize = 13.sp, color = SleekTextDark)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !isConforme,
                            onClick = { isConforme = false },
                            colors = RadioButtonDefaults.colors(selectedColor = SleekPurple)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Non conforme / Anomalie", fontSize = 13.sp, color = SleekTextDark)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes d'inspection...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = SleekPurple,
                        unfocusedBorderColor = SleekBorder,
                        focusedTextColor = SleekTextDark,
                        unfocusedTextColor = SleekTextDark
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Signature Pad Section
                Text(
                    text = "Signature de l'inspecteur (Dessinez ci-dessous) :",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    signaturePath.add(offset)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val currentPoint = signaturePath.lastOrNull()
                                    if (currentPoint != null) {
                                        signaturePath.add(currentPoint + dragAmount)
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Draw signature with a high-fidelity SleekPurple ink stroke
                        for (i in 0 until signaturePath.size - 1) {
                            val start = signaturePath[i]
                            val end = signaturePath[i + 1]
                            if (distance(start, end) < 100f) {
                                drawLine(
                                    color = SleekPurple,
                                    start = start,
                                    end = end,
                                    strokeWidth = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    if (signaturePath.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Signez ici avec votre doigt",
                                fontSize = 12.sp,
                                color = SleekTextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Reset signature button
                    IconButton(
                        onClick = { signaturePath.clear() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SleekErrorText)
                    }
                }

                // Auto-save feedback info row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Saved icon",
                        tint = SleekPurple,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Signature & progression sauvegardées en temps réel",
                        fontSize = 11.sp,
                        color = SleekPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save button (Sleek violet)
                Button(
                    onClick = {
                        triggerSuccessHaptic(context)
                        try {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        } catch (e: Exception) {}

                        val signStr = signaturePath.joinToString(";") { "${it.x},${it.y}" }
                        viewModel.saveControle(
                            equipmentId = equipmentId,
                            statut = if (isConforme) "conforme" else "non conforme",
                            notes = notes,
                            signatureData = signStr
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_report_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPurple, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("VALIDER ET ENREGISTRER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// Distance helper
private fun distance(p1: Offset, p2: Offset): Float {
    val dx = p1.x - p2.x
    val dy = p1.y - p2.y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

@Composable
fun ChecklistRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, SleekBorder, RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = SleekPurple)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, fontSize = 13.sp, color = SleekTextDark, lineHeight = 18.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnomalieScreen(viewModel: InventoryViewModel, equipmentId: Int) {
    val list by viewModel.equipements.collectAsState()
    val eq = list.find { it.id == equipmentId }

    var description by remember { mutableStateOf("") }
    var selectedPhotoStr by remember { mutableStateOf<String?>(null) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    var isCritical by remember { mutableStateOf(false) }

    var isDraftLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(equipmentId) {
        val draft = viewModel.getAnomalieDraft(equipmentId)
        if (draft != null) {
            description = draft.description
            selectedPhotoStr = draft.photo_path
            isCritical = draft.is_critical
        }
        isDraftLoaded = true
    }

    LaunchedEffect(description, selectedPhotoStr, isCritical, isDraftLoaded) {
        if (isDraftLoaded) {
            viewModel.saveAnomalieDraft(equipmentId, description, selectedPhotoStr, isCritical)
        }
    }

    val context = LocalContext.current
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                description = if (description.isEmpty()) spokenText else "$description $spokenText"
            }
        }
    }

    val presetFaultPhotos = listOf(
        Pair("Fumée/Chauffage", "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=400"),
        Pair("Composant Cassé", "https://images.unsplash.com/photo-1544256718-3bcf237f3974?w=400"),
        Pair("Fils Obstrués", "https://images.unsplash.com/photo-1596495574225-b0fdb682cdb4?w=400"),
        Pair("Court-circuit", "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=400")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouveau Mémento Anomalie", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SleekPurpleBg,
                    titleContentColor = SleekTextDark,
                    navigationIconContentColor = SleekTextDark
                )
            )
        }
    ) { innerPadding ->
        if (eq == null) {
            Box(modifier = Modifier.fillMaxSize()) { Text("Erreur", color = SleekTextDark) }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(SleekPurpleBg)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Signaler problème sur : ${eq.code_inventaire}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextDark
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description claire de l'Anomalie constatée") },
                    modifier = Modifier.fillMaxWidth().testTag("anomaly_desc_input"),
                    minLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = SleekPurple,
                        unfocusedBorderColor = SleekBorder,
                        focusedTextColor = SleekTextDark,
                        unfocusedTextColor = SleekTextDark
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Décrivez l'anomalie de vive voix...")
                                    }
                                    speechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    // Safe dictation simulation in virtualized environment
                                    val simulatedDictations = listOf(
                                        "Surchauffe anormale constatée sur le bloc d'alimentation.",
                                        "Bruit de frottement métallique persistant au démarrage.",
                                        "Le voyant LED d'alarme rouge clignote en continu.",
                                        "Fissure visible sur le capot protecteur externe.",
                                        "Perte d'étanchéité ou condensation suspecte."
                                    )
                                    val selected = simulatedDictations.random()
                                    description = if (description.isEmpty()) selected else "$description $selected"
                                    Toast.makeText(context, "Micro activé (Simulation dictée)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Dictée Vocale",
                                tint = SleekPurple,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )

                if (description.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Draft Save",
                            tint = SleekPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Brouillon sauvegardé automatiquement (Room)",
                            fontSize = 11.sp,
                            color = SleekPurple,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Gravity switch container
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Anomalie Critique / Majeure", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
                        Text("Déclenche une alerte de priorité système", fontSize = 11.sp, color = SleekTextMuted)
                    }
                    Switch(
                        checked = isCritical,
                        onCheckedChange = { isCritical = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SleekErrorText,
                            uncheckedThumbColor = SleekTextMuted,
                            uncheckedTrackColor = SleekBorder
                        )
                    )
                }

                // Photo Simulation Selector
                Text(
                    text = "Illustration / Photo de l'anomalie :",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextDark
                )

                if (selectedPhotoStr == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                            .clickable { showPhotoOptions = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "AddPhoto", tint = SleekPurple, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Prendre ou simuler une photo anomalie", fontSize = 11.sp, color = SleekTextMuted)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedPhotoStr),
                            contentDescription = "Fault Visual",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        IconButton(
                            onClick = { selectedPhotoStr = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (showPhotoOptions) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SleekPurpleSurface),
                        border = BorderStroke(1.dp, SleekBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "Simuler une capture d'anomalie sur le terrain :",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextDark
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (photo in presetFaultPhotos) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedPhotoStr = photo.second
                                            showPhotoOptions = false
                                        },
                                        border = BorderStroke(1.dp, SleekPurple),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekPurple)
                                    ) {
                                        Text(photo.first, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (description.isNotEmpty()) {
                            viewModel.saveAnomalie(
                                equipmentId = equipmentId,
                                description = description,
                                photoData = selectedPhotoStr,
                                isCritical = isCritical
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_anomalie_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPurple, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    enabled = description.isNotEmpty()
                ) {
                    Text("SIGNALER L'ANOMALIE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoriqueScreen(viewModel: InventoryViewModel) {
    val controles by viewModel.controles.collectAsState()
    val anomalies by viewModel.anomalies.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 -> Controls, 1 -> Anomalies
    var sortByNewest by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique Global des Inspections", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Dashboard) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SleekPurpleBg,
                    titleContentColor = SleekTextDark,
                    navigationIconContentColor = SleekTextDark
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SleekPurpleBg)
        ) {
            // Sleek Tab row with custom colors
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.White,
                contentColor = SleekPurple,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = SleekPurple
                    )
                }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Text(
                            text = "Contrôles de Conformité",
                            fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (activeTab == 0) SleekPurple else SleekTextMuted,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Text(
                            text = "Anomalies Signalées",
                            fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (activeTab == 1) SleekPurple else SleekTextMuted,
                            fontSize = 13.sp
                        )
                    }
                )
            }

            if (activeTab == 0) {
                if (controles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucun contrôle enregistré.", color = SleekTextMuted, fontSize = 14.sp)
                    }
                } else {
                    val sortedControles = remember(controles, sortByNewest) {
                        if (sortByNewest) {
                            controles.sortedByDescending { it.date_controle }
                        } else {
                            controles.sortedBy { it.date_controle }
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Inspections répertoriées : ${sortedControles.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextMuted
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { sortByNewest = !sortByNewest }
                                    .padding(4.dp)
                                    .testTag("sort_history_btn")
                            ) {
                                Icon(
                                    imageVector = if (sortByNewest) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = "Tri",
                                    tint = SleekPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (sortByNewest) "Récent en premier" else "Ancien en premier",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekPurple
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sortedControles) { ctrl ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, SleekBorder)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Équipement ID: ${ctrl.equipement_id}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = SleekPurple
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        HistoryInspectionItem(ctrl)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (anomalies.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucune anomalie enregistrée.", color = SleekTextMuted, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(anomalies) { anom ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, SleekBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Équipement ID: ${anom.equipement_id}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = SleekPurple
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HistoryAnomalieItem(anom)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComplianceEvolutionChart(controles: List<Controle>) {
    // Sort controls chronologically
    val sortedControles = controles.sortedBy { it.date_controle }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Évolution de la Conformité",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SleekPurple
            )
            Text(
                text = "Suivi chronologique de l'état de l'équipement",
                fontSize = 11.sp,
                color = SleekTextMuted
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (sortedControles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune inspection répertoriée pour ce matériel.",
                        fontSize = 12.sp,
                        color = SleekTextMuted
                    )
                }
            } else {
                // Draw a beautiful custom Canvas chart representing compliance over time
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    
                    // Draw grid/background lines
                    // Top path (CONFORME)
                    drawLine(
                        color = SleekBorder,
                        start = Offset(0f, 0f),
                        end = Offset(width, 0f),
                        strokeWidth = 1f
                    )
                    // Bottom path (NON CONFORME)
                    drawLine(
                        color = SleekBorder,
                        start = Offset(0f, height),
                        end = Offset(width, height),
                        strokeWidth = 1f
                    )
                    
                    val pointsCount = sortedControles.size
                    val xStep = if (pointsCount > 1) width / (pointsCount - 1) else width
                    
                    val points = sortedControles.mapIndexed { index, ctrl ->
                        val x = if (pointsCount > 1) index * xStep else width / 2
                        val y = if (ctrl.statut.lowercase() == "conforme") 0f else height
                        Offset(x, y)
                    }
                    
                    // Draw the continuous trend line
                    if (pointsCount > 1) {
                        for (i in 0 until pointsCount - 1) {
                            drawLine(
                                color = SleekPurpleLight,
                                start = points[i],
                                end = points[i+1],
                                strokeWidth = 3f
                            )
                        }
                    }
                    
                    // Draw each inspection point as color-coded bullet
                    points.forEachIndexed { index, offset ->
                        val isConforme = sortedControles[index].statut.lowercase() == "conforme"
                        val pointColor = if (isConforme) SleekSuccessText else SleekErrorText
                        
                        // Glow aura
                        drawCircle(
                            color = pointColor.copy(alpha = 0.2f),
                            radius = 12f,
                            center = offset
                        )
                        // Inner solid center
                        drawCircle(
                            color = pointColor,
                            radius = 6f,
                            center = offset
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Axis Label Legends
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("CONFORME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SleekSuccessText)
                    Text("NON CONFORME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SleekErrorText)
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = SleekBorder)
                Spacer(modifier = Modifier.height(6.dp))
                
                // Timeline horizontal dates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    sortedControles.forEachIndexed { index, ctrl ->
                        if (index == 0 || index == sortedControles.size - 1 || sortedControles.size <= 4) {
                            Text(
                                text = ctrl.date_controle.substringBefore(" "),
                                fontSize = 9.sp,
                                color = SleekTextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

fun exportCompliancePdf(context: Context, eq: Equipement, controles: List<Controle>) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard page structure
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        val paint = AndroidPaint()
        canvas.drawColor(android.graphics.Color.WHITE)
        
        // Custom branding background block
        paint.color = android.graphics.Color.parseColor("#6750A4")
        canvas.drawRect(0f, 0f, 595f, 90f, paint)
        
        // Brand details
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("RAPPORT ANALYTIQUE DE CONFORMITE", 30f, 40f, paint)
        
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Institut d'Inventaire Technique - Centre de Calcul", 30f, 65f, paint)
        
        // Date timestamp
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.FRANCE)
        val dateText = "Génie Documentaire : " + sdf.format(java.util.Date())
        paint.textAlign = AndroidPaint.Align.RIGHT
        canvas.drawText(dateText, 565f, 40f, paint)
        paint.textAlign = AndroidPaint.Align.LEFT
        
        // Section: Equipment description
        paint.color = android.graphics.Color.parseColor("#21005D")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("IDENTITE TECHNIQUE DE L'EQUIPEMENT", 30f, 130f, paint)
        
        paint.color = android.graphics.Color.parseColor("#EADDFF")
        canvas.drawRect(30f, 140f, 565f, 142f, paint)
        
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 11f
        var yPos = 170f
        
        fun drawRowSpec(label: String, valItem: String) {
            paint.isFakeBoldText = true
            canvas.drawText(label, 40f, yPos, paint)
            paint.isFakeBoldText = false
            canvas.drawText(valItem, 180f, yPos, paint)
            yPos += 22f
        }
        
        drawRowSpec("Code Inventaire :", eq.code_inventaire)
        drawRowSpec("Type / Catégorie :", eq.type)
        drawRowSpec("Emplacement :", eq.nom_salle)
        drawRowSpec("Manufacturier :", eq.const_fabricant)
        drawRowSpec("Modèle exact :", eq.const_modele)
        drawRowSpec("Référence QR Code :", eq.qr_code)
        
        paint.isFakeBoldText = true
        canvas.drawText("Statut Qualificatif :", 40f, yPos, paint)
        paint.isFakeBoldText = false
        val statusTextLabel = if (eq.statut.lowercase() == "opérationnel") "CONFORME / OPERATIONNEL" else eq.statut.uppercase()
        paint.color = if (eq.statut.lowercase() == "opérationnel") android.graphics.Color.parseColor("#4F6354") else android.graphics.Color.RED
        canvas.drawText(statusTextLabel, 180f, yPos, paint)
        paint.color = android.graphics.Color.BLACK
        
        yPos += 35f
        
        // Section: Inspection logging
        paint.color = android.graphics.Color.parseColor("#21005D")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("HISTORIQUE OFFICIEL DES CONTROLES", 30f, yPos, paint)
        
        paint.color = android.graphics.Color.parseColor("#EADDFF")
        canvas.drawRect(30f, yPos + 10f, 565f, yPos + 12f, paint)
        
        yPos += 35f
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 11f
        
        if (controles.isEmpty()) {
            paint.isFakeBoldText = false
            canvas.drawText("Aucun contrôle n'a été enregistré pour le moment.", 40f, yPos, paint)
            yPos += 20f
        } else {
            controles.sortedByDescending { it.date_controle }.take(4).forEach { ctrl ->
                paint.isFakeBoldText = true
                canvas.drawText("Date: ${ctrl.date_controle} | Inspecteur: ${ctrl.inspecteur_nom}", 40f, yPos, paint)
                
                paint.isFakeBoldText = false
                val isOk = ctrl.statut.lowercase() == "conforme"
                paint.color = if (isOk) android.graphics.Color.parseColor("#4F6354") else android.graphics.Color.RED
                canvas.drawText(ctrl.statut.uppercase(), 450f, yPos, paint)
                
                paint.color = android.graphics.Color.BLACK
                yPos += 15f
                if (ctrl.notes.isNotEmpty()) {
                    canvas.drawText("   Note: ${ctrl.notes}", 40f, yPos, paint)
                    yPos += 15f
                }
                yPos += 8f
            }
        }
        
        // Draw the signature of the inspector if available
        val matchedSignatureCtrl = controles.firstOrNull { it.signature_path.isNotEmpty() }
        if (matchedSignatureCtrl != null) {
            yPos += 20f
            paint.color = android.graphics.Color.parseColor("#21005D")
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("SIGNATURE DE L'INSPECTEUR TECHNIQUE ENREGISTREE", 30f, yPos, paint)
            
            yPos += 12f
            // Boundary Frame
            paint.color = android.graphics.Color.parseColor("#E3E3E3")
            canvas.drawRect(30f, yPos, 240f, yPos + 80f, paint)
            paint.color = android.graphics.Color.WHITE
            canvas.drawRect(31f, yPos + 1f, 239f, yPos + 79f, paint)
            
            val signaturePathStr = matchedSignatureCtrl.signature_path
            val pts = signaturePathStr.split(";").mapNotNull { dot ->
                val bits = dot.split(",")
                if (bits.size == 2) {
                    val px = bits[0].toFloatOrNull()
                    val py = bits[1].toFloatOrNull()
                    if (px != null && py != null) android.graphics.PointF(px, py) else null
                } else null
            }
            
            if (pts.isNotEmpty()) {
                val minX = pts.minOf { it.x }
                val maxX = pts.maxOf { it.x }
                val minY = pts.minOf { it.y }
                val maxY = pts.maxOf { it.y }
                
                val sourceW = if (maxX - minX > 0f) maxX - minX else 1f
                val sourceH = if (maxY - minY > 0f) maxY - minY else 1f
                
                val destW = 190f
                val destH = 60f
                val scale = Math.min(destW / sourceW, destH / sourceH)
                
                val linePaint = AndroidPaint().apply {
                    color = android.graphics.Color.parseColor("#6750A4")
                    strokeWidth = 2.5f
                    style = AndroidPaint.Style.STROKE
                    isAntiAlias = true
                }
                
                for (j in 0 until pts.size - 1) {
                    val currentPoint = pts[j]
                    val nextPoint = pts[j+1]
                    
                    val x1 = 40f + (currentPoint.x - minX) * scale
                    val y1 = yPos + 10f + (currentPoint.y - minY) * scale
                    val x2 = 40f + (nextPoint.x - minX) * scale
                    val y2 = yPos + 10f + (nextPoint.y - minY) * scale
                    
                    canvas.drawLine(x1, y1, x2, y2, linePaint)
                }
            }
            yPos += 95f
        }
        
        // Footer signature certification line
        paint.color = android.graphics.Color.parseColor("#D7D7D7")
        canvas.drawRect(30f, 800f, 565f, 801f, paint)
        paint.color = android.graphics.Color.GRAY
        paint.textSize = 8f
        paint.isFakeBoldText = false
        canvas.drawText("Ce document de conformité certifie l'inspection périodique des locaux datacenter de l'Institut.", 30f, 814f, paint)
        
        pdfDocument.finishPage(page)
        
        // Write out PDF file
        val filesDir = context.getExternalFilesDir(null) ?: context.cacheDir
        val destinationFile = File(filesDir, "Rapport_${eq.code_inventaire}.pdf")
        val outputStream = FileOutputStream(destinationFile)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()
        
        Toast.makeText(context, "Rapport PDF exporté à ${destinationFile.name}", Toast.LENGTH_SHORT).show()
        
        // Open PDF Share Menu layout
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            destinationFile
        )
        val viewIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(viewIntent, "Partager le rapport d'inspection PDF")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        Log.e("PDF_EX", "Error export PDF: ${e.message}")
        Toast.makeText(context, "Erreur lors de l'exportation: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun exportEquipmentListCsv(context: Context, equipments: List<Equipement>) {
    try {
        val csvBuilder = StringBuilder()
        csvBuilder.append("ID;Code Inventaire;Type;Salle;Fabricant;Modele;QR Code;Statut\n")
        
        equipments.forEach { eq ->
            csvBuilder.append("${eq.id};")
                .append("${eq.code_inventaire.replace(";", ",")};")
                .append("${eq.type.replace(";", ",")};")
                .append("${eq.nom_salle.replace(";", ",")};")
                .append("${eq.const_fabricant.replace(";", ",")};")
                .append("${eq.const_modele.replace(";", ",")};")
                .append("${eq.qr_code.replace(";", ",")};")
                .append("${eq.statut.replace(";", ",")}\n")
        }
        
        val filesDir = context.getExternalFilesDir(null) ?: context.cacheDir
        val destinationFile = File(filesDir, "Inventaire_Equipements.csv")
        destinationFile.writeText(csvBuilder.toString(), kotlin.text.Charsets.UTF_8)
        
        Toast.makeText(context, "CSV exporté à ${destinationFile.name}", Toast.LENGTH_SHORT).show()
        
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            destinationFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(shareIntent, "Partager l'inventaire CSV")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        Log.e("CSV_EXPORT", "Error creating CSV: ${e.message}")
        Toast.makeText(context, "Erreur lors de l'export CSV: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun shareEquipmentDetails(context: Context, eq: Equipement, anomalies: List<Anomalie>) {
    try {
        val shareBuilder = StringBuilder()
        shareBuilder.append("📋 FICHE TECHNIQUE TECHNIQUE & RAPPORT D'ANOMALIE\n")
        shareBuilder.append("=========================================\n")
        shareBuilder.append("Code Inventaire : ${eq.code_inventaire}\n")
        shareBuilder.append("Type/Catégorie : ${eq.type}\n")
        shareBuilder.append("Constructeur/Modèle : ${eq.const_fabricant} - ${eq.const_modele}\n")
        shareBuilder.append("Emplacement : ${eq.nom_salle}\n")
        shareBuilder.append("Statut actuel : ${eq.statut.uppercase()}\n")
        shareBuilder.append("QR Code UUID : ${eq.qr_code}\n")
        shareBuilder.append("Description : ${eq.description}\n")
        shareBuilder.append("Modifié le : ${eq.date_dernier_controle ?: "Non contrôlé"}\n\n")

        if (anomalies.isNotEmpty()) {
            shareBuilder.append("⚠️ DIRECTIVES / RAPPORT D'ANOMALIES CONSTATÉES :\n")
            anomalies.forEachIndexed { i, anom ->
                shareBuilder.append("  ${i + 1}. [${anom.statut.uppercase()}] ${anom.description} (Date: ${anom.date_creation})\n")
            }
        } else {
            shareBuilder.append("✅ Cet équipement ne répertorie aucune anomalie critique active.\n")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Fiche Technique : ${eq.code_inventaire}")
            putExtra(Intent.EXTRA_TEXT, shareBuilder.toString())
        }
        val chooser = Intent.createChooser(shareIntent, "Partager la fiche technique")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Log.e("SHARE_EQ", "Error sharing specs: ${e.message}")
        Toast.makeText(context, "Erreur lors du partage de la fiche : ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
