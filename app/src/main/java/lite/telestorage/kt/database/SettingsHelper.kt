package lite.telestorage.kt.database

//TODO
//import net.sqlcipher.database.SQLiteDatabase;
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import lite.telestorage.kt.ContextHolder
import lite.telestorage.kt.database.DbSchema.SettingsTable
import lite.telestorage.kt.database.DbSchema.SettingsTable.Cols
import lite.telestorage.kt.models.Settings


object SettingsHelper {

//  private var holder: Settings? = null

  private val database: SQLiteDatabase?
    get() = ContextHolder.context?.let { BaseHelper(it).writableDatabase }

  var settings: Settings
//    get() = holder.let {
//      it ?: querySettings().use { cursor ->
//        if(cursor == null || cursor.count == 0) {
//          val newSet = Settings()
//          holder = newSet
//          add(newSet)
//          newSet
//        } else {
//          cursor.moveToFirst()
//          cursor.settings
//        }
//      }
//    }

  init {
    settings = querySettings().use {
      if(it == null || it.count == 0) {
        val newSet = Settings()
        add(newSet)
        newSet
      } else {
        it.moveToFirst()
        it.settings
      }
    }
  }

  fun add(newSet: Settings) {
    val values = getContentValues(newSet)
    database?.insert(SettingsTable.NAME, null, values)
  }

  fun update(newSet: Settings) {
    val values = getContentValues(newSet)
    database?.update(
      SettingsTable.NAME, values, Cols.UUID + " = ? ", arrayOf(newSet.uuid.toString())
    )
    settings = newSet
  }

  private fun querySettings(): SettingsCursor? {
    return database?.query(
      SettingsTable.NAME,
      null, // columns - с null выбираются все столбцы
      null,
      null,
      null, // groupBy
      null, // having
      null // orderBy
    )?.let { SettingsCursor(it) }
  }

  private fun getContentValues(settings: Settings): ContentValues {
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

//mDatabase = new BaseHelper(CurrentContext.get().getContext()).getWritableDatabase(BuildConfig.DB_SECRET);
}
