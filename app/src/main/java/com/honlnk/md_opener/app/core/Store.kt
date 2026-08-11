package com.honlnk.md_opener.app.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.honlnk.md_opener.app.model.RecentItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.recentDataStore by preferencesDataStore("recent_files")
private val Context.settingsDataStore by preferencesDataStore("settings")

class RecentStore(private val context: Context) {

    private val ds = context.applicationContext.recentDataStore
    private val KEY = stringPreferencesKey("items")

    suspend fun add(uri: String, name: String) {
        ds.edit { prefs ->
            val list = parse(prefs[KEY] ?: "")
            val filtered = list.filter { it.uri != uri }
            val updated = listOf(RecentItem(uri, name, System.currentTimeMillis())) + filtered
            prefs[KEY] = toJson(updated.take(50))
        }
    }

    suspend fun remove(uri: String) {
        ds.edit { prefs ->
            prefs[KEY] = toJson(parse(prefs[KEY] ?: "").filter { it.uri != uri })
        }
    }

    suspend fun clear() {
        ds.edit { it.remove(KEY) }
    }

    fun observe(): Flow<List<RecentItem>> = ds.data.map { parse(it[KEY] ?: "") }

    private fun parse(json: String): List<RecentItem> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val out = mutableListOf<RecentItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    RecentItem(
                        uri = o.getString("uri"),
                        name = o.getString("name"),
                        lastOpened = o.optLong("t", 0L)
                    )
                )
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun toJson(items: List<RecentItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("uri", item.uri)
                    put("name", item.name)
                    put("t", item.lastOpened)
                }
            )
        }
        return arr.toString()
    }
}

class SettingsRepository(private val context: Context) {

    private val ds = context.applicationContext.settingsDataStore
    private val THEME = intPreferencesKey("theme")   // 0 跟随系统 / 1 浅色 / 2 深色
    private val FONT = intPreferencesKey("font")     // 字号 sp
    private val MAXW = intPreferencesKey("maxw")     // 正文最大宽度 dp

    val themeMode: Flow<Int> = ds.data.map { it[THEME] ?: 0 }
    val fontSizeSp: Flow<Int> = ds.data.map { it[FONT] ?: 17 }
    val maxWidthDp: Flow<Int> = ds.data.map { it[MAXW] ?: 720 }

    suspend fun setThemeMode(v: Int) = ds.edit { it[THEME] = v }
    suspend fun setFontSize(v: Int) = ds.edit { it[FONT] = v }
    suspend fun setMaxWidth(v: Int) = ds.edit { it[MAXW] = v }
}
