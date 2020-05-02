package lite.telestorage.services

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import lite.telestorage.*
import java.util.*


class ChangeWatcher(context: Context, params: WorkerParameters) : Worker(context, params) {

  override fun doWork(): Result {
    Log.d("ChangeWatcher", "Worker doWork")

    Thread(Runnable {
      try {
        Thread.sleep(2000)
      } catch(e: InterruptedException) {
        e.printStackTrace()
      }
      if(
        Settings.authenticated
        && (
            Data.lastLocalSync == 0L
            || (Data.lastLocalSync != 0L && Data.lastLocalSync + Data.localSyncPeriod < Date().time)
          )
      ) {
        Data.lastLocalSync = Date().time
        var type = Type.LOCAL
        if(Data.dbFileList.isEmpty()) type = Type.ALL
        if(Tg.isConnected) {
//          Sync.start(type)
          Sync.start()
        } else {
          synchronized(Tg){
            Tg.needUpdate.also {
              if(it == null) Tg.needUpdate = type
            }
          }
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
