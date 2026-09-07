package com.lakony.commandcenter.profile

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

class ProfileImageStore(private val context: Context) {
    private val file: File get() = File(context.filesDir, "profile_picture.jpg")

    fun saveFrom(uri: Uri): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not read selected image")
        true
    }.getOrDefault(false)

    fun load(): ImageBitmap? = runCatching {
        if (!file.exists()) return null
        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
    }.getOrNull()

    fun hasImage(): Boolean = file.exists()

    fun remove() {
        if (file.exists()) file.delete()
    }
}
