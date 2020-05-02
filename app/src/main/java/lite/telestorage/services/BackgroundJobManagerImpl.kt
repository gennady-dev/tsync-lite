package lite.telestorage.services

import android.content.Context
import android.provider.MediaStore
import androidx.work.*
import lite.telestorage.Constants
import java.util.concurrent.TimeUnit


class BackgroundJobManagerImpl(context: Context) : BackgroundJobManager() {

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
    workManager?.enqueueUniqueWork(Constants.fileObserverSyncTag, ExistingWorkPolicy.KEEP, request)
  }

  override fun schedulePeriodicJob() {
    val periodicRequest = PeriodicWorkRequest
      .Builder(PeriodicSync::class.java, 15, TimeUnit.MINUTES)
      .addTag(Constants.periodicSyncTag)
      .build()
    workManager?.enqueueUniquePeriodicWork(
      Constants.periodicSyncTag,
      ExistingPeriodicWorkPolicy.KEEP,
      periodicRequest
    )
  }

  companion object {
    private var workManager: WorkManager? = null
  }

  init {
    workManager = WorkManager.getInstance(context)
  }
}
