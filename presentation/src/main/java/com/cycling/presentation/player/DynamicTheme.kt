package com.cycling.presentation.player

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.cycling.core.ui.theme.ArtColorExtractor
import com.cycling.core.ui.theme.sonColorSchemeOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * 封面派生的主题。
 *
 * [seed] 是封面提取出的种子色，[colorScheme] 是由它展开的完整配色方案。
 * 两者都为空时表示不用封面换肤，主题回落到品牌色。
 */
@Immutable
data class SonArtworkTheme(
    val seed: Color? = null,
    val colorScheme: ColorScheme? = null
) {
    companion object {
        val None = SonArtworkTheme()
    }
}

/**
 * 从当前封面提取种子色并生成配色方案。
 *
 * 整个流程（Coil 解码 → Palette 取色 → HCT 色调映射）都在后台线程完成：
 * 色调映射涉及上千次颜色空间迭代，放在主线程会在切歌瞬间造成掉帧。
 */
@Composable
fun rememberArtworkTheme(imageUrl: String?, darkTheme: Boolean): SonArtworkTheme {
    val context = LocalContext.current
    var theme by remember(imageUrl, darkTheme) { mutableStateOf(SonArtworkTheme.None) }

    LaunchedEffect(imageUrl, darkTheme) {
        if (imageUrl == null) {
            theme = SonArtworkTheme.None
            return@LaunchedEffect
        }
        theme = runCatching {
            withContext(Dispatchers.Default) {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                val result = ImageLoader.Builder(context).build().execute(request) as? SuccessResult
                val seed = result?.image?.toBitmap()?.let { ArtColorExtractor.seedFromBitmap(it) }
                    ?: return@withContext SonArtworkTheme.None
                SonArtworkTheme(seed = seed, colorScheme = sonColorSchemeOf(seed, darkTheme))
            }
        }.onFailure {
            Timber.w(it, "rememberArtworkTheme: 封面取色失败")
        }.getOrDefault(SonArtworkTheme.None)
    }

    return theme
}
