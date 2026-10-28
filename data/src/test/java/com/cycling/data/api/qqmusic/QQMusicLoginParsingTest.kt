package com.cycling.data.api.qqmusic

import com.cycling.data.api.qqmusic.dto.QqLoginCredentialDto
import java.io.IOException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 扫码登录协议里那些「必须逐字对上」的部分：回调脚本、微信轮询脚本、Cookie 拼装与 hash33。
 *
 * 这些是整条链上唯一能在无网络、无设备的情况下钉住的东西 —— 服务端换个状态码或换一处
 * 字段名，登录就会静默失败，所以用例照着真实响应写。
 */
class QQMusicLoginParsingTest {

    @Test
    fun `parses qq qr callback states`() {
        assertEquals(
            QqQrEvent.Waiting,
            parsePtuiCallback("ptuiCB('66','0','','0','二维码未失效。','')").event
        )
        assertEquals(
            QqQrEvent.Scanned,
            parsePtuiCallback("ptuiCB('67','0','','0','二维码认证中。','')").event
        )
        assertEquals(
            QqQrEvent.Expired,
            parsePtuiCallback("ptuiCB('65','0','','0','二维码已失效。','')").event
        )
        assertEquals(
            QqQrEvent.Rejected,
            parsePtuiCallback("ptuiCB('68','0','','0','你已取消授权。','')").event
        )
    }

    @Test
    fun `qq qr callback carries uin and signature when authorized`() {
        val result = parsePtuiCallback(
            "ptuiCB('0','0','https://graph.qq.com/oauth2.0/login_jump?ptsigx=sig-value&uin=12345678','0','登录成功！','')"
        )

        assertEquals(QqQrEvent.Connected, result.event)
        assertEquals("12345678", result.uin)
        assertEquals("sig-value", result.sigX)
    }

    @Test
    fun `parses wechat qr uuid and polling states`() {
        assertEquals(
            "wechat-uuid",
            parseWechatQrUuid("""<img class="qrcode" src="/connect/qrcode/wechat-uuid"/>""")
        )
        assertEquals(
            QqQrEvent.Waiting,
            parseWechatPoll("window.wx_errcode=408; window.wx_code='';").event
        )
        assertEquals(
            QqQrEvent.Scanned,
            parseWechatPoll("window.wx_errcode=404; window.wx_code='';").event
        )
        assertEquals(
            QqQrEvent.Rejected,
            parseWechatPoll("window.wx_errcode=403;").event
        )
        assertEquals(
            QqQrEvent.Expired,
            parseWechatPoll("window.wx_errcode=402;").event
        )

        val authorized = parseWechatPoll("window.wx_errcode=405; window.wx_code='wechat-code';")
        assertEquals(QqQrEvent.Connected, authorized.event)
        assertEquals("wechat-code", authorized.authorizationCode)
    }

    @Test
    fun `wechat payload matches the official exchange shape`() {
        val payload = buildWechatLoginPayload("wechat-code", "wx48db31d50e334801")

        val comm = payload["comm"]?.jsonObject
        assertEquals("qqmusic", comm?.get("tmeAppID")?.jsonPrimitive?.content)
        assertEquals("1", comm?.get("tmeLoginType")?.jsonPrimitive?.content)

        val request = payload["req"]?.jsonObject
        assertEquals("music.login.LoginServer", request?.get("module")?.jsonPrimitive?.content)
        assertEquals("Login", request?.get("method")?.jsonPrimitive?.content)
        val param = request?.get("param")?.jsonObject
        assertEquals("wx48db31d50e334801", param?.get("strAppid")?.jsonPrimitive?.content)
        assertEquals("wechat-code", param?.get("code")?.jsonPrimitive?.content)
    }

    @Test
    fun `hash33 matches the qq login protocol`() {
        assertEquals(1_968_809_247L, qqHash33("example-qrsig", 0).toLong())
        assertEquals(193_496_974L, qqHash33("key", 5381).toLong())
    }

    @Test
    fun `credential becomes a session cookie that parses back`() {
        val credential = QqLoginCredentialDto(
            musicid = JsonPrimitive(12_345_678),
            musickey = "play-key",
            openid = "open-id",
            loginType = 2
        )

        val cookie = buildCredentialCookie(credential)
        val session = parseSessionCookie(cookie)

        assertEquals("12345678", session?.uin)
        assertEquals("play-key", session?.musicKey)
        assertTrue(cookie.contains("psrf_qqopenid=open-id"))
        assertTrue(session?.isLoggedIn == true)
    }

    @Test
    fun `session without a music key is not a login`() {
        assertNull(parseSessionCookie("uin=12345678; qm_keyst="))
        assertNull(parseSessionCookie(""))
    }

    @Test
    fun `incomplete credential is rejected instead of stored`() {
        val credential = QqLoginCredentialDto(musicid = JsonPrimitive(12_345_678), musickey = "")

        assertThrows(IOException::class.java) { buildCredentialCookie(credential) }
    }
}
