package lite.telestorage.kt.database

import java.util.UUID
import java.util.Date
import android.database.Cursor
import android.database.CursorWrapper
import lite.telestorage.kt.database.DbSchema.SettingsTable.Cols
import lite.telestorage.kt.models.Settings

class SettingsCursor(cursor: Cursor?) : CursorWrapper(cursor) {
  val settings: Settings
    get() {
      val uuid = getString(getColumnIndex(Cols.UUID))
      val authenticated = getInt(getColumnIndex(Cols.AUTHENTICATED))
      val chatId = getLong(getColumnIndex(Cols.CHAT_ID))
      val supergroupId = getInt(getColumnIndex(Cols.SUPERGROUP_ID))
      val enabled = getInt(getColumnIndex(Cols.ENABLED))
      val title = getString(getColumnIndex(Cols.TITLE))
      val isChannel = getInt(getColumnIndex(Cols.IS_CHANNEL))
      val path = getString(getColumnIndex(Cols.PATH))
      val uploadAsMedia = getInt(getColumnIndex(Cols.AS_MEDIA))
      val deleteUploaded = getInt(getColumnIndex(Cols.DELETE_UPLOADED))
      val downloadMissing = getInt(getColumnIndex(Cols.DOWNLOAD_MISSING))
      val messagesDownloaded = getLong(getColumnIndex(Cols.MESSAGES_DOWNLOADED))
      val lastUpdate = getLong(getColumnIndex(Cols.LAST_UPDATE))
      val settings = Settings()
      settings.uuid = UUID.fromString(uuid)
      settings.authenticated = authenticated == 1
      settings.chatId = chatId
      settings.supergroupId = supergroupId
      settings.enabled = enabled == 1
      settings.title = title
      settings.isChannel = isChannel == 1
      settings.path = path
      settings.uploadAsMedia = uploadAsMedia == 1
      settings.deleteUploaded = deleteUploaded == 1
      settings.downloadMissing = downloadMissing == 1
      settings.messagesDownloaded = Date(messagesDownloaded).time
      settings.lastUpdate = Date(lastUpdate).time
      return settings
    }
}