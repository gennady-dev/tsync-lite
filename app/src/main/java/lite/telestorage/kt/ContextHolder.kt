package lite.telestorage.kt

import android.content.Context


class ContextHolder {

  private var context: Context
  private var activity: MainActivity? = null

  private constructor(ctx: Context) {
    context = ctx
  }

  private constructor(mainActivity: MainActivity) {
    activity = mainActivity
    context = mainActivity.applicationContext
  }

  companion object {

    private var instance: ContextHolder? = null

    fun init(ctx: Context) {
      if(instance == null) {
        instance = ContextHolder(ctx)
      }
    }

    fun init(mainActivity: MainActivity) {
      if(instance == null) {
        instance = ContextHolder(mainActivity)
      }
    }

    fun get(): Context? {
      return instance?.context
    }

    fun getActivity(): MainActivity? {
      return instance?.activity
    }

    fun destroy() {
      instance = null
    }

  }

}
