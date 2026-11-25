package com.planner.tracker.util

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val locationName: String?
)

object LocationHelper {

    /**
     * 현재 위치를 가져옵니다. 위치 권한이 없거나 실패 시 null 반환.
     * 최대 10초 대기.
     */
    suspend fun getCurrentLocation(context: Context): LocationResult? =
        withTimeoutOrNull(10_000L) {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cancellationSource = CancellationTokenSource()

                val location: Location? = suspendCancellableCoroutine { cont ->
                    cont.invokeOnCancellation { cancellationSource.cancel() }
                    try {
                        @Suppress("MissingPermission")
                        fusedClient.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            cancellationSource.token
                        ).addOnSuccessListener { loc: Location? ->
                            cont.resume(loc)
                        }.addOnFailureListener {
                            cont.resume(null)
                        }
                    } catch (e: SecurityException) {
                        cont.resume(null)
                    }
                }

                if (location == null) return@withTimeoutOrNull null

                val lat = location.latitude
                val lng = location.longitude
                val name = reverseGeocode(context, lat, lng)
                LocationResult(lat, lng, name)
            } catch (e: Exception) {
                null
            }
        }

    /**
     * 위도/경도를 주소(동/구 단위)로 변환합니다.
     */
    private suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.KOREA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine<String?> { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val addr = addresses.firstOrNull()
                        val name = formatAddress(addr?.thoroughfare, addr?.subLocality, addr?.locality, addr?.adminArea)
                        cont.resume(name)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val addr = addresses?.firstOrNull()
                formatAddress(addr?.thoroughfare, addr?.subLocality, addr?.locality, addr?.adminArea)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 주소 구성요소를 "구 동" 형식으로 조합합니다.
     */
    private fun formatAddress(
        thoroughfare: String?,   // 도로명/동
        subLocality: String?,    // 동/읍
        locality: String?,       // 시/군/구
        adminArea: String?       // 도/특별시
    ): String? {
        val parts = listOfNotNull(locality, subLocality ?: thoroughfare)
        return if (parts.isNotEmpty()) parts.joinToString(" ") else adminArea
    }
}
