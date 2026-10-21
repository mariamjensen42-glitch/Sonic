package com.cycling.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一条播放记录，是全部听歌统计与排行榜的唯一数据来源。
 *
 * 为什么不像 `songs.playCount` 那样只存累计值：时长榜要"这段时间里听了多久"、
 * 时间脉络要"每天听了多少"、小时分布要"几点在听"，这些都必须按事件留痕才成立。
 * 累计值只能回答"一共听了多少遍"。
 *
 * 字段是**记录当时的快照**（标题/歌手/封面都存一份），所以歌被改名、换封面、甚至删掉
 * 之后，历史榜单仍然显示它当时的样子；`songId` 只用于"点一行去播放"时回查当前曲目。
 *
 * 字段按"一次播放"组织：曲目快照 + 实际时长 + 是否听完，便于以后做云端同步时直接映射。
 */
@Entity(
    tableName = "play_events",
    indices = [
        Index("playedAt"),
        Index("songId"),
        Index("platform")
    ]
)
data class PlayEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 音源标识。本地曲库固定为 `local`，在线音源留出扩展位。 */
    val platform: String,
    val songId: Long,
    val title: String,
    val artist: String,
    /** 歌手 id，供"常听歌手"一行点进歌手详情；未知时为 0。 */
    val artistId: Long,
    val album: String,
    /** 封面 URI：MediaStore 的专辑行或用户自选封面，取记录当时的有效值。 */
    val coverUrl: String?,
    /** 歌曲总时长，用来判断这次播放是否真的听到了结尾。 */
    val durationMs: Long,
    /** 开始播放的时刻（epoch 毫秒）。 */
    val playedAt: Long,
    /** 实际听了多久（毫秒）。暂停、拖动都会如实反映在这个值里。 */
    val playMs: Long,
    /** 是否听到了结尾。 */
    val isCompleted: Boolean,
    /** 这次播放的来源（列表、歌单、随机……），只用于排查，不参与统计。 */
    val source: String
)
