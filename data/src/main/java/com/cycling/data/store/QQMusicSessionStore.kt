package com.cycling.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cycling.data.api.qqmusic.QQMusicSession
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.qqMusicSessionDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "qq_music_account")

/**
 * QQ 音乐登录态的本机存储。
 *
 * 只落 DataStore，不做任何加密：Cookie 本身就是一张有有效期的票据，能被读到的人已经拿到了
 * 这个 App 的数据目录；真要防的越权读取得靠系统沙箱，而不是在这里做一层假加密。
 *
 * 五列分开存而不是把 Cookie 整串当唯一真相：账号标识与昵称要给界面直接读，
 * 每次都从 Cookie 里重新解析会让「未登录」的判定散落在各处。
 */
@Singleton
class QQMusicSessionStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val session: Flow<QQMusicSession?> = context.qqMusicSessionDataStore.data.map { preferences ->
        val cookie = preferences[cookieKey].orEmpty()
        val uin = preferences[uinKey].orEmpty()
        val musicKey = preferences[musicKeyKey].orEmpty()
        if (cookie.isBlank() || uin.isBlank() || musicKey.isBlank()) {
            null
        } else {
            QQMusicSession(
                cookie = cookie,
                uin = uin,
                musicKey = musicKey,
                nickname = preferences[nicknameKey].orEmpty(),
                avatarUrl = preferences[avatarKey]?.takeIf(String::isNotBlank)
            )
        }
    }

    suspend fun save(session: QQMusicSession) {
        require(session.isLoggedIn) { "QQ 音乐登录态不完整" }
        context.qqMusicSessionDataStore.edit { preferences ->
            preferences[cookieKey] = session.cookie
            preferences[uinKey] = session.uin
            preferences[musicKeyKey] = session.musicKey
            preferences[nicknameKey] = session.nickname
            val avatar = session.avatarUrl
            if (avatar.isNullOrBlank()) {
                preferences.remove(avatarKey)
            } else {
                preferences[avatarKey] = avatar
            }
        }
    }

    suspend fun clear() {
        context.qqMusicSessionDataStore.edit { preferences -> preferences.clear() }
    }

    private companion object {
        val cookieKey = stringPreferencesKey("cookie")
        val uinKey = stringPreferencesKey("uin")
        val musicKeyKey = stringPreferencesKey("music_key")
        val nicknameKey = stringPreferencesKey("nickname")
        val avatarKey = stringPreferencesKey("avatar_url")
    }
}
