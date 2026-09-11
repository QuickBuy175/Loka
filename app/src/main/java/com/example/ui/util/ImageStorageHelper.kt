package com.example.ui.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object ImageStorageHelper {
    private const val TAG = "ImageStorageHelper"
    private const val DIRECTORY_NAME = "product_images"

    /**
     * Copies an image from a selected content Uri (e.g. from PhotoPicker or Gallery)
     * into the app's private internal storage directory.
     * Returns the absolute path of the persisted image file, or null on failure.
     */
    fun saveUriToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val imagesDir = File(context.filesDir, DIRECTORY_NAME)
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val fileName = "product_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
            val destinationFile = File(imagesDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            destinationFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy image to internal storage", e)
            null
        }
    }

    /**
     * Checks if a string represents a web URL.
     */
    fun isWebUrl(pathOrUrl: String): Boolean {
        val trimmed = pathOrUrl.trim().lowercase()
        return trimmed.startsWith("http://") || trimmed.startsWith("https://")
    }

    /**
     * Checks if a string is a local file path.
     */
    fun isFilePath(path: String): Boolean {
        val trimmed = path.trim()
        return trimmed.startsWith("/") || trimmed.startsWith("file://") || trimmed.startsWith("content://")
    }
}
