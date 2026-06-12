package com.planner.tracker.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import java.util.TimeZone

object CalendarSyncManager {
    fun getGoogleAccounts(context: Context): List<String> {
        val projection = arrayOf(
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )
        val uri = CalendarContract.Calendars.CONTENT_URI
        val accounts = mutableListOf<String>()
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val typeIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIdx)
                    val type = cursor.getString(typeIdx)
                    if (type == "com.google" && !accounts.contains(name)) {
                        accounts.add(name)
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
        } catch (e: Exception) {
            // Other exceptions
        }
        return accounts
    }

    fun getCalendarsForAccount(context: Context, accountName: String): List<Pair<Long, String>> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )
        val uri = CalendarContract.Calendars.CONTENT_URI
        val selection = "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?"
        val selectionArgs = arrayOf(accountName, "com.google")
        val list = mutableListOf<Pair<Long, String>>()
        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    list.add(Pair(cursor.getLong(idIdx), cursor.getString(nameIdx)))
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
        } catch (e: Exception) {
            // Other exceptions
        }
        return list
    }

    fun createCustomCalendar(context: Context, accountName: String, calendarName: String): Long? {
        val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, "com.google")
            .build()

        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, "com.google")
            put(CalendarContract.Calendars.NAME, calendarName)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, calendarName)
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF4CAF50.toInt())
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, accountName)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
            put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 1)
        }

        return try {
            val newUri = context.contentResolver.insert(uri, values)
            newUri?.lastPathSegment?.toLongOrNull()
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun isCalendarValid(context: Context, calendarId: Long): Boolean {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val uri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarId)
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) return true
            }
        } catch (e: Exception) {
            // Ignore
        }
        return false
    }

    fun getTargetCalendarId(context: Context): Long {
        val prefs = context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
        val savedId = prefs.getLong("calendar_id", -1L)
        if (savedId != -1L) {
            if (isCalendarValid(context, savedId)) {
                return savedId
            }
        }
        return getPrimaryCalendarId(context)
    }

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

        val calendarId = getTargetCalendarId(context)
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
