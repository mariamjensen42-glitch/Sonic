package com.cycling.domain.repository

import com.cycling.domain.model.QQMusicAccount
import com.cycling.domain.model.QQMusicLoginMethod
import com.cycling.domain.model.QQMusicQrStatus
import com.cycling.domain.model.QQMusicQrTicket
import kotlinx.coroutines.flow.Flow

/**
 * QQ 音乐账号：扫码登录、手机号验证码登录与登录态读取。
 *
 * 授权协议本身（`ptlogin2` 扫码 / `music.login.LoginServer` 手机验证码 + `musicu.fcg` 换票）
 * 留在 `:data`，这里只描述界面需要的事：现在登的是谁、怎么开一次扫码、这次扫码到哪一步了、
 * 手机号登录走到哪一步了。
 *
 * 登录态是**本机唯一**的一份：同一时间只保留一张有效二维码，重新扫码会顶掉上一张；
 * 手机号登录成功会直接覆盖扫码得到的会话。
 */
interface QQMusicAccountRepository {

    /** 已登录账号；未登录时发 `null`。登录/退出后立即发射新值。 */
    val account: Flow<QQMusicAccount?>

    /**
     * 申请一张新的登录二维码。
     *
     * @throws java.io.IOException 二维码服务不可达或没有下发图片。
     */
    suspend fun createQrTicket(method: QQMusicLoginMethod): QQMusicQrTicket

    /**
     * 查询一次扫码进度。界面按 1 秒左右轮询，直到拿到 [QQMusicQrStatus.Authorized]、
     * [QQMusicQrStatus.Expired] 或 [QQMusicQrStatus.Rejected]。
     *
     * 走到 [QQMusicQrStatus.Authorized] 时凭据已经写入会话存储，并通过账号资料接口验证过；
     * 验证不通过会抛异常，此时不会留下任何登录态。
     *
     * @param ticketId [createQrTicket] 返回的句柄。句柄失效（已过期、已被新二维码顶掉）时
     *   返回 [QQMusicQrStatus.Expired]。
     */
    suspend fun checkQrTicket(ticketId: String): QQMusicQrStatus

    /**
     * 给手机号发一条登录验证码。
     *
     * 只支持中国大陆号码：QQ 音乐的这条通道把区号写死成 `86`。
     *
     * @throws java.io.IOException 号码格式不对、触发频控、或账号被要求先做网页安全验证。
     */
    suspend fun sendPhoneCode(phone: String)

    /**
     * 用验证码换登录态。
     *
     * 与扫码一样，凭据落盘前会用账号资料接口校验一次；校验不过抛异常且不留下登录态。
     *
     * @throws java.io.IOException 验证码错误、过期，或账号被要求先做网页安全验证。
     */
    suspend fun loginWithPhone(phone: String, code: String)

    /** 清除本机登录态。 */
    suspend fun logout()
}
