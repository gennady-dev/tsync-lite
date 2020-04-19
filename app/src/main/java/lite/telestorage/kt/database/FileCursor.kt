package lite.telestorage.kt.database

import android.database.Cursor
import android.database.CursorWrapper
import lite.telestorage.kt.database.DbSchema.FileTable.Cols
import lite.telestorage.kt.models.FileData
import java.util.UUID


class FileCursor(cursor: Cursor?) : CursorWrapper(cursor) {
  val file: FileData
    get() {
      val uuid = getString(getColumnIndex(Cols.UUID))
      val id = getInt(getColumnIndex(Cols.FILE_ID))
      val uniqueId = getString(getColumnIndex(Cols.FILE_UNIQUE_ID))
      val chatId = getLong(getColumnIndex(Cols.CHAT_ID))
      val messageId = getLong(getColumnIndex(Cols.MESSAGE_ID))
      val name = getString(getColumnIndex(Cols.NAME))
      val mimeType = getString(getColumnIndex(Cols.MIME_TYPE))
      val path = getString(getColumnIndex(Cols.PATH))
      val uploaded = getInt(getColumnIndex(Cols.UPLOADED))
      val downloaded = getInt(getColumnIndex(Cols.DOWNLOADED))
      val lastModified = getLong(getColumnIndex(Cols.LAST_MODIFIED))
      val date = getLong(getColumnIndex(Cols.DATE))
      val editDate = getLong(getColumnIndex(Cols.EDIT_DATE))
      val size = getInt(getColumnIndex(Cols.SIZE))
      val file = FileData()
      file.uuid = UUID.fromString(uuid)
      file.fileId = id
      file.fileUniqueId = uniqueId
      file.chatId = chatId
      file.messageId = messageId
      file.name = name
      file.mimeType = mimeType
      file.path = path
      file.uploaded = uploaded == 1
      file.downloaded = downloaded == 1
      file.lastModified = lastModified
      file.date = date
      file.editDate = editDate
      file.size = size
      file.updated = false
      return file
    }
}
