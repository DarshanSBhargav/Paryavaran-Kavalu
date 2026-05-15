package com.example.paryavarankavalu

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.example.paryavarankavalu.ui.theme.ParyavaranKavaluTheme
import com.example.paryavarankavalu.ui.theme.PendingRed
import com.example.paryavarankavalu.ui.theme.CleanedGreen
import com.example.paryavarankavalu.ui.theme.EcoGreen
import com.example.paryavarankavalu.ui.theme.EcoLightGreen
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class UserRole { CITIZEN, AUTHORITY }

class MainActivity : ComponentActivity() {
    private val viewModel: WasteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParyavaranKavaluTheme {
                var currentRole by remember { mutableStateOf<UserRole?>(null) }
                if (currentRole == null) {
                    RoleSelectionScreen { currentRole = it }
                } else {
                    MainAppContent(viewModel, currentRole!!) { currentRole = null }
                }
            }
        }
    }
}

@Composable
fun RoleSelectionScreen(onRoleSelected: (UserRole) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(EcoLightGreen.copy(alpha = 0.2f), Color.White)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = EcoGreen.copy(alpha = 0.1f),
                modifier = Modifier.size(140.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Eco,
                        null,
                        Modifier.size(80.dp),
                        tint = EcoGreen
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Paryavaran-Kavalu",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = EcoGreen,
                textAlign = TextAlign.Center
            )
            Text(
                "Environmental Guardian System",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(48.dp))
            RoleCard(
                "Citizen Portal",
                "Report blackspots & earn Eco-Karma",
                Icons.Rounded.Person,
                MaterialTheme.colorScheme.primaryContainer
            ) {
                onRoleSelected(UserRole.CITIZEN)
            }
            Spacer(Modifier.height(16.dp))
            RoleCard(
                "Authority Monitor",
                "Monitor & Verify city cleanups",
                Icons.Rounded.AdminPanelSettings,
                MaterialTheme.colorScheme.secondaryContainer
            ) {
                onRoleSelected(UserRole.AUTHORITY)
            }
        }
    }
}

@Composable
fun RoleCard(title: String, desc: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(30.dp), tint = Color.DarkGray)
                }
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(desc, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray.copy(alpha = 0.7f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: WasteViewModel, role: UserRole, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isMapView by remember { mutableStateOf(true) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showLeaderboard by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf<WasteReport?>(null) }
    var isLocating by remember { mutableStateOf(false) }

    val permissions = remember {
        mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_MEDIA_LOCATION)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissions)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.builder().target(LatLng(12.9716, 77.5946)).zoom(18f).tilt(45f).build()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (role == UserRole.CITIZEN) "Guardian Portal" else "Authority Monitor", 
                        fontWeight = FontWeight.ExtraBold,
                        color = EcoGreen
                    ) 
                },
                navigationIcon = { 
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = EcoGreen) 
                    } 
                },
                actions = {
                    IconButton(onClick = { isMapView = !isMapView }) {
                        Icon(if (isMapView) Icons.AutoMirrored.Rounded.List else Icons.Rounded.Map, null, tint = EcoGreen)
                    }
                    if (role == UserRole.CITIZEN) {
                        XPBadge(viewModel.ecoKarmaPoints.value) { showLeaderboard = true }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                )
            )
        },
        floatingActionButton = {
            if (role == UserRole.CITIZEN) {
                ExtendedFloatingActionButton(
                    onClick = { showReportDialog = true },
                    containerColor = EcoGreen,
                    contentColor = Color.White,
                    icon = { Icon(Icons.AutoMirrored.Rounded.NoteAdd, null) },
                    text = { Text("Report Blackspot") },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize()) {
            if (isMapView) {
                MapViewContainer(cameraPositionState, viewModel) { selectedReport = it }
            } else {
                ListViewContainer(viewModel) { selectedReport = it }
            }
            if (selectedReport != null) {
                ModalBottomSheet(
                    onDismissRequest = { selectedReport = null },
                    dragHandle = { BottomSheetDefaults.DragHandle(color = EcoGreen) },
                    containerColor = Color.White
                ) {
                    ReportDetailContent(selectedReport!!, role == UserRole.AUTHORITY) { evidence ->
                        viewModel.markAsCleaned(selectedReport!!.id, evidence)
                        selectedReport = null
                    }
                }
            }
            if (isLocating) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), Alignment.Center) {
                    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = EcoGreen, strokeWidth = 4.dp)
                            Spacer(Modifier.height(24.dp))
                            Text("Geo-Tagging Blackspot...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Fetching precise location", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
            if (showLeaderboard) LeaderboardDialog(viewModel.leaderboard) { showLeaderboard = false }
        }
        if (showReportDialog) {
            ReportWasteDialog(
                onDismiss = { showReportDialog = false },
                onReport = { type, photo, photoLocation ->
                    isLocating = true
                    scope.launch {
                        // Optimizing location fetch: using a timeout to prevent "infinite" waiting
                        val finalLocation = photoLocation ?: try {
                            withTimeout(5000) { // 5 second timeout
                                getOneTimeLocation(context)?.let { LatLng(it.latitude, it.longitude) }
                            }
                        } catch (e: Exception) {
                            // Fallback to last known if current fails or times out
                            getLastKnownLocation(context)?.let { LatLng(it.latitude, it.longitude) }
                        }
                        
                        if (finalLocation != null) {
                            viewModel.addReport(finalLocation, type, photo)
                            Toast.makeText(context, "Reported! +15 Eco-Karma", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Location Error. Please ensure GPS is ON.", Toast.LENGTH_LONG).show()
                        }
                        isLocating = false
                        showReportDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun ReportWasteDialog(onDismiss: () -> Unit, onReport: (WasteType, Bitmap?, LatLng?) -> Unit) {
    var selectedType by remember { mutableStateOf(WasteType.GENERAL) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoLocation by remember { mutableStateOf<LatLng?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                isProcessing = true
                val b = loadBitmapFromUri(context, it)
                bitmap = b?.let { compressBitmap(it) }
                photoLocation = extractLocationFromUri(context, it)
                isProcessing = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            scope.launch {
                isProcessing = true
                val b = loadBitmapFromUri(context, photoUri!!)
                bitmap = b?.let { compressBitmap(it) }
                isProcessing = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Lodge Complaint", fontWeight = FontWeight.ExtraBold, color = EcoGreen) },
        text = {
            Column {
                Text("Select Category:", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                WasteType.entries.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { type ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(getWasteIcon(type), null, Modifier.size(16.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EcoGreen,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(20.dp))
                if (bitmap == null && !isProcessing) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val file = File(context.cacheDir, "report_${System.currentTimeMillis()}.jpg")
                                photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                cameraLauncher.launch(photoUri!!)
                            }, 
                            Modifier.weight(1f), 
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EcoGreen)
                        ) {
                            Icon(Icons.Rounded.PhotoCamera, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Camera")
                        }
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") }, 
                            Modifier.weight(1f), 
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EcoGreen)
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, null, tint = EcoGreen)
                            Spacer(Modifier.width(4.dp))
                            Text("Gallery", color = EcoGreen)
                        }
                    }
                } else if (isProcessing) {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EcoGreen)
                    }
                } else {
                    Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))) {
                        Image(bitmap!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        IconButton(
                            onClick = { bitmap = null; photoLocation = null }, 
                            Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Auto-optimized (< 500KB) for fast upload", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        },
        confirmButton = { 
            Button(
                onClick = { if (!isProcessing) onReport(selectedType, bitmap, photoLocation) },
                enabled = bitmap != null && !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = EcoGreen),
                shape = RoundedCornerShape(12.dp)
            ) { 
                Text("Submit Report", fontWeight = FontWeight.Bold) 
            } 
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("Cancel", color = Color.Gray) 
            } 
        }
    )
}

@Composable
fun ReportDetailContent(report: WasteReport, isAuthority: Boolean, onMarkCleaned: (Bitmap?) -> Unit) {
    var evidenceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                val b = loadBitmapFromUri(context, it)
                evidenceBitmap = b?.let { compressBitmap(it) }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            scope.launch {
                val b = loadBitmapFromUri(context, photoUri!!)
                evidenceBitmap = b?.let { compressBitmap(it) }
            }
        }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(report.wasteType.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = EcoGreen)
            StatusChip(report.status)
        }
        Text(
            SimpleDateFormat("EEEE, dd MMMM yyyy • hh:mm a", Locale.getDefault()).format(Date(report.timestamp)), 
            style = MaterialTheme.typography.bodySmall, 
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text("Visual Evidence:", style = MaterialTheme.typography.labelLarge, color = EcoGreen)
        Spacer(Modifier.height(8.dp))
        if (report.photo != null) {
            Image(report.photo.asImageBitmap(), null, Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
        }
        
        if (report.status == ReportStatus.CLEANED) {
            Spacer(Modifier.height(24.dp))
            Text("Verification Proof (After):", style = MaterialTheme.typography.labelLarge, color = CleanedGreen)
            Spacer(Modifier.height(8.dp))
            if (report.cleanedPhoto != null) {
                Image(report.cleanedPhoto.asImageBitmap(), null, Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)).border(2.dp, CleanedGreen, RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Text("Satellite Location:", style = MaterialTheme.typography.labelLarge, color = EcoGreen)
        Spacer(Modifier.height(8.dp))
        
        // Mini Satellite Map view for the blackspot
        Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(20.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(20.dp))) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(report.location, 19f)
                },
                properties = MapProperties(mapType = MapType.SATELLITE),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, scrollGesturesEnabled = false, tiltGesturesEnabled = false)
            ) {
                Marker(state = MarkerState(position = report.location))
            }
        }
        
        Surface(
            color = EcoLightGreen.copy(alpha = 0.1f), 
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, null, tint = EcoGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("${"%.6f".format(report.location.latitude)}, ${"%.6f".format(report.location.longitude)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
        
        if (isAuthority && report.status == ReportStatus.PENDING) {
            Spacer(Modifier.height(32.dp))
            Text("Resolution Evidence Required:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (evidenceBitmap == null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val file = File(context.cacheDir, "cleaned_${System.currentTimeMillis()}.jpg")
                            photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            cameraLauncher.launch(photoUri!!)
                        }, 
                        Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CleanedGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.AddAPhoto, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Capture")
                    }
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") }, 
                        Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CleanedGreen)
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, null, tint = CleanedGreen)
                        Spacer(Modifier.width(4.dp))
                        Text("Gallery", color = CleanedGreen)
                    }
                }
            } else {
                Box {
                    Image(evidenceBitmap!!.asImageBitmap(), null, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                    IconButton(onClick = { evidenceBitmap = null }, Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                        Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onMarkCleaned(evidenceBitmap) }, 
                enabled = evidenceBitmap != null,
                Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CleanedGreen)
            ) {
                Text("VERIFY & RESOLVE SPOT", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

private fun extractLocationFromUri(context: Context, uri: Uri): LatLng? {
    return try {
        val originalUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try { MediaStore.setRequireOriginal(uri) } catch (e: Exception) { uri }
        } else uri
        context.contentResolver.openInputStream(originalUri)?.use { stream ->
            val exif = ExifInterface(stream)
            val latLong = exif.latLong
            if (latLong != null) LatLng(latLong[0], latLong[1]) else null
        }
    } catch (e: Exception) { null }
}

@Composable
fun MapViewContainer(cameraState: CameraPositionState, viewModel: WasteViewModel, onSelect: (WasteReport) -> Unit) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraState,
        properties = MapProperties(isMyLocationEnabled = true, mapType = MapType.SATELLITE)
    ) {
        viewModel.reports.forEach { report ->
            Marker(
                state = MarkerState(position = report.location), 
                title = report.wasteType.name, 
                icon = BitmapDescriptorFactory.defaultMarker(if (report.status == ReportStatus.PENDING) BitmapDescriptorFactory.HUE_RED else BitmapDescriptorFactory.HUE_GREEN), 
                onClick = { onSelect(report); true }
            )
        }
    }
}

@Composable
fun ListViewContainer(viewModel: WasteViewModel, onSelect: (WasteReport) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp), 
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(viewModel.reports.reversed()) { report ->
            Card(
                onClick = { onSelect(report) }, 
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(EcoGreen.copy(alpha = 0.1f)), Alignment.Center) {
                        Icon(getWasteIcon(report.wasteType), null, Modifier.size(28.dp), tint = EcoGreen)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(report.wasteType.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(report.timestamp)), 
                            style = MaterialTheme.typography.bodySmall, 
                            color = Color.Gray
                        )
                    }
                    StatusChip(report.status)
                }
            }
        }
    }
}

fun getWasteIcon(type: WasteType): ImageVector = when(type) {
    WasteType.PLASTIC -> Icons.Rounded.Inventory
    WasteType.ORGANIC -> Icons.Rounded.Eco
    WasteType.ELECTRONIC -> Icons.Rounded.Devices
    WasteType.CHEMICAL -> Icons.Rounded.Warning
    WasteType.GENERAL -> Icons.Rounded.DeleteOutline
}

@Composable
fun StatusChip(status: ReportStatus) {
    val color = if (status == ReportStatus.PENDING) PendingRed else CleanedGreen
    Surface(
        color = color.copy(alpha = 0.12f), 
        shape = RoundedCornerShape(8.dp), 
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            status.name, 
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp), 
            style = MaterialTheme.typography.labelSmall, 
            color = color, 
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun XPBadge(points: Int, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp), 
        color = EcoGreen, 
        modifier = Modifier.padding(end = 8.dp).clickable { onClick() }
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Star, null, Modifier.size(18.dp), tint = Color.White)
            Spacer(Modifier.width(6.dp))
            Text("$points Karma", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

@Composable
fun LeaderboardDialog(leaderboard: List<UserXP>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = EcoGreen, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Text("Eco-Karma Leaders", fontWeight = FontWeight.ExtraBold, color = EcoGreen) 
            }
        },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                itemsIndexed(leaderboard) { index, user ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}", 
                            modifier = Modifier.width(32.dp), 
                            fontWeight = FontWeight.Black, 
                            color = if (user.isMe) EcoGreen else Color.Gray,
                            fontSize = 18.sp
                        )
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(if (user.isMe) EcoGreen else Color.LightGray.copy(alpha = 0.5f)), 
                            contentAlignment = Alignment.Center
                        ) {
                            Text(user.name.take(1), color = if (user.isMe) Color.White else Color.DarkGray, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            user.name, 
                            Modifier.weight(1f), 
                            fontWeight = if (user.isMe) FontWeight.ExtraBold else FontWeight.Medium,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text("${user.points} XP", fontWeight = FontWeight.Black, color = EcoGreen)
                    }
                    if (index < leaderboard.size - 1) HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        },
        confirmButton = { 
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EcoGreen),
                shape = RoundedCornerShape(12.dp)
            ) { 
                Text("Great!", fontWeight = FontWeight.Bold) 
            } 
        }
    )
}

suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) { null }
}

suspend fun compressBitmap(bitmap: Bitmap): Bitmap = withContext(Dispatchers.IO) {
    // Optimization: Scale down if the image is too large before compressing
    val maxDimension = 1200
    val originalWidth = bitmap.width
    val originalHeight = bitmap.height
    
    val scaledBitmap = if (originalWidth > maxDimension || originalHeight > maxDimension) {
        val scale = maxDimension.toFloat() / Math.max(originalWidth, originalHeight)
        Bitmap.createScaledBitmap(
            bitmap, 
            (originalWidth * scale).toInt(), 
            (originalHeight * scale).toInt(), 
            true
        )
    } else {
        bitmap
    }

    val stream = ByteArrayOutputStream()
    var q = 80 // Start with 80 quality for speed
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, q, stream)
    
    // Quick exit if already small enough
    if (stream.size() > 500 * 1024) {
        while (stream.size() > 500 * 1024 && q > 20) {
            stream.reset(); q -= 20
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, q, stream)
        }
    }
    
    BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())
}

@SuppressLint("MissingPermission")
suspend fun getOneTimeLocation(context: Context): Location? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    return try { 
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await() 
    } catch (e: Exception) { null }
}

@SuppressLint("MissingPermission")
suspend fun getLastKnownLocation(context: Context): Location? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    return try { client.lastLocation.await() } catch (e: Exception) { null }
}
