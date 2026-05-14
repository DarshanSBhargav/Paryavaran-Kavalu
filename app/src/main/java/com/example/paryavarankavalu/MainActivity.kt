package com.example.paryavarankavalu

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.example.paryavarankavalu.ui.theme.ParyavaranKavaluTheme
import com.example.paryavarankavalu.ui.theme.PendingRed
import com.example.paryavarankavalu.ui.theme.CleanedGreen
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Rounded.Eco, null, Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Paryavaran-Kavalu", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Environmental Guardian System", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(Modifier.height(48.dp))
            RoleCard("Citizen Portal", "Report blackspots & earn Eco-Karma", Icons.Rounded.Person, MaterialTheme.colorScheme.primaryContainer) {
                onRoleSelected(UserRole.CITIZEN)
            }
            Spacer(Modifier.height(16.dp))
            RoleCard("Authority Monitor", "Monitor & Verify city cleanups", Icons.Rounded.AdminPanelSettings, MaterialTheme.colorScheme.secondaryContainer) {
                onRoleSelected(UserRole.AUTHORITY)
            }
        }
    }
}

@Composable
fun RoleCard(title: String, desc: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().height(100.dp), colors = CardDefaults.cardColors(containerColor = color)) {
        Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(desc, style = MaterialTheme.typography.bodySmall)
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

    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACCESS_MEDIA_LOCATION)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissions.toTypedArray())
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.builder().target(LatLng(12.9716, 77.5946)).zoom(18f).tilt(45f).build()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (role == UserRole.CITIZEN) "Guardian Portal" else "Authority Monitor", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { isMapView = !isMapView }) {
                        Icon(if (isMapView) Icons.AutoMirrored.Rounded.List else Icons.Rounded.Map, null)
                    }
                    if (role == UserRole.CITIZEN) {
                        XPBadge(viewModel.ecoKarmaPoints.value) { showLeaderboard = true }
                    }
                }
            )
        },
        floatingActionButton = {
            if (role == UserRole.CITIZEN) {
                ExtendedFloatingActionButton(
                    onClick = { showReportDialog = true },
                    icon = { Icon(Icons.AutoMirrored.Rounded.NoteAdd, null) },
                    text = { Text("Report Blackspot") }
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
                ModalBottomSheet(onDismissRequest = { selectedReport = null }) {
                    ReportDetailContent(selectedReport!!, role == UserRole.AUTHORITY) { evidence ->
                        viewModel.markAsCleaned(selectedReport!!.id, evidence)
                        selectedReport = null
                    }
                }
            }
            if (isLocating) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), Alignment.Center) {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Geo-Tagging Blackspot...", style = MaterialTheme.typography.bodyMedium)
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
                        val finalLocation = photoLocation ?: getOneTimeLocation(context)?.let { LatLng(it.latitude, it.longitude) }
                        if (finalLocation != null) {
                            viewModel.addReport(finalLocation, type, photo)
                            Toast.makeText(context, "Reported! +15 XP", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Location Error.", Toast.LENGTH_SHORT).show()
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

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                val b = loadBitmapFromUri(context, it)
                bitmap = b?.let { compressBitmap(it) }
                photoLocation = extractLocationFromUri(context, it)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            scope.launch {
                val b = loadBitmapFromUri(context, photoUri!!)
                bitmap = b?.let { compressBitmap(it) }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lodge Complaint", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Select Category:", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                WasteType.entries.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { type ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.name, fontSize = 10.sp) },
                                leadingIcon = { Icon(getWasteIcon(type), null, Modifier.size(14.dp)) }
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (bitmap == null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val file = File(context.cacheDir, "report_${System.currentTimeMillis()}.jpg")
                            photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            cameraLauncher.launch(photoUri!!)
                        }, Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Rounded.PhotoCamera, null)
                            Text("Camera", Modifier.padding(start = 4.dp))
                        }
                        OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Rounded.PhotoLibrary, null)
                            Text("Gallery", Modifier.padding(start = 4.dp))
                        }
                    }
                } else {
                    Box(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp))) {
                        Image(bitmap!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        IconButton(onClick = { bitmap = null; photoLocation = null }, Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.4f), CircleShape)) {
                            Icon(Icons.Rounded.Close, null, tint = Color.White)
                        }
                    }
                }
                Text("Photo auto-optimized (< 500KB)", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { Button(onClick = { onReport(selectedType, bitmap, photoLocation) }) { Text("Submit") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
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
            Text(report.wasteType.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            StatusChip(report.status)
        }
        Spacer(Modifier.height(16.dp))
        Text("Reported Hazard:", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        if (report.photo != null) {
            Image(report.photo.asImageBitmap(), null, Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
        }
        if (report.status == ReportStatus.CLEANED) {
            Spacer(Modifier.height(24.dp))
            Text("Authority Proof (After):", style = MaterialTheme.typography.labelLarge, color = CleanedGreen)
            if (report.cleanedPhoto != null) {
                Image(report.cleanedPhoto.asImageBitmap(), null, Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
            }
        }
        Spacer(Modifier.height(24.dp))
        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)) {
            Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Location: ${"%.4f".format(report.location.latitude)}, ${"%.4f".format(report.location.longitude)}", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (isAuthority && report.status == ReportStatus.PENDING) {
            Spacer(Modifier.height(24.dp))
            Text("Cleanup Evidence:", style = MaterialTheme.typography.labelLarge)
            if (evidenceBitmap == null) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val file = File(context.cacheDir, "cleaned_${System.currentTimeMillis()}.jpg")
                        photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        cameraLauncher.launch(photoUri!!)
                    }, Modifier.weight(1f)) {
                        Icon(Icons.Rounded.AddAPhoto, null)
                        Text("Camera")
                    }
                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, Modifier.weight(1f)) {
                        Icon(Icons.Rounded.PhotoLibrary, null)
                        Text("Gallery")
                    }
                }
            } else {
                Image(evidenceBitmap!!.asImageBitmap(), null, Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onMarkCleaned(evidenceBitmap) }, Modifier.fillMaxWidth().height(56.dp)) {
                Text("VERIFY & RESOLVE")
            }
        }
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
        properties = MapProperties(isMyLocationEnabled = true, mapType = MapType.HYBRID)
    ) {
        viewModel.reports.forEach { report ->
            Marker(state = MarkerState(position = report.location), title = report.wasteType.name, icon = BitmapDescriptorFactory.defaultMarker(if (report.status == ReportStatus.PENDING) BitmapDescriptorFactory.HUE_RED else BitmapDescriptorFactory.HUE_GREEN), onClick = { onSelect(report); true })
        }
    }
}

@Composable
fun ListViewContainer(viewModel: WasteViewModel, onSelect: (WasteReport) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(viewModel.reports.reversed()) { report ->
            Card(onClick = { onSelect(report) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), Alignment.Center) {
                        Icon(getWasteIcon(report.wasteType), null, Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(report.wasteType.name, fontWeight = FontWeight.Bold)
                        Text(SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(report.timestamp)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
    Surface(color = if (status == ReportStatus.PENDING) PendingRed.copy(alpha = 0.1f) else CleanedGreen.copy(alpha = 0.1f), shape = CircleShape, border = BorderStroke(1.dp, if (status == ReportStatus.PENDING) PendingRed else CleanedGreen)) {
        Text(status.name, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = if (status == ReportStatus.PENDING) PendingRed else CleanedGreen, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun XPBadge(points: Int, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.padding(end = 8.dp).clickable { onClick() }) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Star, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(4.dp))
            Text("$points XP", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun LeaderboardDialog(leaderboard: List<UserXP>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eco-Karma Leaderboard", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                itemsIndexed(leaderboard) { index, user ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "${index + 1}", modifier = Modifier.width(32.dp), fontWeight = FontWeight.Bold, color = if (user.isMe) MaterialTheme.colorScheme.primary else Color.Gray)
                        Box(Modifier.size(32.dp).clip(CircleShape).background(if (user.isMe) MaterialTheme.colorScheme.primary else Color.LightGray), contentAlignment = Alignment.Center) {
                            Text(user.name.take(1), color = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(user.name, Modifier.weight(1f), fontWeight = if (user.isMe) FontWeight.Bold else FontWeight.Normal)
                        Text("${user.points} XP", fontWeight = FontWeight.Black)
                    }
                    if (index < leaderboard.size - 1) HorizontalDivider(thickness = 0.5.dp)
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Awesome!") } }
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
    val stream = ByteArrayOutputStream()
    var q = 90
    bitmap.compress(Bitmap.CompressFormat.JPEG, q, stream)
    while (stream.toByteArray().size > 500 * 1024 && q > 10) {
        stream.reset(); q -= 10
        bitmap.compress(Bitmap.CompressFormat.JPEG, q, stream)
    }
    BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())
}

@SuppressLint("MissingPermission")
suspend fun getOneTimeLocation(context: Context): Location? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    return try { client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await() } catch (e: Exception) { null }
}
