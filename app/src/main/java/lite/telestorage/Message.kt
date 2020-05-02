package lite.telestorage

import android.util.Log
import lite.telestorage.models.FileData
import org.drinkless.td.libcore.telegram.TdApi

class Message() {

  var message: TdApi.Message? = null
  var messageId: Long = 0
  var chatId: Long = 0
  var content: TdApi.MessageContent? = null
  var fileId: Int = 0
  var fileUniqueId: String? = null
  var fileName: String? = null
  var caption: String? = null
  var path: String? = null
  var mimeType: String? = null
  var localPath: String? = null
  var downloaded: Boolean = false
  var uploaded: Boolean = false
  var date: Long = 0
  var editDate: Long = 0
  var size: Int = 0
  var upload = false
  var download = false

  val fileData: FileData?
    get() = FileData().also {
        it.path = path
        it.chatId = chatId
        it.messageId = messageId
        it.fileId = fileId
        it.mimeType = mimeType
        it.fileUniqueId = fileUniqueId
        it.name = fileName
        it.downloaded = downloaded
        it.uploaded = uploaded
        it.date = date
        it.editDate = editDate
        it.size = size
        it.upload = upload
        it.download = download
      }

  internal constructor(update: TdApi.UpdateMessageSendSucceeded) : this() {
    update.message?.also {
      message = it
      messageId = it.id
      chatId = it.chatId
      date = it.date.toLong() * 1000
      editDate = it.editDate.toLong() * 1000
      content = it.content
    }
    Log.d("Message", "TdApi.UpdateMessageSendSucceeded")
    setFromUpdate()
  }

  internal constructor(update: TdApi.UpdateMessageContent?) : this() {
    update?.newContent?.also {
      content = it
      messageId = update.messageId
      chatId = update.chatId
    }
    Log.d("Message", "TdApi.UpdateMessageContent")
    setFromUpdate()
  }

  internal constructor(update: TdApi.Message?) : this() {
    update?.also {
      message = it
      messageId = it.id
      chatId = it.chatId
      content = it.content
      date = it.date.toLong() * 1000
      editDate = it.editDate.toLong() * 1000
    }
    Log.d("Message", "TdApi.Message")
    setFromUpdate()
  }

  private fun setFromUpdate() {
    content?.also {
      when(it.constructor) {
        TdApi.MessageDocument.CONSTRUCTOR -> setFromDocument(it as TdApi.MessageDocument)
        TdApi.MessageVideo.CONSTRUCTOR -> setFromVideo(it as TdApi.MessageVideo)
        TdApi.MessageAudio.CONSTRUCTOR -> setFromAudio(it as TdApi.MessageAudio)
        TdApi.MessagePhoto.CONSTRUCTOR -> setFromPhoto(it as TdApi.MessagePhoto)
        TdApi.MessageAnimation.CONSTRUCTOR -> setFromAnimation(it as TdApi.MessageAnimation)
        TdApi.MessageSticker.CONSTRUCTOR -> setFromSticker(it as TdApi.MessageSticker)
        else -> Log.d("Message", "Receive an unknown message type")
      }
    }
    Log.d("Message", "setFromUpdate")
  }

  private fun setFromDocument(content: TdApi.MessageDocument){
    caption = content.caption?.text
    path = caption?.trim()
//    path = content.caption?.text?.let { getPathFromCaption(it) }
    content.document?.document?.also { setFromTdApiFile(it) }
    content.document?.also {
      if(!it.fileName.isNullOrBlank()) fileName = it.fileName
      if(!it.mimeType.isNullOrBlank()) mimeType = it.mimeType
    }
  }

  private fun setFromVideo(content: TdApi.MessageVideo){
    caption = content.caption?.text
    path = caption?.trim()
//    path = content.caption?.text?.let { getPathFromCaption(it) }
    content.video?.video?.also { setFromTdApiFile(it) }
    content.video?.also {
      if(!it.fileName.isNullOrBlank()) fileName = it.fileName
      if(!it.mimeType.isNullOrBlank()) mimeType = it.mimeType
    }
  }

  private fun setFromAudio(content: TdApi.MessageAudio){
    caption = content.caption?.text
    path = caption?.trim()
//    path = content.caption?.text?.let { getPathFromCaption(it) }
    content.audio?.audio?.also { setFromTdApiFile(it) }
    content.audio?.also {
      if(!it.fileName.isNullOrBlank()) fileName = it.fileName
      if(!it.mimeType.isNullOrBlank()) mimeType = it.mimeType
    }
  }

  private fun setFromPhoto(content: TdApi.MessagePhoto){
    caption = content.caption?.text
    path = caption?.trim()
//    path = content.caption?.text?.let { getPathFromCaption(it) }
    content.photo?.sizes?.also {
      var height = 0
      var width = 0
      for(photo in it) {
        if(photo != null && (photo.height > height || photo.width > width)) {
          height = photo.height
          width = photo.width
          photo.photo?.also { file -> setFromTdApiFile(file) }
        }
      }
    }
  }

  private fun setFromAnimation(content: TdApi.MessageAnimation){
    caption = content.caption?.text
    path = caption?.trim()
//    path = content.caption?.text?.let { getPathFromCaption(it) }
    content.animation?.animation?.also { setFromTdApiFile(it) }
    content.animation?.also {
      if(!it.fileName.isNullOrBlank()) fileName = it.fileName
      if(!it.mimeType.isNullOrBlank()) mimeType = it.mimeType
//      if(!it.fileName.isNullOrBlank()) fileName = it.fileName.replace(".gif", ".mp4", ignoreCase = true)
//      if(!it.mimeType.isNullOrBlank()) mimeType = "video/mp4"
    }
  }

  private fun setFromSticker(content: TdApi.MessageSticker){
    content.sticker?.sticker?.also { setFromTdApiFile(it) }
    fileUniqueId?.also {
      fileName = "$it.webp"
      mimeType = "image/webp"
      path = "${Constants.stickerDir}/${fileName}"
    }
  }

  private fun setFromTdApiFile(file: TdApi.File) {
    val update = FileUpdate(file)
    fileId = update.fileId
    size = update.size
    fileUniqueId = update.fileUniqueId
    upload = update.upload
    uploaded = update.uploaded
    localPath = update.localPath
    update.filePath.also { if(!it.isNullOrBlank()) path = it }
    download = update.download
    downloaded = update.downloaded


//    fileId = file.id
//    size = file.size
//
//    file.remote?.also {
//      if(!it.uniqueId.isNullOrBlank()) {
//        fileUniqueId = it.uniqueId
//      }
//      if(!it.isUploadingActive && it.isUploadingCompleted) {
//        uploaded = true
//      }
//    }
//    file.local?.also { local ->
//      localPath = local.path.let { if(it.isNullOrBlank()) null else it }
//      localPath?.also {
//        if(it.matches("${Fs.syncDirAbsPath}/.+".toRegex(RegexOption.DOT_MATCHES_ALL))){
//          path = it.replace("${Fs.syncDirAbsPath}/", "")
//          upload = true
//        }
//      }
//      downloaded = !local.isDownloadingActive && local.isDownloadingCompleted
//    }
  }

  private fun getPathFromCaption(caption: String): String? {
    return Regex("${Constants.FOLDER_ICON}[^\r\n\t]*", RegexOption.MULTILINE)
      .find(caption)
      ?.value
      ?.replace(Constants.FOLDER_ICON, "")
  }

  private fun log(){
    Log.d("messageId", messageId.toString())
    Log.d("chatId", chatId.toString())
    Log.d("fileId", fileId.toString())
    Log.d("fileUniqueId", fileUniqueId.toString())
    Log.d("fileName", fileName.toString())
    Log.d("path", path.toString())
    Log.d("mimeType", mimeType.toString())
    Log.d("localPath", localPath.toString())
    Log.d("downloaded", downloaded.toString())
    Log.d("uploaded", uploaded.toString())
  }

}