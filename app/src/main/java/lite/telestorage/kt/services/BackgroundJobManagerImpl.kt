package lite.telestorage.kt.services

import android.content.Context
import android.provider.MediaStore
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit


class BackgroundJobManagerImpl(context: Context) : BackgroundJobManager() {

  private val WORK_TAG = "lite.telestorage.tags.work"
  private val MAX_CONTENT_TRIGGER_DELAY_MS = 1500L

  override fun scheduleContentObserverJob() {
    val constrains = Constraints
        .Builder()
        .addContentUriTrigger(MediaStore.Files.getContentUri("external"), true)
        .addContentUriTrigger(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true)
        .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
        .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
        .setTriggerContentMaxDelay(MAX_CONTENT_TRIGGER_DELAY_MS, TimeUnit.MILLISECONDS)
        .build()
    val request = OneTimeWorkRequest
      .Builder(ChangeWatcher::class.java)
      .setConstraints(constrains)
      .build()
    workManager?.enqueueUniqueWork(WORK_TAG, ExistingWorkPolicy.KEEP, request)
  }

  companion object {
    private var workManager: WorkManager? = null
  }

  init {
    workManager = WorkManager.getInstance(context)
  }
}
