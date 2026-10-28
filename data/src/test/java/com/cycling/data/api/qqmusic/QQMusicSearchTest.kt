package com.cycling.data.api.qqmusic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * QQ 音乐搜索响应的解析。
 *
 * 端点是 `musicu.fcg` 的 `music.search.SearchCgiService.DoSearchForQQMusicDesktop`；老的
 * `client_search_cp` 已经下线（实测 HTTP 500），这里是本项目唯一能把关键词变成 `songmid` 的入口。
 *
 * 响应结构是 `req_0.data.body.song.list[]`，字段取自真实响应（2026-10-28 实测）。
 */
class QQMusicSearchTest {

    private val response = """
        {"code":0,"ts":1793187260453,"req_0":{"code":0,"data":{"body":{"song":{"list":[
          {"id":97773,"mid":"0039MnYb0qxYhV","name":"晴天","title":"晴天","interval":269,
           "singer":[{"name":"周杰伦"}],"album":{"id":8220,"mid":"000MkMni19ClKG","name":"叶惠美"},
           "time_public":"1270000000"},
          {"id":244115385,"mid":"004Fs2FP1EvZYc","name":"晴天","title":"晴天 (Live)","interval":249,
           "singer":[{"name":"周杰伦"}],"album":{"mid":"004fXSyj3bWTMN","name":"世界巡回演唱会"}}
        ]}}}}}
    """.trimIndent()

    @Test
    fun `reads the songmid, the numeric id and the album mid`() {
        val songs = parseQqMusicSearch(response)

        assertEquals(2, songs.size)
        assertEquals("0039MnYb0qxYhV", songs[0].songMid)
        assertEquals(97773L, songs[0].songId)
        assertEquals("000MkMni19ClKG", songs[0].albumMid)
        assertEquals("叶惠美", songs[0].album)
        assertEquals("周杰伦", songs[0].artist)
        assertEquals(269_000L, songs[0].durationMs)
    }

    @Test
    fun `the title keeps the version suffix while the plain name does not`() {
        // 平台同时给 `name`（晴天）与 `title`（晴天 (Live)）。选版要靠 `title`，
        // 只看 `name` 分不出现场版与录音室版。
        val songs = parseQqMusicSearch(response)

        assertEquals("晴天 (Live)", songs[1].title)
        assertEquals(249_000L, songs[1].durationMs)
    }

    @Test
    fun `a blocked search is reported as the wind-control code`() {
        // 2001 不是「没有这首歌」，而是「这个词现在不给搜」—— 换一种写法往往就能搜到。
        val blocked = """{"code":0,"req_0":{"code":2001,"data":{"body":{"song":{"list":[]}}}}}"""

        assertEquals(2001, qqSearchCode(blocked))
        assertTrue(parseQqMusicSearch(blocked).isEmpty())
    }

    @Test
    fun `garbage yields nothing instead of throwing`() {
        assertTrue(parseQqMusicSearch("not json").isEmpty())
        assertTrue(parseQqMusicSearch("").isEmpty())
        assertTrue(parseQqMusicSearch("""{"req_0":{"code":0}}""").isEmpty())
    }
}
