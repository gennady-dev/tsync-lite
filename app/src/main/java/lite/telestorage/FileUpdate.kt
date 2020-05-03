package lite.telestorage

import org.drinkless.td.libcore.telegram.TdApi

class FileUpdate internal constructor() {

  var isUploadingCompleted = false
    private set
  var fileType: String? = null
  var messageId: Long = 0
  var chatId: Long = 0
  var message: TdApi.Message? = null
  var fileId: Int = 0
  var filePath: String? = null
  var local: TdApi.LocalFile? = null
  var downloaded = false
  var localPath: String? = null
  var remote: TdApi.RemoteFile? = null
  var fileUniqueId: String? = null
  var uploaded = false
  var upload = false
  var downloading = false
  var size: Int = 0


  internal constructor(updateFile: TdApi.File) : this() {
    local = updateFile.local
    remote = updateFile.remote
    fileId = updateFile.id
    size = updateFile.size
    remote?.let {
      uploaded = !it.isUploadingActive && it.isUploadingCompleted
      fileUniqueId = if(!it.uniqueId.isNullOrBlank()) it.uniqueId else null
    }
    local?.let {
      downloaded = !it.isDownloadingActive && it.isDownloadingCompleted
      localPath = if(!it.path.isNullOrBlank()) it.path else null
    }
    localPath?.also {
      if(it.matches("${Constants.tdLibPath}.+".toRegex(RegexOption.DOT_MATCHES_ALL))){
        downloading = true
      } else {
        Fs.syncDirAbsPath?.also { syncDir ->
          if(it.matches("""${Regex.escape(syncDir)}.+""".toRegex(RegexOption.DOT_MATCHES_ALL))) {
            upload = true
            filePath = it.replace("${Fs.syncDirAbsPath}/", "")
          }
        }
      }
    }
    val test = null
  }

}
