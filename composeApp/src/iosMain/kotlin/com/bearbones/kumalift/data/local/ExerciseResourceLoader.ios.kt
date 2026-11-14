package com.bearbones.kumalift.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

@OptIn(ExperimentalForeignApi::class)
actual fun readExercisesJson(): String {
    val path = NSBundle.mainBundle.pathForResource("exercises", "json")
        ?: return "[]"
    return NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) as? String ?: "[]"
}
