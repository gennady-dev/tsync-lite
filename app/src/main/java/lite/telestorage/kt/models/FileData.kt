package lite.telestorage.kt.models

import org.drinkless.td.libcore.telegram.TdApi
import java.util.UUID

class FileData {

  var uuid: UUID? = null
  var fileId = 0
    set(value) {
      if(field != value){
        field = value
        updated = true
      }
    }
  var fileUniqueId: String? = null
    set(value) {
      if(valueChanged(field, value)){
        field = newValue(value)
        updated = true
      }
    }
  var chatId: Long = 0
    set(value) {
      if(field != value){
        field = value
        updated = true
      }
    }
  var messageId: Long = 0
    set(value) {
      if(field != value){
        field = value
        updated = true
      }
    }
  var name: String? = null
    set(value) {
      if(valueChanged(field, value)){
        field = newValue(value)
        updated = true
      }
    }
  var mimeType: String? = null
    set(value) {
      if(valueChanged(field, value)){
        field = newValue(value)
        updated = true
      }
    }
  var path: String? = null
    set(value) {
      if(valueChanged(field, value)){
        field = newValue(value)
        updated = true
      }
    }
  var uploaded = false
    set(value) {
      if(field != value){
        field = value
        updated = true
      }
    }
  var downloaded = false
    set(value) {
      if(field != value){
        field = value
        updated = true
      }
    }
  var inProgress = false
    set(value) {
      if(field != value){
        field = value
        updated = true
      }
    }
  var lastModified: Long = 0
    set(value) {
      if(field != value){
        field = value
        updated = true
      }
    }
  var size: Int = 0
    set(value) {
      if(field != value){
        field = value
        updated = true
      }
    }
  var date: Long = 0
    set(value) {
      if(field != value){
        field = value
        updated = true
      }
    }
  var editDate: Long = 0
    set(value) {
      if(field != value){
        field = value
        updated = true
      }
    }
  var updated: Boolean = false

  var upload = false
  var download = false

  var lastDate: Long = 0
    get() = if(editDate == 0L) date else editDate

  override fun equals(other: Any?): Boolean {
    return (other is FileData)
      && messageId == other.messageId
      && lastDate == other.lastDate
      && size == other.size
  }

  override fun hashCode(): Int {
    return path.hashCode() + messageId.hashCode() + lastDate.hashCode() + size.hashCode()
  }

  private fun valueChanged(old: String?, new: String?): Boolean {
    return old != if(!new.isNullOrBlank()) new.trim() else null
  }

  private fun newValue(new: String?): String? {
    return if(!new.isNullOrBlank()) new.trim() else null
  }

}
