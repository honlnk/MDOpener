package com.honlnk.md_opener.app.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("settings")

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
