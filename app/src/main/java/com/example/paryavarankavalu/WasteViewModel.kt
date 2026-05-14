package com.example.paryavarankavalu

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import java.util.UUID

data class UserXP(val name: String, val points: Int, val isMe: Boolean = false)

class WasteViewModel : ViewModel() {
    private val _reports = mutableStateListOf<WasteReport>()
    val reports: List<WasteReport> get() = _reports

    val ecoKarmaPoints = mutableStateOf(0)
    
    // Mock Leaderboard using SnapshotStateList for reactivity
    val leaderboard = listOf(
        UserXP("Arjun S.", 450),
        UserXP("Priya K.", 380),
        UserXP("You", 0, isMe = true),
        UserXP("Rahul M.", 210),
        UserXP("Sneha V.", 150)
    ).toMutableStateList()

    init {
        // Initial Community Data for Bangalore area
        addMockReport(LatLng(12.9716, 77.5946), WasteType.PLASTIC, ReportStatus.PENDING)
        addMockReport(LatLng(12.9850, 77.6100), WasteType.CHEMICAL, ReportStatus.PENDING)
        addMockReport(LatLng(12.9500, 77.5800), WasteType.ELECTRONIC, ReportStatus.PENDING)
        addMockReport(LatLng(12.9650, 77.5950), WasteType.GENERAL, ReportStatus.CLEANED)
    }

    private fun addMockReport(location: LatLng, type: WasteType, status: ReportStatus) {
        _reports.add(
            WasteReport(
                id = UUID.randomUUID().toString(),
                location = location,
                wasteType = type,
                photo = null,
                status = status
            )
        )
    }

    fun addReport(location: LatLng, wasteType: WasteType, photo: Bitmap?) {
        val newReport = WasteReport(
            id = UUID.randomUUID().toString(),
            location = location,
            wasteType = wasteType,
            photo = photo
        )
        _reports.add(newReport)
        updatePoints(15) // Reward for pinning a new spot
    }

    fun markAsCleaned(reportId: String, cleanedPhoto: Bitmap?) {
        val index = _reports.indexOfFirst { it.id == reportId }
        if (index != -1) {
            val report = _reports[index]
            if (report.status == ReportStatus.PENDING) {
                _reports[index] = report.copy(
                    status = ReportStatus.CLEANED,
                    cleanedPhoto = cleanedPhoto
                )
                updatePoints(50) // High reward for verified cleanup
            }
        }
    }

    private fun updatePoints(earned: Int) {
        ecoKarmaPoints.value += earned
        
        // Update "You" in the leaderboard
        val myIndex = leaderboard.indexOfFirst { it.isMe }
        if (myIndex != -1) {
            leaderboard[myIndex] = leaderboard[myIndex].copy(points = ecoKarmaPoints.value)
            
            // Re-sort the leaderboard list to reflect rank changes
            val currentList = leaderboard.toList().sortedByDescending { it.points }
            leaderboard.clear()
            leaderboard.addAll(currentList)
        }
    }
}
