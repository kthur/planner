package com.planner.tracker.wear.complication

import android.net.Uri
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit

class TodayTimeComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("120분").build(),
            contentDescription = PlainComplicationText.Builder("오늘의 Planner 누적 기록 시간").build()
        ).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null

        var todayTotalMinutes = 0
        try {
            val dataClient = Wearable.getDataClient(this)
            val uri = Uri.Builder().scheme("wear").path("/today_total_minutes").build()
            val task = dataClient.getDataItem(uri)
            
            // 2초 타임아웃으로 동기 대기하여 폰으로부터 갱신받은 값 읽기
            val dataItem = Tasks.await(task, 2, TimeUnit.SECONDS)
            if (dataItem != null) {
                val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                todayTotalMinutes = dataMap.getInt("total_minutes", 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("${todayTotalMinutes}분").build(),
            contentDescription = PlainComplicationText.Builder("오늘 기록: ${todayTotalMinutes}분").build()
        ).build()
    }
}
