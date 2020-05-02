package lite.telestorage.services

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import lite.telestorage.*
import kotlin.concurrent.thread


class PeriodicSync(context: Context, params: WorkerParameters) : Worker(context, params) {

  override fun doWork(): Result {
    Log.d("PeriodicSync", "PeriodicSync doWork")

      if(Settings.authenticated && Data.inProgress == 0L) {
        thread(){
          if(Tg.isConnected) {
            Sync.start()
          } else {
            synchronized(Tg){
              Tg.needUpdate.also {
                if(it == null) Tg.needUpdate = Type.ALL
              }
            }
          }
        }
      }
    return Result.success()
  }

  override fun onStopped() {
    Log.d("PeriodicSync", "PeriodicSync stopped")
  }

  init {
    ContextHolder.ctx(applicationContext)
  }

}
