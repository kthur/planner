package com.planner.tracker.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

object WearDataManager {
    private const val TAG = "WearDataManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    fun syncCategories(context: Context, categories: List<CategoryEntity>) {
        scope.launch {
            try {
                val jsonArray = JSONArray()
                categories.forEach { category ->
                    val obj = JSONObject().apply {
                        put("name", category.name)
                        put("displayName", category.displayName)
                        put("colorHex", category.colorHex)
                        put("entryType", category.entryType)
                    }
                    jsonArray.put(obj)
                }

                val request = PutDataMapRequest.create("/categories").apply {
                    dataMap.putString("json", jsonArray.toString())
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }
                
                Wearable.getDataClient(context).putDataItem(request.asPutDataRequest())
                Log.d(TAG, "Successfully synced categories: ${jsonArray.length()} items")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing categories to wear", e)
            }
        }
    }

    fun syncTrackingState(
        context: Context,
        isTracking: Boolean,
        startElapsed: Long,
        targetSec: Int,
        note: String,
        categories: Set<String>,
        categoryDisplays: String,
        photoUri: String?
    ) {
        scope.launch {
            try {
                val request = PutDataMapRequest.create("/tracking_state").apply {
                    dataMap.putBoolean("is_tracking", isTracking)
                    dataMap.putLong("start_elapsed", startElapsed)
                    dataMap.putInt("target_sec", targetSec)
                    dataMap.putString("note", note)
                    dataMap.putString("categories", categories.joinToString(","))
                    dataMap.putString("category_displays", categoryDisplays)
                    dataMap.putString("photo_uri", photoUri ?: "")
                    dataMap.putLong("timestamp", System.currentTimeMillis()) // Force update
                }

                Wearable.getDataClient(context).putDataItem(request.asPutDataRequest())
                Log.d(TAG, "Successfully synced tracking state: isTracking=$isTracking")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing tracking state to wear", e)
            }
        }
    }

    fun syncTodayTotalMinutes(context: Context, totalMinutes: Int) {
        scope.launch {
            try {
                val request = PutDataMapRequest.create("/today_total_minutes").apply {
                    dataMap.putInt("total_minutes", totalMinutes)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }
                Wearable.getDataClient(context).putDataItem(request.asPutDataRequest())
                Log.d(TAG, "Successfully synced today total minutes: $totalMinutes")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing today total minutes to wear", e)
            }
        }
    }
}
