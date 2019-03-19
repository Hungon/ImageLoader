package com.trials.imageloader

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.work.*
import com.trials.imageloader.workers.BlurWorker
import com.trials.imageloader.workers.CleanupWorker
import com.trials.imageloader.workers.SaveImageToFileWorker

class BlurViewModel : ViewModel() {

    private val workManager: WorkManager = WorkManager.getInstance()
    internal var imageUri: Uri? = null
    internal var outputUri: Uri? = null

    // New instance variable for the WorkInfo
    internal val outputWorkInfos: LiveData<List<WorkInfo>>

    //In the BlurViewModel constructor
    init {
        // This transformation makes sure that whenever the current work Id changes the WorkInfo
        // the UI is listening to changes
        outputWorkInfos = workManager.getWorkInfosByTagLiveData(TAG_OUTPUT)
    }

    private fun uriOrNull(uriString: String?): Uri? {
        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }
    }

    /**
     * Creates the input data bundle which includes the Uri to operate on
     * @return Data which contains the Image Uri as a String
     */
    private fun createInputDataForUri(): Data {
        val builder = Data.Builder()
        imageUri?.let {
            builder.putString(KEY_IMAGE_URI, imageUri.toString())
        }
        return builder.build()
    }

    internal fun applyBlur(blurLevel: Int) {
        // want to stop the current one and start blurring the new image.
        var continuation = workManager
            .beginUniqueWork(
                IMAGE_MANIPULATION_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.from(CleanupWorker::class.java)
            )
        // Add WorkRequests to blur the image the number of times requested
        for (i in 0 until blurLevel) {
            val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()
            // Input the Uri if this is the first blur operation
            // After the first blur operation the input will be the output of previous
            // blur operations.
            if (i == 0) {
                blurBuilder.setInputData(createInputDataForUri())
            }
            continuation = continuation.then(blurBuilder.build())
        }
        // Create charging constraint
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .build()

        // Add WorkRequest to save the image to the filesystem
        val save = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
            .setConstraints(constraints)
            .addTag(TAG_OUTPUT)
            .build()
        continuation = continuation.then(save)
        // Actually start the work
        continuation.enqueue()
    }

    internal fun cancelWork() {
        workManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME)
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
}
