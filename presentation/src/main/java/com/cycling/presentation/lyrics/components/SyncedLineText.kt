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

@Composable
fun SyncedLineText(
    line: SyncedLine,
    isLineRtl: Boolean,
    textStyle: TextStyle,
    textColor: Color,
    modifier: Modifier = Modifier,
    /** 行文本在可用宽度内居中；关掉就按语言方向靠起始侧。 */
    alignCenter: Boolean = false,
) {
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
        line.translation?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                color = textColor.copy(alpha = 0.6f),
                textAlign = textAlign
            )
        }
    }
}
