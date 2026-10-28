package com.cycling.domain.online.usecase

import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.repository.OnlinePluginRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * 用音源脚本补一张在线曲目的封面（LX `action = 'pic'`）。
 *
 * 封面是锦上添花：任何失败都返回 null，绝不让「没有封面」升级成「放不出来」。
 * 脚本这一层本来也常常没有 pic 能力，调用方应当只把它当作兜底。
 */
@Singleton
class ResolveOnlineCoverUseCase @Inject constructor(
    private val pluginRepository: OnlinePluginRepository
) {

    suspend operator fun invoke(track: MusicInfo): String? = try {
        pluginRepository.fetchCover(track)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
