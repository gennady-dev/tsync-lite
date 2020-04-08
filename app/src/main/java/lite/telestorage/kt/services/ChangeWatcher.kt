package lite.telestorage.kt.services

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import lite.telestorage.kt.ContextHolder
import lite.telestorage.kt.Sync
import lite.telestorage.kt.Tg
import lite.telestorage.kt.database.SettingsHelper


class ChangeWatcher(context: Context, params: WorkerParameters) : Worker(context, params) {

  override fun doWork(): Result {
    println("ChangeWatcher")
    Thread(Runnable {
      try {
        Thread.sleep(2000)
      } catch(e: InterruptedException) {
        e.printStackTrace()
      }

      val settings = SettingsHelper.get()?.settings
      if(settings != null && settings.authenticated) {
        Tg.get()
        if(Tg.isConnected) {
          Sync.start()
        } else {
          Tg.needUpdate = true
        }
      }
      scheduleSelf()
    }).start()
    return Result.success()
  }

  override fun onStopped() {
    Log.d(null, "Worker onStopped")
  }

  private fun scheduleSelf() {
    BackgroundJobManagerImpl(applicationContext).scheduleContentObserverJob()
  }

  init {
    ContextHolder.init(context.applicationContext)
  }
}
