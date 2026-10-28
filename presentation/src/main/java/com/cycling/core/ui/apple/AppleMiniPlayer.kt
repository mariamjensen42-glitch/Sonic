package com.cycling.core.ui.apple

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.components.FlamingoIcon
import com.cycling.presentation.R

/**
 * 迷你播放器，照 mini_player.xml 还原。
 *
 * 几何全部来自资源：高度 48dp、左右边距 16dp、内边距 16dp、封面 30dp（圆角 5dp）、
 * 控件 32dp、容器是**胶囊**（corner_radius_pill = 80dp）加 0.5dp 描边。
 * 底色用 `apple_material_background`（日间 #F5F5F7 / 夜间 #262628）—— 原实现是毛玻璃图层，
 * 这里落成实色，不引入 RenderEffect。
 *
 * 注意：原布局里**没有进度条**，只有三段文本与两个 32dp 控件，所以这里也不放。
 */
@Composable
fun AppleMiniPlayer(
    artwork: String?,
    title: String,
    subtitle: String,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 封面节点的额外修饰：播放页打开时要用它上报位置、并把封面让给飞行中的那一张。 */
    artworkModifier: Modifier = Modifier,
    /** 胶囊本体（不含左右外边距）的额外修饰：播放页收起时外壳要缩到这条胶囊上。 */
    barModifier: Modifier = Modifier
) {
    val colors = AppleTheme.colors
    val shape = RoundedCornerShape(AppleDims.cornerPill)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppleDims.miniPlayerMargin)
            .height(AppleDims.miniPlayerHeight)
            .then(barModifier)
            .clip(shape)
            // 玻璃胶囊：iOS 迷你播放器是半透明材质，内容从下方透出。
            .background(colors.glass)
            .border(AppleDims.hairline, colors.materialStroke, shape)
            .applePress(onClick)
            .padding(start = AppleDims.padding, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        M3Artwork(
            artwork = artwork,
            shape = RoundedCornerShape(AppleDims.cornerS),
            modifier = Modifier
                .size(AppleDims.miniPlayerThumbnail)
                .then(artworkModifier)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppleType.caption1Emphasized,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = AppleType.caption1,
                color = colors.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        MiniPlayerButton(
            // Flamingo 的 mini_* 就是为这条 48dp 胶囊画的：三角与竖条的比例比大播放键粗一档，
            // 缩到 24dp 才不显细。
            icon = if (isPlaying) R.drawable.ic_flamingo_mini_pause else R.drawable.ic_flamingo_mini_play,
            contentDescription = stringResource(
                if (isPlaying) R.string.player_pause_desc else R.string.player_play_desc
            ),
            onClick = onPlayPause
        )
        MiniPlayerButton(
            icon = R.drawable.ic_flamingo_mini_fast_forward,
            contentDescription = stringResource(R.string.player_next_desc),
            onClick = onSkipNext
        )
    }
}

/** 32dp 控件，图标 24dp，与 mini_player_button_size 一致。 */
@Composable
private fun MiniPlayerButton(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(AppleDims.miniPlayerButton)
            .clip(RoundedCornerShape(AppleDims.cornerM))
            .applePress(onClick),
        contentAlignment = Alignment.Center
    ) {
        FlamingoIcon(
            res = icon,
            contentDescription = contentDescription,
            tint = AppleTheme.colors.label,
            size = 24.dp
        )
    }
}
