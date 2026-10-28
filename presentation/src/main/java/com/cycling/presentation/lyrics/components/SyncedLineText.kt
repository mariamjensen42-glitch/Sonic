package com.cycling.presentation.lyrics.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.lyrics.model.synced.SyncedLine
import com.cycling.presentation.lyrics.LocalLyricsPalette

@Composable
fun SyncedLineText(
    line: SyncedLine,
    isLineRtl: Boolean,
    textStyle: TextStyle,
    /** 正文色。不传就取当前歌词页的调色板，不回退到主题 token。 */
    textColor: Color = LocalLyricsPalette.current.textColor,
    modifier: Modifier = Modifier,
    /** 行文本在可用宽度内居中；关掉就按语言方向靠起始侧。 */
    alignCenter: Boolean = false,
    /** 是否渲染译文行。关掉时译文仍在模型里，只是不画。 */
    showTranslation: Boolean = true,
) {
    val palette = LocalLyricsPalette.current
    val alignment = when {
        alignCenter -> Alignment.CenterHorizontally
        isLineRtl -> Alignment.End
        else -> Alignment.Start
    }
    val textAlign = when {
        alignCenter -> TextAlign.Center
        isLineRtl -> TextAlign.End
        else -> TextAlign.Start
    }
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = SonSpacing.small, horizontal = SonSpacing.medium),
        horizontalAlignment = alignment
    ) {
        Text(
            text = line.content,
            style = textStyle,
            color = textColor,
            textAlign = textAlign
        )
        line.translation?.takeIf { showTranslation }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                // 与逐字歌词同一套口径：译文 = 正文色 × 固定的「已开始」alpha，
                // 非当前行的整体压暗已经由 LyricsLineItem 的图层做过了，这里不再叠。
                color = textColor.copy(alpha = palette.translationActiveAlpha),
                textAlign = textAlign
            )
        }
    }
}
