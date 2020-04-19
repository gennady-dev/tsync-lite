package lite.telestorage.kt.services

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import lite.telestorage.kt.*


class ChangeWatcher(context: Context, params: WorkerParameters) : Worker(context, params) {

  override fun doWork(): Result {
    Log.d("ChangeWatcher", "Worker doWork")

    Thread(Runnable {
      try {
        Thread.sleep(2000)
      } catch(e: InterruptedException) {
        e.printStackTrace()
      }
      if(Settings.authenticated) {
        if(Tg.isConnected) {
          Sync.start(Type.LOCAL)
        } else {
          Tg.needUpdate = true
        }
      }
      scheduleSelf()
    }).start()
    return Result.success()
  }

  override fun onStopped() {
    Log.d("ChangeWatcher", "Worker onStopped")
  }

  private fun scheduleSelf() {
    BackgroundJobManagerImpl(applicationContext).scheduleContentObserverJob()
  }

  init {
    ContextHolder.ctx(applicationContext)
  }
}
