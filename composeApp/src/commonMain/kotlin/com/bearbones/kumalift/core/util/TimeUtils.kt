package com.bearbones.kumalift.core.util

import kotlinx.datetime.Clock

fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
