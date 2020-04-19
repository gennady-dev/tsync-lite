package lite.telestorage.kt

import android.os.Build.VERSION


object Constants {
  const val defaultGroupName = "Channel"
//  const val FOLDER_ICON = "\uD83D\uDCC2"
  const val FOLDER_ICON = "\uD83D\uDCC1"
  private const val packageName = "lite.telestorage.kt"
  const val externalAppFilesPath = "Android/data/$packageName/files/external"
  const val tdLibPath = "/data/data/$packageName/tdlib"
  const val stickerDir = "sticker_dir"
  const val updatePreferences = "${packageName}.update_preferences"
  const val FOLDER_ICON_UPDATED = "folder_icon_updated"
  const val tags = "$packageName.tags"
  const val periodicSyncTag = "$tags.work.periodic"
  const val fileObserverSyncTag = "$tags.work.observer"
  val SDK = VERSION.SDK_INT

  const val settings = "$packageName.settings"
  const val authenticated = "authenticated"
  const val chatId = "chatId"
  const val supergroupId = "supergroupId"
  const val enabled = "enabled"
  const val title = "title"
  const val isChannel = "isChannel"
  const val path = "path"
  const val uploadAsMedia = "uploadAsMedia"
  const val deleteUploaded = "deleteUploaded"
  const val downloadMissing = "downloadMissing"
  const val uploadMissing = "uploadMissing"

}
