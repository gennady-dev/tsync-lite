package lite.telestorage.kt.models

import java.util.UUID

class FileData {

  var uuid = UUID.randomUUID()
  var id = 0
  var uniqueId: String? = null
    set(value) {
      field = if(value?.trim() == "") null else value?.trim()
    }
  var chatId: Long = 0
  var messageId: Long = 0
  var name: String? = null
    set(value) {
      field = if(value?.trim() == "") null else value?.trim()
    }
  var mimeType: String? = null
    set(value) {
      field = if(value?.trim() == "") null else value?.trim()
    }
  var path: String? = null
    set(value) {
      field = if(value?.trim() == "") null else value?.trim()
    }
  var fileUri: String? = null
    set(value) {
      field = if(value?.trim() == "") null else value?.trim()
    }
  var uploaded = false
  var downloaded = false
  var inProgress = false
  var lastModified: Long = 0

}
