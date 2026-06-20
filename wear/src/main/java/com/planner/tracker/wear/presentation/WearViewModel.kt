package com.planner.tracker.wear.presentation

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class WearCategory(
    val name: String,
    val displayName: String,
    val colorHex: String,
    val entryType: String
)

class WearViewModel(application: Application) : AndroidViewModel(application), DataClient.OnDataChangedListener {
    companion object {
        private const val TAG = "WearViewModel"
    }

    private val _categories = MutableStateFlow<List<WearCategory>>(emptyList())
    val categories: StateFlow<List<WearCategory>> = _categories

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking

    private val _startElapsed = MutableStateFlow(0L)
    val startElapsed: StateFlow<Long> = _startElapsed

    private val _targetSec = MutableStateFlow(0)
    val targetSec: StateFlow<Int> = _targetSec

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note

    private val _activeCategories = MutableStateFlow<Set<String>>(emptySet())
    val activeCategories: StateFlow<Set<String>> = _activeCategories

    private val _categoryDisplays = MutableStateFlow("")
    val categoryDisplays: StateFlow<String> = _categoryDisplays

    init {
        // Register listener
        Wearable.getDataClient(application).addListener(this)

        // Load initial values
        loadInitialState()
    }

    private fun loadInitialState() {
        val dataClient = Wearable.getDataClient(getApplication<Application>())

        // Fetch current tracking state
        val stateUri = Uri.Builder().scheme("wear").path("/tracking_state").build()
        dataClient.getDataItem(stateUri).addOnSuccessListener { dataItem ->
            if (dataItem != null) {
                val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                _isTracking.value = dataMap.getBoolean("is_tracking")
                _startElapsed.value = dataMap.getLong("start_elapsed")
                _targetSec.value = dataMap.getInt("target_sec")
                _note.value = dataMap.getString("note", "")
                val catStr = dataMap.getString("categories", "")
                _activeCategories.value = if (catStr.isEmpty()) emptySet() else catStr.split(",").toSet()
                _categoryDisplays.value = dataMap.getString("category_displays", "")
                Log.d(TAG, "Initial tracking state loaded: isTracking=${_isTracking.value}")
            }
        }.addOnFailureListener {
            Log.e(TAG, "Failed to load initial tracking state", it)
        }

        // Fetch categories
        val catUri = Uri.Builder().scheme("wear").path("/categories").build()
        dataClient.getDataItem(catUri).addOnSuccessListener { dataItem ->
            if (dataItem != null) {
                val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                val json = dataMap.getString("json", "[]")
                _categories.value = parseCategories(json)
                Log.d(TAG, "Initial categories loaded: ${_categories.value.size} items")
            }
        }.addOnFailureListener {
            Log.e(TAG, "Failed to load initial categories", it)
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val item = event.dataItem
                val path = item.uri.path
                Log.d(TAG, "onDataChanged path: $path")
                if (path == "/tracking_state") {
                    val dataMap = DataMapItem.fromDataItem(item).dataMap
                    _isTracking.value = dataMap.getBoolean("is_tracking")
                    _startElapsed.value = dataMap.getLong("start_elapsed")
                    _targetSec.value = dataMap.getInt("target_sec")
                    _note.value = dataMap.getString("note", "")
                    val catStr = dataMap.getString("categories", "")
                    _activeCategories.value = if (catStr.isEmpty()) emptySet() else catStr.split(",").toSet()
                    _categoryDisplays.value = dataMap.getString("category_displays", "")
                } else if (path == "/categories") {
                    val dataMap = DataMapItem.fromDataItem(item).dataMap
                    val json = dataMap.getString("json", "[]")
                    _categories.value = parseCategories(json)
                }
            }
        }
    }

    private fun parseCategories(jsonStr: String): List<WearCategory> {
        val list = mutableListOf<WearCategory>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    WearCategory(
                        name = obj.getString("name"),
                        displayName = obj.getString("displayName"),
                        colorHex = obj.getString("colorHex"),
                        entryType = obj.optString("entryType", "DURATION")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse categories JSON", e)
        }
        return list
    }

    fun startTracking(categories: Set<String>, categoryDisplays: String, note: String, timerMinutes: String) {
        val payload = JSONObject().apply {
            put("command", "START")
            put("categories", categories.joinToString(","))
            put("category_displays", categoryDisplays)
            put("note", note)
            put("timer_minutes", timerMinutes)
        }
        sendCommand(payload)
    }

    fun stopTracking() {
        val payload = JSONObject().apply {
            put("command", "STOP")
        }
        sendCommand(payload)
    }

    fun cancelTracking() {
        val payload = JSONObject().apply {
            put("command", "CANCEL")
        }
        sendCommand(payload)
    }

    fun quickLog(category: String, minutes: Int, note: String = "") {
        val payload = JSONObject().apply {
            put("command", "ADD_ENTRY")
            put("category", category)
            put("minutes", minutes)
            put("note", note)
        }
        sendCommand(payload)
    }

    private fun sendCommand(payload: JSONObject) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val nodes = Tasks.await(Wearable.getNodeClient(getApplication<Application>()).connectedNodes)
                if (nodes.isEmpty()) {
                    Log.w(TAG, "No connected nodes found to send command.")
                }
                for (node in nodes) {
                    Wearable.getMessageClient(getApplication<Application>())
                        .sendMessage(node.id, "/tracking/command", payload.toString().toByteArray())
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully sent command $payload to node ${node.displayName}")
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "Failed to send command to node ${node.displayName}", it)
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in sendCommand", e)
            }
        }
    }

    override fun onCleared() {
        Wearable.getDataClient(getApplication<Application>()).removeListener(this)
        super.onCleared()
    }
}
