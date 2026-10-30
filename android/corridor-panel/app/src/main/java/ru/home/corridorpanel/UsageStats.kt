package ru.home.corridorpanel

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.home.corridorpanel.model.TopicLabels
import org.json.JSONObject

data class UsageEntry(
    val topic: String,
    val title: String,
    val count: Int,
    val lastUsedMs: Long?,
)

/**
 * Счётчик команд MQTT — для будущего главного экрана по частоте.
 * Слайдеры: не чаще 1 раза / 1.5 с на топик.
 */
class UsageStats(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _counts = MutableStateFlow(loadCounts())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

    private val lastUsed = mutableMapOf<String, Long>().also { it.putAll(loadLast()) }
    private val lastCountedAt = mutableMapOf<String, Long>()

    fun record(topic: String) {
        val key = topic.trim()
        if (key.isEmpty()) return
        val now = System.currentTimeMillis()
        synchronized(this) {
            lastUsed[key] = now
            val prev = lastCountedAt[key]
            if (prev != null && now - prev < DEBOUNCE_MS) {
                persistLast()
                return
            }
            lastCountedAt[key] = now
            _counts.update { cur -> cur + (key to (cur[key] ?: 0) + 1) }
            persist()
        }
    }

    fun clear() {
        synchronized(this) {
            _counts.value = emptyMap()
            lastUsed.clear()
            lastCountedAt.clear()
            prefs.edit()
                .remove(KEY_COUNTS)
                .remove(KEY_LAST)
                .apply()
        }
    }

    fun totalActions(): Int = _counts.value.values.sum()

    fun top(limit: Int = 20): List<UsageEntry> {
        val snap = _counts.value
        val last = synchronized(this) { lastUsed.toMap() }
        return snap.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenByDescending { last[it.key] ?: 0L },
            )
            .take(limit)
            .map { (topic, count) ->
                UsageEntry(
                    topic = topic,
                    title = TopicLabels.displayTitle(topic),
                    count = count,
                    lastUsedMs = last[topic],
                )
            }
    }

    private fun loadCounts(): Map<String, Int> {
        val raw = prefs.getString(KEY_COUNTS, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                for (k in json.keys()) put(k, json.optInt(k, 0))
            }
        }.getOrDefault(emptyMap())
    }

    private fun loadLast(): Map<String, Long> {
        val raw = prefs.getString(KEY_LAST, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                for (k in json.keys()) put(k, json.optLong(k, 0L))
            }
        }.getOrDefault(emptyMap())
    }

    private fun persist() {
        val json = JSONObject()
        _counts.value.forEach { (k, v) -> json.put(k, v) }
        prefs.edit().putString(KEY_COUNTS, json.toString()).apply()
        persistLast()
    }

    private fun persistLast() {
        val json = JSONObject()
        lastUsed.forEach { (k, v) -> json.put(k, v) }
        prefs.edit().putString(KEY_LAST, json.toString()).apply()
    }

    companion object {
        private const val PREFS = "usage_stats"
        private const val KEY_COUNTS = "counts_json"
        private const val KEY_LAST = "last_json"
        private const val DEBOUNCE_MS = 1_500L
    }
}
