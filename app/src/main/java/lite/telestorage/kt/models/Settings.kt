package lite.telestorage.kt.models

import java.util.UUID
import java.util.Date


class Settings {

  var uuid: UUID = UUID.randomUUID()
  var authenticated = false
  var chatId: Long = 0
  var supergroupId = 0
  var enabled = false
  var title: String? = null
  var isChannel = false
  var path: String? = null
  var uploadAsMedia = false
  var deleteUploaded = false
  var downloadMissing = true
  var uploadMissing = true
  var messagesDownloaded: Long = 0
  var lastUpdate: Long = Date().time

}
