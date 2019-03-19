package com.trials.imageloader.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.trials.imageloader.KEY_IMAGE_URI
import java.io.FileNotFoundException

class BlurWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    private val TAG by lazy { BlurWorker::class.java.simpleName }

    override fun doWork(): Result {
        val appContext = applicationContext

        // Makes a notification when the work starts and slows down the work so that it's easier to
        // see each WorkRequest start, even on emulated devices
        makeStatusNotification("Blurring image", appContext)
        sleep()

        return try {
            val outputData = createBlurredBitmap(appContext, inputData.getString(KEY_IMAGE_URI))
            Result.success(outputData)
        } catch (fileNotFoundException: FileNotFoundException) {
            Log.e(TAG, "Failed to decode input stream", fileNotFoundException)
            throw RuntimeException("Failed to decode input stream", fileNotFoundException)
        } catch (throwable: Throwable) {
            // If there were errors, return FAILURE
            Log.e(TAG, "Error applying blur", throwable)
            Result.failure()
        }
    }

    @Throws(FileNotFoundException::class, IllegalArgumentException::class)
    private fun createBlurredBitmap(appContext: Context, resourceUri: String?): Data {
        if (resourceUri.isNullOrEmpty()) {
            Log.e(TAG, "Invalid input uri")
            throw IllegalArgumentException("Invalid input uri")
        }

        val resolver = appContext.contentResolver

        // Create a bitmap
        val bitmap = BitmapFactory.decodeStream(
            resolver.openInputStream(Uri.parse(resourceUri)))

        // Blur the bitmap
        val output = blurBitmap(bitmap, appContext)

        // Write bitmap to a temp file
        val outputUri = writeBitmapToFile(appContext, output)

        // Return the output for the temp file
        return workDataOf(KEY_IMAGE_URI to outputUri.toString())
    }
}