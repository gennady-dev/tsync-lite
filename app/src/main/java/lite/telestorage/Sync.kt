package lite.telestorage

//import lite.telestorage.kt.database.FileHelper
import lite.telestorage.models.FileData
import java.io.File
import java.util.Date
import kotlin.concurrent.withLock


object Sync {

  var inProgress: Long = 0
  var syncStatus: SettingsFragment.SyncStatus? = null
  var dataTransferInProgress: Long = 0
  var syncFileInProgress = false
  private var isFileDbCleared = false

  //    public static long sUploadingInProgress;
  //    public static long sDownloadingInProgress;
  var waitingTime = 60000L
  var fileByPathMap: MutableMap<String, FileData> = mutableMapOf()
  var fileByMsgIdMap: MutableMap<Long, FileData> = mutableMapOf()
  var updateMap: MutableMap<Long, FileData> = mutableMapOf()
  var pathByMsgIdMap: MutableMap<Long, String> = mutableMapOf()
  var fileList: MutableList<FileData> = mutableListOf()
  val ready
    get() = Settings.authenticated
      && Settings.enabled
      && Settings.path != null
      && Settings.chatId != 0L
      && Settings.groupId != 0

  fun start(type: Type = Type.ALL) {

    if(ready && Data.inProgress == 0L) {
      if(type == Type.ALL){
        Data.deletedMsgIds.clear()
        Data.remoteFileList.clear()
        Data.localFileList.clear()
        syncFiles()
        Messages.getMessages()
      } else if(type == Type.LOCAL){
        Data.syncFiles()
        if(Data.dataTransferInProgress == 0L) FileUpdates.nextDataTransfer()

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

  fun stop(){
    if(!Data.stop) Data.stop = true
  }

  fun syncFiles() {
    Data.lock.withLock {
      if(Settings.path != null) {
        Fs.scanPath()
        for(abs in Data.absPathList) {
          val relPath: String? = Fs.getRelPath(abs)
          if(relPath != null) {
            val localFile = File(abs)
            val file = FileData()
            file.path = relPath
            file.chatId = Settings.chatId
            file.name = localFile.name
            file.mimeType = Fs.getMimeType(abs)
            file.downloaded = true
            file.lastModified = localFile.lastModified()
            file.size = localFile.length().toInt()
            Data.localFileList.add(file)
          }
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


//  private fun clearFileDb() {
//    if(!isFileDbCleared) {
//      if(Settings.chatId != 0L) {
//        FileHelper.leaveByChatId(Settings.chatId)
//      }
//      isFileDbCleared = true
//    }
//  }

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
