package com.cycling.data.di

import javax.inject.Qualifier

/**
 * The dispatcher every playback component runs on.
 *
 * Qualified so unit tests can substitute a `StandardTestDispatcher` and drive the progress loop
 * and volume ramps on virtual time instead of waiting on the main looper.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlayerDispatcher
