package lite.telestorage.kt.database

//TODO
//import net.sqlcipher.database.SQLiteDatabase;
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import lite.telestorage.kt.ContextHolder
import lite.telestorage.kt.Data
import lite.telestorage.kt.database.DbSchema.FileTable.NAME
import lite.telestorage.kt.database.DbSchema.FileTable.Cols
import lite.telestorage.kt.models.FileData
import java.util.*
import kotlin.collections.ArrayList


object FileHelper {

  private var db: SQLiteDatabase? = null

  fun addFile(f: FileData) {
    val values = getContentValues(f)
    db?.insert(NAME, null, values)
  }

  fun getFiles(where: String?, args: Array<String>): List<FileData> {
    val files: MutableList<FileData> = ArrayList<FileData>()
    val cursor = queryFiles(where, args)
    cursor.use {
      it.moveToFirst()
      while(!it.isAfterLast) {
        files.add(it.file)
        it.moveToNext()
      }
    }
    return files
  }

  fun setFileList() {
    Data.dbFileList.clear()
    val cursor = queryFiles(null, arrayOf())
    cursor.use {
      it.moveToFirst()
      while(!it.isAfterLast) {
        Data.dbFileList.add(it.file)
        it.moveToNext()
      }
    }
  }

  fun getFileByMsgId(id: Long): FileData? {
    var file: FileData? = null
    val cursor = queryFiles(Cols.MESSAGE_ID + " = ? ", arrayOf(id.toString()))
    cursor.use {
      if(it.count == 1) {
        it.moveToFirst()
        file = it.file
      }
    }
    return file
  }

  fun getFileByPath(path: String): MutableList<FileData> {
    var list: MutableList<FileData> = mutableListOf()
    val cursor = queryFiles(Cols.PATH + " = ?", arrayOf(path))
    cursor.use {
      it.moveToFirst()
      while(!it.isAfterLast) {
        list.add(it.file)
        it.moveToNext()
      }
    }
    return list
  }

  val nextFile: FileData?
    get() {
      var file: FileData? = null
      val cursor = queryFiles(
        Cols.UPLOADED + " = 0 ",
        arrayOf()
      )
      cursor.use {
        if(it.count > 0) {
          it.moveToFirst()
          file = it.file
        }
      }
      return file
    }

  val nextDownloadingFile: FileData?
    get() {
      var file: FileData? = null
      val cursor = queryFiles(
        """${Cols.FILE_ID} IS NOT NULL
           AND ${Cols.FILE_ID} <> ?
           AND ${Cols.FILE_UNIQUE_ID} IS NOT NULL
           AND ${Cols.FILE_UNIQUE_ID} <> ?
           AND ${Cols.DOWNLOADED} = 0""",
        arrayOf("", "")
      )
      cursor.use {
        if(it.count > 0) {
          it.moveToFirst()
          file = it.file
        }
      }
      return file
    }

  fun updateFile(file: FileData): Boolean {
    return if(file.messageId == 0L) false
    else (db?.insertWithOnConflict(
      NAME,
      null,
      getContentValues(file),
      CONFLICT_REPLACE
    ) ?: -1L) != -1L
  }

  fun updateFileByPath(file: FileData) {
    val path: String? = file.path
    val values = getContentValues(file)
    db?.update(
      NAME, values, Cols.PATH + "=?", arrayOf(path)
    )
  }

  fun updateFileByMsgId(file: FileData) {
    val messageId: Long = file.messageId
    val values = getContentValues(file)
    db?.update(
      NAME,
      values,
      Cols.MESSAGE_ID + " = ?",
      arrayOf(messageId.toString())
    )
  }

  fun delete(file: FileData): Int {
    for(ste in Thread.currentThread().stackTrace) {
      println(ste)
    }
    return if(file.messageId == 0L) 0
    else db?.delete(
      NAME,
      Cols.MESSAGE_ID + " = ? ",
      arrayOf(file.messageId.toString())
    ) ?: 0
  }

  fun deleteByChatId(chatId: Long): Int {
    for(ste in Thread.currentThread().stackTrace) {
      println(ste)
    }
    return db?.delete(
      NAME,
      Cols.CHAT_ID + " = ? ",
      arrayOf(chatId.toString())
    ) ?: 0
  }

  fun leaveByChatId(chatId: Long): Int {
    return db?.delete(
      NAME,
      Cols.CHAT_ID + " <> 0 AND " + Cols.CHAT_ID + " <> ?",
      arrayOf(chatId.toString())
    ) ?: 0
  }

  fun deleteList(list: MutableList<FileData>) {
    db?.beginTransaction()
    try {
      for(file in list){
        db?.execSQL("DELETE FROM $NAME WHERE ${Cols.PATH} = '${file.path}';");
      }
      db?.setTransactionSuccessful()
    } finally {
      db?.endTransaction()
    }
  }

  private fun queryFiles(whereClause: String?, whereArgs: Array<String>): FileCursor {
    val cursor = db?.query(
      NAME,
      null,  // columns - с null выбираются все столбцы
      whereClause,
      whereArgs,
      null,  // groupBy
      null,  // having
      null // orderBy
    )
    return FileCursor(cursor)
  }

    private fun getContentValues(file: FileData): ContentValues {
      val values = ContentValues()
      values.put(Cols.MESSAGE_ID, file.messageId)
      values.put(Cols.PATH, file.path)
      values.put(Cols.FILE_ID, file.fileId)
      values.put(Cols.FILE_UNIQUE_ID, file.fileUniqueId)
      values.put(Cols.CHAT_ID, file.chatId)
      values.put(Cols.NAME, file.name)
      values.put(Cols.MIME_TYPE, file.mimeType)
      values.put(Cols.UPLOADED, if(file.uploaded) 1 else 0)
      values.put(Cols.DOWNLOADED, if(file.downloaded) 1 else 0)
      values.put(Cols.LAST_MODIFIED, file.lastModified)
      values.put(Cols.DATE, file.date)
      values.put(Cols.EDIT_DATE, file.editDate)
      values.put(Cols.SIZE, file.size)
      return values
    }

  init {
    db = ContextHolder.context?.let { BaseHelper(it).writableDatabase }
//    if(ContextHolder.get() != null) {
      //TODO
      //mDatabase = new BaseHelper(CurrentContext.get().getContext()).getWritableDatabase(BuildConfig.DB_SECRET);
//      db = BaseHelper(ContextHolder.get()).writableDatabase
//    }
  }

}
