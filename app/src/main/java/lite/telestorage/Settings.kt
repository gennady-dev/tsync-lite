package lite.telestorage

import android.content.Context
import java.util.*

object Settings {

  var authenticated = false
  var chatId: Long = 0
  var groupId: Int = 0
  var enabled = false
  var title: String? = null
  var isChannel = false
  var path: String? = null
  var deleteUploaded = false
  var downloadMissing = true
  var uploadMissing = true
  var canSend = false
  var device: String? = null

  init {
    ContextHolder.context?.getSharedPreferences(Constants.settings, Context.MODE_PRIVATE)
      ?.also {
        authenticated = it.getBoolean(Constants.authenticated, false)
        chatId = it.getLong(Constants.chatId, 0)
        groupId = it.getInt(Constants.supergroupId, 0)
        enabled = it.getBoolean(Constants.enabled, false)
        title = it.getString(Constants.title, null)
        isChannel = it.getBoolean(Constants.isChannel, false)
        path = it.getString(Constants.path, null)
        deleteUploaded = it.getBoolean(Constants.deleteUploaded, false)
        downloadMissing = it.getBoolean(Constants.downloadMissing, true)
        uploadMissing = it.getBoolean(Constants.uploadMissing, true)
        canSend = it.getBoolean(Constants.canSend, false)
        device = it.getString(Constants.device, UUID.randomUUID().toString())
      }
  }

  fun save(){
    ContextHolder.context?.getSharedPreferences(Constants.settings, Context.MODE_PRIVATE)?.edit()
      ?.also {
        it.putBoolean(Constants.authenticated, authenticated)
        it.putLong(Constants.chatId, chatId)
        it.putInt(Constants.supergroupId, groupId)
        it.putBoolean(Constants.enabled, enabled)
        it.putString(Constants.title, title)
        it.putBoolean(Constants.isChannel, isChannel)
        it.putString(Constants.path, path)
        it.putBoolean(Constants.deleteUploaded, deleteUploaded)
        it.putBoolean(Constants.downloadMissing, downloadMissing)
        it.putBoolean(Constants.uploadMissing, uploadMissing)
        it.putBoolean(Constants.canSend, canSend)
        it.putString(Constants.device, device)
        it.apply()
      }
  }

}