package lite.tsync

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TSyncApp: Application() {
//  @Inject lateinit var tg: Tg
  val tg = Tg

}
