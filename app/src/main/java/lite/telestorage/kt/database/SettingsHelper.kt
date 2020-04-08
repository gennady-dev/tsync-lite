package lite.telestorage.kt.database

//TODO
//import net.sqlcipher.database.SQLiteDatabase;
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import lite.telestorage.kt.ContextHolder
import lite.telestorage.kt.database.DbSchema.SettingsTable
import lite.telestorage.kt.database.DbSchema.SettingsTable.Cols
import lite.telestorage.kt.models.Settings


class SettingsHelper private constructor() {

  private var database: SQLiteDatabase? = null

  fun addSettings(s: Settings) {
      val values = getContentValues(s)
      database?.insert(SettingsTable.NAME, null, values)
  }

  val settings: Settings?
    get() {
      val cursor = querySettings()
      return try {
        if(cursor == null || cursor.count == 0) {
          return null
        }
        cursor.moveToFirst()
        cursor.settings
      } finally {
        cursor?.close()
      }
    }

  fun updateSettings(s: Settings) {
      val values = getContentValues(s)
      if(settings == null) {
        database?.insert(SettingsTable.NAME, null, values)
      } else {
        val uuid: String = settings?.uuid.toString()
        database?.update(
          SettingsTable.NAME,
          values,
          Cols.UUID + " = ? ",
          arrayOf(uuid)
        )
      }
  }

  private fun querySettings(): SettingsCursor? {
    val cursor = database?.query(
      SettingsTable.NAME,
      null,  // columns - с null выбираются все столбцы
      null,
      null,
      null,  // groupBy
      null,  // having
      null // orderBy
    )
    if(cursor != null){
      return SettingsCursor(cursor)
    }
    return null
  }

  companion object {

    private var instance: SettingsHelper? = null

    fun get(): SettingsHelper? {
      if(instance == null) {
        instance = SettingsHelper()
      }
      return instance
    }

    private fun getContentValues(settings: Settings): ContentValues? {
      if(settings != null){
        val values = ContentValues()
        values.put(Cols.UUID, settings.uuid.toString())
        values.put(Cols.AUTHENTICATED, if(settings.authenticated) 1 else 0)
        values.put(Cols.CHAT_ID, settings.chatId)
        values.put(Cols.SUPERGROUP_ID, settings.supergroupId)
        values.put(Cols.ENABLED, if(settings.enabled) 1 else 0)
        values.put(Cols.TITLE, settings.title)
        values.put(Cols.IS_CHANNEL, if(settings.isChannel) 1 else 0)
        values.put(Cols.PATH, settings.path)
        values.put(Cols.AS_MEDIA, if(settings.uploadAsMedia) 1 else 0)
        values.put(Cols.DELETE_UPLOADED, if(settings.deleteUploaded) 1 else 0)
        values.put(Cols.DOWNLOAD_MISSING, if(settings.downloadMissing) 1 else 0)
        values.put(Cols.MESSAGES_DOWNLOADED, settings.messagesDownloaded)
        values.put(Cols.LAST_UPDATE, settings.lastUpdate)
        return values
      }
      return null;
    }
  }

  init {
    if(ContextHolder.get() != null) { //TODO
//mDatabase = new BaseHelper(CurrentContext.get().getContext()).getWritableDatabase(BuildConfig.DB_SECRET);
      database = BaseHelper(ContextHolder.get()).writableDatabase
    }
  }
}
