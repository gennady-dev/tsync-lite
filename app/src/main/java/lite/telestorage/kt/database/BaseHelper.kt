package lite.telestorage.kt.database

//TODO
//import net.sqlcipher.database.SQLiteDatabase;
//import net.sqlcipher.database.SQLiteOpenHelper;
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import lite.telestorage.kt.BuildConfig
import lite.telestorage.kt.database.DbSchema.FileTable
import lite.telestorage.kt.database.DbSchema.SettingsTable


class BaseHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) {

  override fun onCreate(db: SQLiteDatabase) {
    db.execSQL(
      """CREATE TABLE ${SettingsTable.NAME}(
          _id INTEGER PRIMARY KEY AUTOINCREMENT,
          ${SettingsTable.Cols.UUID} TEXT,
          ${SettingsTable.Cols.AUTHENTICATED} INTEGER,
          ${SettingsTable.Cols.CHAT_ID} INTEGER,
          ${SettingsTable.Cols.SUPERGROUP_ID} INTEGER,
          ${SettingsTable.Cols.ENABLED} INTEGER,
          ${SettingsTable.Cols.TITLE} TEXT,
          ${SettingsTable.Cols.IS_CHANNEL} INTEGER,
          ${SettingsTable.Cols.PATH} TEXT,
          ${SettingsTable.Cols.AS_MEDIA} INTEGER,
          ${SettingsTable.Cols.DELETE_UPLOADED} INTEGER,
          ${SettingsTable.Cols.DOWNLOAD_MISSING} INTEGER,
          ${SettingsTable.Cols.MESSAGES_DOWNLOADED} INTEGER,
          ${SettingsTable.Cols.LAST_UPDATE} INTEGER
        )"""
    )

    db.execSQL(
      """CREATE TABLE ${FileTable.NAME}(
          _id INTEGER PRIMARY KEY AUTOINCREMENT,
          ${FileTable.Cols.UUID} TEXT,
          ${FileTable.Cols.FILE_ID} INTEGER,
          ${FileTable.Cols.FILE_UNIQUE_ID} TEXT,
          ${FileTable.Cols.CHAT_ID} INTEGER,
          ${FileTable.Cols.MESSAGE_ID} INTEGER,
          ${FileTable.Cols.NAME} TEXT,
          ${FileTable.Cols.MIME_TYPE} TEXT,
          ${FileTable.Cols.PATH} TEXT,
          ${FileTable.Cols.UPLOADED} INTEGER,
          ${FileTable.Cols.DOWNLOADED} INTEGER,
          ${FileTable.Cols.LAST_MODIFIED} INTEGER,
          ${FileTable.Cols.DATE} INTEGER,
          ${FileTable.Cols.EDIT_DATE} INTEGER,
          ${FileTable.Cols.SIZE} INTEGER
        )"""
    )

    db.execSQL("CREATE INDEX IF NOT EXISTS index_${FileTable.Cols.UUID} ON ${FileTable.NAME}(${FileTable.Cols.UUID})")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_${FileTable.Cols.FILE_ID} ON ${FileTable.NAME}(${FileTable.Cols.FILE_ID})")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_${FileTable.Cols.FILE_UNIQUE_ID} ON ${FileTable.NAME}(${FileTable.Cols.FILE_UNIQUE_ID})")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_${FileTable.Cols.CHAT_ID} ON ${FileTable.NAME}(${FileTable.Cols.CHAT_ID})")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_${FileTable.Cols.MESSAGE_ID} ON ${FileTable.NAME}(${FileTable.Cols.MESSAGE_ID})")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_${FileTable.Cols.PATH} ON ${FileTable.NAME}(${FileTable.Cols.PATH})")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_${FileTable.Cols.UPLOADED} ON ${FileTable.NAME}(${FileTable.Cols.UPLOADED})")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_${FileTable.Cols.DOWNLOADED} ON ${FileTable.NAME}(${FileTable.Cols.DOWNLOADED})")

  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    if(oldVersion == 1 && newVersion == 2) {
      db.beginTransaction()
      try {
        db.execSQL("ALTER TABLE " + SettingsTable.NAME + " ADD COLUMN " + SettingsTable.Cols.AUTHENTICATED + " INTEGER;")
        db.setTransactionSuccessful()
      } finally {
        db.endTransaction()
      }
    }
  }

  companion object {
    private const val VERSION = 2
    private const val DATABASE_NAME: String = BuildConfig.DB_NAME
  }
}
