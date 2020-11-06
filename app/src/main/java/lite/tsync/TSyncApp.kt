package lite.tsync

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TSyncApp: Application() {
//  @Inject lateinit var tg: Tg
  val tg = Tg2

}
