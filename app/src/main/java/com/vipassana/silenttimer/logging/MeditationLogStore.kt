package com.vipassana.silenttimer.logging

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DailyTotal(val date: LocalDate, val totalMillis: Long)

object MeditationLogStore {
    private const val FILE_NAME = "meditation_log.json"
    private val lock = Any()

    fun addSession(
        context: Context,
        durationMillis: Long,
        sessionEndTimeMillis: Long = System.currentTimeMillis()
    ) {
        if (durationMillis <= 0) return
        val date = Instant.ofEpochMilli(sessionEndTimeMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        synchronized(lock) {
            val map = readMap(context)
            val newTotal = (map[date] ?: 0L) + durationMillis
            map[date] = newTotal
            writeMap(context, map)
        }
    }

    fun loadDailyTotals(context: Context): List<DailyTotal> {
        synchronized(lock) {
            val map = readMap(context)
            return map.entries
                .map { DailyTotal(it.key, it.value) }
                .sortedByDescending { it.date }
        }
    }

    private fun readMap(context: Context): MutableMap<LocalDate, Long> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return mutableMapOf()
        return try {
            val text = file.readText()
            if (text.isBlank()) {
                mutableMapOf()
            } else {
                val json = JSONObject(text)
                val result = mutableMapOf<LocalDate, Long>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val date = LocalDate.parse(key)
                    val value = json.optLong(key, 0L)
                    result[date] = value
                }
                result
            }
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun writeMap(context: Context, map: Map<LocalDate, Long>) {
        val json = JSONObject()
        for ((date, value) in map) {
            json.put(date.toString(), value)
        }
        val file = File(context.filesDir, FILE_NAME)
        file.writeText(json.toString())
    }
}
