package lite.telestorage.kt.database

class DbSchema {

  object FileTable {

    const val NAME = "files"

    object Cols {
      const val MESSAGE_ID = "message_id"
      const val PATH = "path"
      const val FILE_ID = "file_id"
      const val FILE_UNIQUE_ID = "file_unique_id"
      const val CHAT_ID = "chat_id"
      const val NAME = "name"
      const val MIME_TYPE = "mime_type"
      const val UPLOADED = "uploaded"
      const val DOWNLOADED = "downloaded"
      const val LAST_MODIFIED = "last_modified"
      const val DATE = "date"
      const val EDIT_DATE = "edit_date"
      const val SIZE = "size"
    }

  }

}