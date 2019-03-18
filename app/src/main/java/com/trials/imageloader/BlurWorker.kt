package com.trials.imageloader

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.trials.imageloader.workers.blurBitmap
import com.trials.imageloader.workers.makeStatusNotification
import com.trials.imageloader.workers.writeBitmapToFile

class BlurWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    private val TAG by lazy { BlurWorker::class.java.simpleName }

    override fun doWork(): Result {
        val appContext = applicationContext

        // passed from Data object
        val resourceUri = inputData.getString(KEY_IMAGE_URI)

        makeStatusNotification("Blurring image", appContext)

        return try {
            if (TextUtils.isEmpty(resourceUri)) {
                Log.e(TAG, "Invalid input uri")
                throw IllegalArgumentException("Invalid input uri")
            }

            val resolver = appContext.contentResolver
            val picture = BitmapFactory.decodeStream(
                resolver.openInputStream(Uri.parse(resourceUri)))

            val output = blurBitmap(picture, appContext)

            // Write bitmap to a temp file
            val outputUri = writeBitmapToFile(appContext, output)

            val outputData = Data.Builder().putString(KEY_IMAGE_URI, outputUri.toString()).build()

            Result.success(outputData)
        } catch (throwable: Throwable) {
            Log.e(TAG, "Error applying blur", throwable)
            Result.failure()
        }
    }
}