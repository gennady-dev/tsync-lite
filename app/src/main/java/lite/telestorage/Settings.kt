package lite.telestorage

import android.content.Context

object Settings {

  var authenticated = false
  var chatId: Long = 0
  var supergroupId: Int = 0
  var enabled = false
  var title: String? = null
  var isChannel = false
  var path: String? = null
  var deleteUploaded = false
  var downloadMissing = true
  var uploadMissing = true

  init {
    ContextHolder.context?.getSharedPreferences(Constants.settings, Context.MODE_PRIVATE)
      ?.also {
        authenticated = it.getBoolean(Constants.authenticated, false)
        chatId = it.getLong(Constants.chatId, 0)
        supergroupId = it.getInt(Constants.supergroupId, 0)
        enabled = it.getBoolean(Constants.enabled, false)
        title = it.getString(Constants.title, null)
        isChannel = it.getBoolean(Constants.isChannel, false)
        path = it.getString(Constants.path, null)
        deleteUploaded = it.getBoolean(Constants.deleteUploaded, false)
        downloadMissing = it.getBoolean(Constants.downloadMissing, true)
        uploadMissing = it.getBoolean(Constants.uploadMissing, true)
      }
  }

  fun save(){
    ContextHolder.context?.getSharedPreferences(Constants.settings, Context.MODE_PRIVATE)?.edit()
      ?.also {
        it.putBoolean(Constants.authenticated, authenticated)
        it.putLong(Constants.chatId, chatId)
        it.putInt(Constants.supergroupId, supergroupId)
        it.putBoolean(Constants.enabled, enabled)
        it.putString(Constants.title, title)
        it.putBoolean(Constants.isChannel, isChannel)
        it.putString(Constants.path, path)
        it.putBoolean(Constants.deleteUploaded, deleteUploaded)
        it.putBoolean(Constants.downloadMissing, downloadMissing)
        it.putBoolean(Constants.uploadMissing, uploadMissing)
        it.apply()
      }
  }

}