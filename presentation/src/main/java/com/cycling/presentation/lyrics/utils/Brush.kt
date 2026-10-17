package com.cycling.presentation.lyrics.utils

import androidx.compose.ui.graphics.Paint

/**
 * 离屏层复用的默认 Paint。
 *
 * 逐字动画每帧都要 saveLayer，若每次新建 Paint 会在绘制路径上产生大量小对象分配。
 */
val LayerPaint = Paint()
