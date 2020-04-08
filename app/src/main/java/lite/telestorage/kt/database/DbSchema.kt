package lite.telestorage.kt.database

class DbSchema {

  object SettingsTable {
    const val NAME = "settings"

    object Cols {
      const val UUID = "uuid"
      const val AUTHENTICATED = "authenticated"
      const val CHAT_ID = "chat_id"
      const val SUPERGROUP_ID = "supergroup_id"
      const val ENABLED = "enabled"
      const val TITLE = "title"
      const val IS_CHANNEL = "is_channel"
      const val PATH = "path"
      const val AS_MEDIA = "as_media"
      const val DELETE_UPLOADED = "delete_uploaded"
      const val DOWNLOAD_MISSING = "download_missing"
      const val MESSAGES_DOWNLOADED = "messages_downloaded"
      const val LAST_UPDATE = "last_update"
    }
  }

  object FileTable {
    const val NAME = "files"

    object Cols {
      const val UUID = "uuid"
      const val FILE_ID = "file_id"
      const val FILE_UNIQUE_ID = "file_unique_id"
      const val CHAT_ID = "chat_id"
      const val MESSAGE_ID = "message_id"
      const val NAME = "name"
      const val MIME_TYPE = "mime_type"
      const val PATH = "path"
      const val FILE_URI = "uri"
      const val UPLOADED = "uploaded"
      const val DOWNLOADED = "downloaded"
      const val IN_PROGRESS = "in_progress"
      const val LAST_MODIFIED = "last_modified"
    }
  }
}