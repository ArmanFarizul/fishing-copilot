package com.fishingcopilot.catchlog

import android.content.Context
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Keeps catch photos inside the app, so they survive the picker's temporary read permission. */
interface PhotoStore {
    /** Copies the picked image and returns a URI string for the app's own copy. */
    suspend fun import(source: String): String

    suspend fun delete(uri: String)
}

class AppPhotoStore(context: Context) : PhotoStore {
    private val appContext = context.applicationContext
    private val directory get() = File(appContext.filesDir, "catch_photos").apply { mkdirs() }

    override suspend fun import(source: String): String = withContext(Dispatchers.IO) {
        val target = File(directory, "${UUID.randomUUID()}.jpg")
        appContext.contentResolver.openInputStream(source.toUri()).use { input ->
            requireNotNull(input) { "Cannot open picked photo" }
            target.outputStream().use { input.copyTo(it) }
        }
        target.toUri().toString()
    }

    override suspend fun delete(uri: String) {
        withContext(Dispatchers.IO) {
            uri.toUri().path?.let(::File)?.takeIf { it.parentFile == directory }?.delete()
        }
    }
}
