package com.cycling.sonic

import timber.log.Timber

object LogUtils {
    
    private const val DEFAULT_TAG = "Sonic"
    
    fun d(message: String, tag: String = DEFAULT_TAG) {
        Timber.tag(tag).d(message)
    }
    
    fun d(message: String, vararg args: Any, tag: String = DEFAULT_TAG) {
        Timber.tag(tag).d(message, *args)
    }
    
    fun i(message: String, tag: String = DEFAULT_TAG) {
        Timber.tag(tag).i(message)
    }
    
    fun i(message: String, vararg args: Any, tag: String = DEFAULT_TAG) {
        Timber.tag(tag).i(message, *args)
    }
    
    fun w(message: String, tag: String = DEFAULT_TAG) {
        Timber.tag(tag).w(message)
    }
    
    fun w(message: String, vararg args: Any, tag: String = DEFAULT_TAG) {
        Timber.tag(tag).w(message, *args)
    }
    
    fun w(throwable: Throwable, message: String, tag: String = DEFAULT_TAG) {
        Timber.tag(tag).w(throwable, message)
    }
    
    fun e(message: String, tag: String = DEFAULT_TAG) {
        Timber.tag(tag).e(message)
    }
    
    fun e(message: String, vararg args: Any, tag: String = DEFAULT_TAG) {
        Timber.tag(tag).e(message, *args)
    }
    
    fun e(throwable: Throwable, message: String, tag: String = DEFAULT_TAG) {
        Timber.tag(tag).e(throwable, message)
    }
    
    fun e(throwable: Throwable, tag: String = DEFAULT_TAG) {
        Timber.tag(tag).e(throwable)
    }
    
    fun v(message: String, tag: String = DEFAULT_TAG) {
        Timber.tag(tag).v(message)
    }
    
    fun v(message: String, vararg args: Any, tag: String = DEFAULT_TAG) {
        Timber.tag(tag).v(message, *args)
    }
    
    fun wtf(message: String, tag: String = DEFAULT_TAG) {
        Timber.tag(tag).wtf(message)
    }
    
    fun wtf(throwable: Throwable, message: String, tag: String = DEFAULT_TAG) {
        Timber.tag(tag).wtf(throwable, message)
    }
    
    fun json(json: String, tag: String = DEFAULT_TAG) {
        Timber.tag(tag).d("JSON: $json")
    }
    
    fun method(tag: String = DEFAULT_TAG) {
        Timber.tag(tag).d(getCallerInfo())
    }
    
    private fun getCallerInfo(): String {
        val stackTrace = Thread.currentThread().stackTrace
        val caller = stackTrace.find { 
            it.className != Thread::class.java.name && 
            it.className != LogUtils::class.java.name &&
            !it.className.contains("LogUtilsKt")
        }
        return caller?.let { "${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" } ?: "Unknown"
    }
}

fun logD(message: String, tag: String = "Sonic") = LogUtils.d(message, tag)

fun logI(message: String, tag: String = "Sonic") = LogUtils.i(message, tag)

fun logW(message: String, tag: String = "Sonic") = LogUtils.w(message, tag)

fun logE(message: String, tag: String = "Sonic") = LogUtils.e(message, tag)

fun logE(throwable: Throwable, message: String, tag: String = "Sonic") = LogUtils.e(throwable, message, tag)

fun logV(message: String, tag: String = "Sonic") = LogUtils.v(message, tag)
