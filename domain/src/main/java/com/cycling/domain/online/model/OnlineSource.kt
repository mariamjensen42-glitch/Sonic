package com.cycling.domain.online.model

/**
 * 一个可以被音源插件取链的音乐平台。
 *
 * [lxKey] 是 LX 音源脚本协议里用的平台键，同时也是宿主与脚本之间 `source` 字段的取值：
 * `wy` 网易云、`tx` QQ 音乐、`kg` 酷狗、`kw` 酷我、`mg` 咪咕、`sd` 汽水音乐。
 *
 * 苹果音乐 / Spotify / 百度网盘不在这里：它们走「歌单导入解析」而不是取链，
 * 没有可播放的音源键。
 */
enum class OnlineSource(
    val lxKey: String,
    val displayName: String
) {
    NETEASE("wy", "网易云音乐"),
    QQ("tx", "QQ 音乐"),
    KUGOU("kg", "酷狗音乐"),
    KUWO("kw", "酷我音乐"),
    MIGU("mg", "咪咕音乐"),
    SODA("sd", "汽水音乐");

    companion object {
        /** 固定展示顺序（也是默认的候选平台顺序）。 */
        val ORDERED: List<OnlineSource> = entries

        fun fromLxKey(key: String): OnlineSource? = entries.firstOrNull { it.lxKey == key }
    }
}
