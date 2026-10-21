package com.cycling.presentation.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.cycling.core.ui.theme.ArtColorExtractor
import com.cycling.core.ui.theme.CoverArtworkLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * 从当前封面提取**动态色面**用的颜色。
 *
 * 只产出封面上真实存在的那两三个颜色，供播放页 / 歌词页的 `SonDynamicBackdrop` 使用
 * （经 [com.cycling.core.ui.theme.LocalSonBackdropColors] 注入）。
 *
 * 这里刻意不再生成配色方案：应用主题跟随系统壁纸（Android 12+ 动态取色），
 * 封面只负责"正在听的这张图"的色面，不该顺带把整个 App 的配色换掉 ——
 * 换歌时全应用重新着色既没意义，也让 HCT 色调映射每次切歌都白跑一遍。
 *
 * 封面位图走 [CoverArtworkLoader]（MediaStore 缩略图 → 图片流 → 内嵌封面），
 * 不用 Coil：取色只需要一张 256px 的缩略图，而 Coil 的内存缓存可能给出硬件位图，
 * `Palette` 拿它取色会直接抛异常。解码与取色都在后台线程完成。
 *
 * @param imageUrl 封面 URI（通常是 `Song.albumArt`）
 * @param filePath 音频文件路径：MediaStore 拿不到封面时用它读内嵌图
 */
@Composable
fun rememberArtworkBackdrop(
    imageUrl: String?,
    filePath: String? = null
): List<Color> {
    val context = LocalContext.current
    var backdrop by remember(imageUrl, filePath) { mutableStateOf(emptyList<Color>()) }

    LaunchedEffect(imageUrl, filePath) {
        if (imageUrl == null && filePath == null) {
            backdrop = emptyList()
            return@LaunchedEffect
        }
        backdrop = runCatching {
            withContext(Dispatchers.IO) {
                val bitmap = CoverArtworkLoader.load(context, imageUrl, filePath)
                if (bitmap == null) {
                    Timber.w("rememberArtworkBackdrop: 封面读取失败 %s", filePath ?: imageUrl)
                    return@withContext emptyList<Color>()
                }
                val colors = ArtColorExtractor.colorsFromBitmap(bitmap)
                if (colors == null) {
                    Timber.w("rememberArtworkBackdrop: Palette 没有取到颜色 %s", filePath ?: imageUrl)
                    return@withContext emptyList<Color>()
                }
                // Palette 偶尔一个 swatch 都给不出来（纯色图），这时用种子色兜底 ——
                // 它同样来自封面，比配色方案里被旋转过的 container 好。
                val result = colors.backdrop.ifEmpty { listOf(colors.seed) }
                Timber.i(
                    "rememberArtworkBackdrop: backdrop=%s",
                    result.joinToString { "#%06X".format(it.toArgb() and 0xFFFFFF) }
                )
                result
            }
        }.onFailure {
            Timber.w(it, "rememberArtworkBackdrop: 封面取色失败")
        }.getOrDefault(emptyList<Color>())
    }

    return backdrop
}
