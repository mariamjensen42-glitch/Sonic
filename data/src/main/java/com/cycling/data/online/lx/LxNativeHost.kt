package com.cycling.data.online.lx

/**
 * 脚本调用宿主（Kotlin 侧）的入口对象。
 *
 * 宿主不把 `LxNativeHost` 整个对象暴露给 JS 全局，脚本侧只拿到一个普通函数
 * `lxHostInvoke(channel, payload)`（而不是 `LxNativeHost.invoke(channel, payload)`），
 * 通道名、payload JSON 结构、返回值语义都不变 ——
 * 只是少了一层 ObjectBinding，减少 native 绑定层的反射面。
 */
class LxNativeHost {

    /** (channel, payloadJson) -> resultJson。返回 null 表示该通道没有返回值。 */
    var handler: ((String, String) -> String?)? = null

    fun invoke(channel: String, payload: String): String? = handler?.invoke(channel, payload)
}
