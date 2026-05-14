package com.example.paryavarankavalu

import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng

enum class ReportStatus {
    PENDING, CLEANED
}

enum class WasteType {
    PLASTIC, ORGANIC, ELECTRONIC, CHEMICAL, GENERAL
}

data class WasteReport(
    val id: String,
    val location: LatLng,
    val wasteType: WasteType,
    val photo: Bitmap?,
    val cleanedPhoto: Bitmap? = null, // New field for authority verification
    val status: ReportStatus = ReportStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis()
)
