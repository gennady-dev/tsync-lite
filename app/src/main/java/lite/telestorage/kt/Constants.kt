package lite.telestorage.kt

import android.os.Build.VERSION


object Constants {
  const val defaultGroupName = "Channel"
  private const val packageName = "lite.telestorage.kt"
  const val externalAppFilesPath = "Android/data/$packageName/files/external"
  const val tdLibPath = "/data/data/$packageName/tdlib"
  val SDK = VERSION.SDK_INT
}
