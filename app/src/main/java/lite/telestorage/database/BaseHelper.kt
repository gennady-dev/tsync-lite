package lite.telestorage.kt.database

//TODO
//import net.sqlcipher.database.SQLiteDatabase;
//import net.sqlcipher.database.SQLiteOpenHelper;
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import lite.telestorage.kt.BuildConfig
import lite.telestorage.kt.database.DbSchema.FileTable.NAME
import lite.telestorage.kt.database.DbSchema.FileTable.Cols


class BaseHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) {

  override fun onCreate(db: SQLiteDatabase) {

    db.execSQL(
      """CREATE TABLE ${NAME}(
          ${Cols.MESSAGE_ID} INTEGER PRIMARY KEY,
          ${Cols.PATH} TEXT,
          ${Cols.FILE_ID} INTEGER,
          ${Cols.FILE_UNIQUE_ID} TEXT,
          ${Cols.CHAT_ID} INTEGER,
          ${Cols.NAME} TEXT,
          ${Cols.MIME_TYPE} TEXT,
          ${Cols.UPLOADED} INTEGER,
          ${Cols.DOWNLOADED} INTEGER,
          ${Cols.LAST_MODIFIED} INTEGER,
          ${Cols.DATE} INTEGER,
          ${Cols.EDIT_DATE} INTEGER,
          ${Cols.SIZE} INTEGER
        )"""
    )

    db.execSQL("CREATE INDEX IF NOT EXISTS index_${Cols.CHAT_ID} ON $NAME(${Cols.CHAT_ID})")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_${Cols.PATH} ON $NAME(${Cols.PATH})")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_${Cols.UPLOADED} ON $NAME(${Cols.UPLOADED})")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_${Cols.DOWNLOADED} ON $NAME(${Cols.DOWNLOADED})")

  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    if(oldVersion == 1 && newVersion == 2) {
//      db.beginTransaction()
//      try {
//        db.execSQL("ALTER TABLE " + SettingsTable.NAME + " ADD COLUMN " + SettingsTable.Cols.AUTHENTICATED + " INTEGER;")
//        db.setTransactionSuccessful()
//      } finally {
//        db.endTransaction()
//      }
    }
  }

  companion object {
    private const val VERSION = 2
    private const val DATABASE_NAME: String = BuildConfig.DB_NAME
  }
}
