package lite.telestorage.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class StartupReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    BackgroundJobManagerImpl(context).scheduleContentObserverJob()
  }

}
