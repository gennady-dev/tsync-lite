package lite.telestorage.kt.services


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import lite.telestorage.kt.ContextHolder
import lite.telestorage.kt.Tg
import kotlin.concurrent.thread


class StartService : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    //context.startService(new Intent(context, TgService.class));

//    ContextHolder.ctx(context)
//    Tg

    thread(
      start = true,
      isDaemon = true,
      block = {
        ContextHolder.ctx(context)
        Tg
        BackgroundJobManagerImpl(context).scheduleContentObserverJob()
      }
    )
    // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    //  context.startForegroundService(new Intent(context, TgService.class));
    // } else {
    //  context.startService(new Intent(context, TgService.class));
    // }
  }

}
