package lite.telestorage

import android.os.Build.VERSION


object Constants {
  const val defaultGroupName = "Channel"
//  const val FOLDER_ICON = "\uD83D\uDCC2"
  const val FOLDER_ICON = "\uD83D\uDCC1"
  private const val packageName = "lite.telestorage"
  const val externalAppFilesPath = "Android/data/$packageName/files/external"
  const val tdLibPath = "/data/data/$packageName/tdlib"
  const val stickerDir = "sticker_dir"
  const val animationDir = "animation_dir"
  const val updatePreferences = "${packageName}.update_preferences"
  const val FOLDER_ICON_UPDATED = "folder_icon_updated"
  const val tags = "$packageName.tags"
  const val periodicSyncTag = "$tags.work.periodic"
  const val fileObserverSyncTag = "$tags.work.observer"
  private const val timers = "$tags.timers"
  const val progressTimer = "$timers.progress"
  val SDK = VERSION.SDK_INT

  const val settings = "$packageName.settings"
  const val authenticated = "authenticated"
  const val chatId = "chatId"
  const val supergroupId = "supergroupId"
  const val enabled = "enabled"
  const val title = "title"
  const val isChannel = "isChannel"
  const val path = "path"
  const val deleteUploaded = "deleteUploaded"
  const val downloadMissing = "downloadMissing"
  const val uploadMissing = "uploadMissing"
  const val canSend = "canSend"
  const val device = "device"

}
