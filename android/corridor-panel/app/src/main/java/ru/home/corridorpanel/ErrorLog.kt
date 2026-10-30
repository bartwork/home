package ru.home.corridorpanel

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ErrorKind(val title: String) {
    Mqtt("MQTT"),
    Sip("SIP"),
    Camera("Камера"),
    App("Приложение"),
}

data class ErrorEntry(
    val id: String,
    val atMs: Long,
    val kind: ErrorKind,
    val message: String,
)

/** Журнал ошибок с временем и типом (persist). */
class ErrorLog(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _entries = MutableStateFlow(load())
    val entries: StateFlow<List<ErrorEntry>> = _entries.asStateFlow()

    fun record(kind: ErrorKind, message: String) {
        val msg = message.trim()
        if (msg.isEmpty()) return
        val now = System.currentTimeMillis()
        synchronized(this) {
            val cur = _entries.value
            val last = cur.firstOrNull()
            if (last != null &&
                last.kind == kind &&
                last.message == msg &&
                now - last.atMs < DEBOUNCE_MS
            ) {
                return
            }
            val next = listOf(
                ErrorEntry(
                    id = UUID.randomUUID().toString(),
                    atMs = now,
                    kind = kind,
                    message = msg,
                ),
            ) + cur
            _entries.value = next.take(MAX_ENTRIES)
            persist()
        }
    }

    fun clear() {
        synchronized(this) {
            _entries.value = emptyList()
            prefs.edit().remove(KEY_LOG).apply()
        }
    }

    fun isEmpty(): Boolean = _entries.value.isEmpty()

    private fun load(): List<ErrorEntry> {
        val raw = prefs.getString(KEY_LOG, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val kind = runCatching {
                        ErrorKind.valueOf(o.getString("kind"))
                    }.getOrNull() ?: continue
                    add(
                        ErrorEntry(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            atMs = o.getLong("at"),
                            kind = kind,
                            message = o.getString("message"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun persist() {
        val arr = JSONArray()
        _entries.value.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("at", e.atMs)
                    .put("kind", e.kind.name)
                    .put("message", e.message),
            )
        }
        prefs.edit().putString(KEY_LOG, arr.toString()).apply()
    }

    companion object {
        private const val PREFS = "error_log"
        private const val KEY_LOG = "entries_json"
        private const val MAX_ENTRIES = 100
        private const val DEBOUNCE_MS = 30_000L

        private val timeFmt = SimpleDateFormat("dd.MM HH:mm:ss", Locale("ru", "RU"))

        fun formatTime(atMs: Long): String = synchronized(timeFmt) {
            timeFmt.format(Date(atMs))
        }
    }
}
