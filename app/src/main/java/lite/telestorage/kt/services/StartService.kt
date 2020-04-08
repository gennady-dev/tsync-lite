package lite.telestorage.kt.services


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import lite.telestorage.kt.ContextHolder
import lite.telestorage.kt.Tg


class StartService : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    //context.startService(new Intent(context, TgService.class));

    ContextHolder.init(context)
    Tg.get()

    // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    //  context.startForegroundService(new Intent(context, TgService.class));
    // } else {
    //  context.startService(new Intent(context, TgService.class));
    // }
  }

}
