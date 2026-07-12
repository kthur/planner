package com.planner.tracker.wear.tile

import android.net.Uri
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.TimelineBuilders
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.tasks.Tasks
import com.google.common.util.concurrent.Futures
import java.util.concurrent.TimeUnit

class ActivityTileService : TileService() {

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): com.google.common.util.concurrent.ListenableFuture<TileBuilders.Tile> {
        // Wear OS TileService의 onTileRequest는 ListenableFuture를 반환해야 하므로
        // Future를 생성해서 반환해 줍니다.
        val tile = buildTile()
        return com.google.common.util.concurrent.Futures.immediateFuture(tile)
    }

    private fun buildTile(): TileBuilders.Tile {
        var todayTotalMinutes = 0
        try {
            // 동기 방식으로 데이터 레이어의 값을 조회합니다.
            val dataClient = Wearable.getDataClient(applicationContext)
            val uri = Uri.Builder().scheme("wear").path("/today_total_minutes").build()
            val task = dataClient.getDataItem(uri)
            
            // 2초 내에 태스크 타임아웃 대기 후 값 획득
            val dataItem = Tasks.await(task, 2, TimeUnit.SECONDS)
            if (dataItem != null) {
                val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                todayTotalMinutes = dataMap.getInt("total_minutes", 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val textLayout = LayoutElementBuilders.Text.Builder()
            .setText("오늘 누적: ${todayTotalMinutes}분")
            .build()

        val layout = LayoutElementBuilders.Layout.Builder()
            .setRoot(textLayout)
            .build()

        val timelineEntry = TimelineBuilders.TimelineEntry.Builder()
            .setLayout(layout)
            .build()

        val timeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(timelineEntry)
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setTimeline(timeline)
            .build()
    }

    override fun onResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): com.google.common.util.concurrent.ListenableFuture<ResourceBuilders.Resources> {
        val resources = ResourceBuilders.Resources.Builder()
            .setVersion("1")
            .build()
        return com.google.common.util.concurrent.Futures.immediateFuture(resources)
    }
}
