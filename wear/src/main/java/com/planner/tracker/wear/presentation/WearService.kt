package com.planner.tracker.wear.presentation

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearService : WearableListenerService() {
    companion object {
        private const val TAG = "WearService"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d(TAG, "WearService onDataChanged triggered. Data sync completed.")
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "WearService onMessageReceived: path=${messageEvent.path}")
    }
}
