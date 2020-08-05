package lite.tsync.services

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import lite.tsync.*
import kotlin.concurrent.thread


class PeriodicSync(context: Context, params: WorkerParameters) : Worker(context, params) {

  override fun doWork(): Result {
    Log.d("PeriodicSync", "PeriodicSync doWork")

      if(Settings2.authenticated && Data.inProgress == 0L) {
        thread(){
          if(Tg2.isConnected) {
            Sync.start()
          } else {
            synchronized(Tg2){
              Tg2.needUpdate.also {
                if(it == null) Tg2.needUpdate = Type.ALL
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
