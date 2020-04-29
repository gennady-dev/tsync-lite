package lite.telestorage

import android.content.Context

object ContextHolder {

  var context: Context? = null
  var activity: MainActivity? = null

  fun ctx(ctx: Context) {
    context = ctx
  }

  fun ctx(mainActivity: MainActivity) {
    activity = mainActivity
    context = mainActivity.applicationContext
  }

}
