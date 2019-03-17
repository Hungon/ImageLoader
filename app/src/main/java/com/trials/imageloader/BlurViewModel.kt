package com.trials.imageloader

import android.arch.lifecycle.ViewModel
import android.net.Uri
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager

class BlurViewModel : ViewModel() {

    private val workManager: WorkManager = WorkManager.getInstance()
    internal var imageUri: Uri? = null
    internal var outputUri: Uri? = null

    private fun uriOrNull(uriString: String?): Uri? {
        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }
    }

    /**
     * Setters
     */
    internal fun setImageUri(uri: String?) {
        imageUri = uriOrNull(uri)
    }

    internal fun setOutputUri(outputImageUri: String?) {
        outputUri = uriOrNull(outputImageUri)
    }

    internal fun applyBlur(blurLevel: Int) {
        workManager.enqueue(OneTimeWorkRequest.from(BlurWorker::class.java))
    }
}
