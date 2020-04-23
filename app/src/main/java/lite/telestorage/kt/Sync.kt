package lite.telestorage.kt

import lite.telestorage.kt.database.FileHelper
import lite.telestorage.kt.models.FileData
import java.io.File
import java.util.Date


object Sync {

  var inProgress: Long = 0
  var syncStatus: SettingsFragment.SyncStatus? = null
  var dataTransferInProgress: Long = 0
  private var isFileDbCleared = false

  //    public static long sUploadingInProgress;
  //    public static long sDownloadingInProgress;
  var waitingTime = 60000L
  var fileByPathMap: MutableMap<String, FileData> = mutableMapOf()
  var fileByMsgIdMap: MutableMap<Long, FileData> = mutableMapOf()
  var updateMap: MutableMap<Long, FileData> = mutableMapOf()
  var pathByMsgIdMap: MutableMap<Long, String> = mutableMapOf()
  var fileList: MutableList<FileData> = mutableListOf()

  private val isSetGroup: Boolean
    get() = Settings.let { it.chatId != 0L && it.supergroupId != 0 }

  fun start(type: Type = Type.ALL) {
    var sync = false
      clearFileDb()
      val path: String? = Settings.path
      //              if(Tg.haveAuthorization && settings.isEnabled() && path != null && isSetGroup()){
      if(Settings.authenticated && Settings.enabled && path != null && isSetGroup) {
        sync = true
      }

    if(Data.inProgress == 0L && sync) {
      if(type == Type.ALL){
        Data.clearData()
        syncFiles()
        Messages.getMessages()
      } else if(type == Type.LOCAL){
//        if(Data.dbFileList.isEmpty()){
//          Data.clearData()
//          syncFiles()
//          Messages.getMessages()
//        } else {
//          Data.syncFiles()
//          if(Data.fileQueue.isNotEmpty() && Data.dataTransferInProgress == 0L) FileUpdates.nextDataTransfer()
//        }
      }
    }
  }

  fun afterMessageSynced() {
    Tg.needUpdate = false
    nextDataTransfer()
  }

  fun nextDataTransfer() {
    Tg.uploadNextFile()
    Tg.downloadNextFile()
  }

  private fun syncFiles() {
    if(Settings.path != null) {
      Data.clearLocal()
      Fs.scanPath()
      Data.absPathList
      FileHelper.setFileList()
      Data.deleteDbDuplicates()

      for(abs in Data.absPathList) {
        val relPath: String? = Fs.getRelPath(abs)
        if(relPath != null) {
          var file: FileData? = Data.dbFileList.find { it.path == relPath }
          val localFile = File(abs)
          if(file == null) {
            file = FileData()
            file.name = localFile.name
            file.mimeType = Fs.getMimeType(abs)
            file.downloaded = true
            file.path = relPath
            file.lastModified = localFile.lastModified()
            file.size = localFile.length().toInt()
            Data.newLocalFileList.add(file)
          } else {
//            if(localFile.lastModified() > file.lastModified){
//              file.uploaded = false
//            }
            file.lastModified = localFile.lastModified()
          }
          Data.localFileList.add(file)
        }
      }
    }
  }

//  private fun getPath(path: String): String? {
//    return path
////    return Fs.syncDirAbsPath?.let {
////      if(path.matches("$it.+".toRegex(RegexOption.DOT_MATCHES_ALL))) path.replace("$it/", "")
////      else null
////    }
//  }📁📁


  private fun clearFileDb() {
    if(!isFileDbCleared) {
      if(Settings.chatId != 0L) {
        FileHelper.leaveByChatId(Settings.chatId)
      }
      isFileDbCleared = true
    }
  }

  fun updateDataTransferProgressStatus() {
    val currentTime = Date().time
    if(Data.dataTransferInProgress != 0L && Data.dataTransferInProgress + waitingTime < currentTime) {
      Data.dataTransferInProgress = 0
    }
  }

  fun updateProgressStatus() {
    if(Data.dataTransferInProgress == 0L) {
      Data.inProgress = 0
      syncStatus?.setInProgress(false)
    } else {
      Data.inProgress = Data.dataTransferInProgress + waitingTime
      syncStatus?.setInProgress(true)
    }
  }

}

enum class Type {
  ALL, LOCAL, REMOTE
}
