package com.planner.tracker.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import java.util.TimeZone

object CalendarSyncManager {
    fun getPrimaryCalendarId(context: Context): Long {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )
        val uri = CalendarContract.Calendars.CONTENT_URI
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                var fallbackId = -1L
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val isPrimary = cursor.getInt(1) != 0
                    val accountName = cursor.getString(2)
                    val accountType = cursor.getString(3)

                    if (isPrimary && accountType == "com.google") {
                        return id
                    }
                    if (fallbackId == -1L && accountType == "com.google") {
                        fallbackId = id
                    }
                }
                if (fallbackId != -1L) return fallbackId
            }
        } catch (e: SecurityException) {
            // Permission not granted
        } catch (e: Exception) {
            // Other exceptions
        }
        return 1L // Fallback default
    }

    fun addEvent(context: Context, entry: Entry, categoryDisplay: String): Long? {
        val start = if (entry.startTime > 0) entry.startTime else entry.date
        val end = if (entry.endTime > 0) entry.endTime else (start + entry.minutes * 60 * 1000L)
        val finalEnd = if (end <= start) start + 600000L else end

        val calendarId = getPrimaryCalendarId(context)
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, finalEnd)
            put(CalendarContract.Events.TITLE, categoryDisplay)
            put(CalendarContract.Events.DESCRIPTION, entry.note)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        return try {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull()
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    fun updateEvent(context: Context, eventId: Long, entry: Entry, categoryDisplay: String): Boolean {
        val start = if (entry.startTime > 0) entry.startTime else entry.date
        val end = if (entry.endTime > 0) entry.endTime else (start + entry.minutes * 60 * 1000L)
        val finalEnd = if (end <= start) start + 600000L else end

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, finalEnd)
            put(CalendarContract.Events.TITLE, categoryDisplay)
            put(CalendarContract.Events.DESCRIPTION, entry.note)
        }

        val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return try {
            val rows = context.contentResolver.update(updateUri, values, null, null)
            rows > 0
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    fun deleteEvent(context: Context, eventId: Long): Boolean {
        val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return try {
            val rows = context.contentResolver.delete(deleteUri, null, null)
            rows > 0
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}
