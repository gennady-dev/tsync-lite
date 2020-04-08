package lite.telestorage.kt

import lite.telestorage.kt.database.FileHelper
import lite.telestorage.kt.database.SettingsHelper
import lite.telestorage.kt.models.FileData
import lite.telestorage.kt.models.Settings
import java.io.File
import java.util.*


object Sync {
  var inProgress: Long = 0
  var syncStatus: SettingsFragment.SyncStatus? = null
  var dataTransferInProgress: Long = 0
  private var isFileDbCleared = false

  //    public static long sUploadingInProgress;
  //    public static long sDownloadingInProgress;
  var waitingTime = 60000L
  var fileByPathMap: MutableMap<String, FileData> = mutableMapOf()
  var updateMap: MutableMap<Long, FileData> = mutableMapOf()
  var pathByMsgIdMap: MutableMap<Long, String> = mutableMapOf()

  private val fileHelper: FileHelper?
    get() = FileHelper.get()



  fun init() {
//        if(sFileDataMap == null || sUpdateMap == null || sPathByIdMap == null){
//            sFileDataMap = new ConcurrentHashMap<String, FileData>();
//            sPathByIdMap = new ConcurrentHashMap<Long, String>();
//            sUpdateMap = new ConcurrentHashMap<Long, FileData>();
//        }
  }

  fun start() {
    println("sInProgress $inProgress")
    var sync = false
    val settingsHelper: SettingsHelper? = SettingsHelper.get()
    if(settingsHelper != null) {
      val settings: Settings? = settingsHelper.settings
      if(settings != null) {
        clearFileDb()
        val path: String? = settings.path
        //              if(Tg.haveAuthorization && settings.isEnabled() && path != null && isSetGroup()){
        if(settings.authenticated && settings.enabled && path != null && isSetGroup) {
          sync = true
        }
      }
    }
    val currentTime = Date().time
    if(inProgress != 0L && inProgress + waitingTime < currentTime) {
      inProgress = 0
      syncStatus?.setInProgress(false)
    }
    if(inProgress == 0L && sync) {
      inProgress = currentTime
      syncStatus?.setInProgress(true)
      syncFiles()
      Messages.get().getMessages()
    }
  }

  fun afterMessageSynced() {
    Tg.needUpdate = false
    nextDataTransfer()
  }

  fun nextDataTransfer() {
    Tg.get()?.uploadNextFile()
    Tg.get()?.downloadNextFile()
  }

  fun syncFiles() {
    val settings: Settings? = SettingsHelper.get()?.settings
    if(settings?.path != null && fileHelper != null) {
      Fs.filePaths.clear()
      Fs.scanPath()
      fileByPathMap.clear()
      fileHelper?.setFileDataMap()
      if(Fs.filePaths.size > 0) {
        for(absPath in Fs.filePaths) {
          val relativePath: String? = Fs.getRelativePath(absPath)
          if(relativePath != null) {
            var file: FileData? = fileByPathMap[relativePath]
            if(file == null) {
              file = FileData()
              file.name = File(absPath).name
              file.mimeType = Fs.getMimeType(absPath)
              file.uploaded = false
              file.downloaded = true
              file.path = relativePath
              file.inProgress = false
              file.lastModified = File(absPath).lastModified()
              fileByPathMap[relativePath] = file
            }
          }
        }
      }
    }
  }

  private fun clearFileDb() {
    if(!isFileDbCleared) {
      val settings: Settings? = SettingsHelper.get()?.settings
      if(settings != null && settings.chatId != 0L) {
        FileHelper.get()?.leaveByChatId(settings.chatId)
      }
      isFileDbCleared = true
    }
  }

  fun updateDataTransferProgressStatus() {
    val currentTime = Date().time
    if(dataTransferInProgress != 0L && dataTransferInProgress + waitingTime < currentTime) {
      dataTransferInProgress = 0
    }
  }

  fun updateProgressStatus() {
    if(dataTransferInProgress == 0L) {
      inProgress = 0
      syncStatus?.setInProgress(false)
    } else {
      inProgress = dataTransferInProgress + waitingTime
      syncStatus?.setInProgress(true)
    }
  }

  //    public static void syncDb(){
  val isSetGroup: Boolean
    get() {
      val settings: Settings? = SettingsHelper.get()?.settings
      return settings != null && settings.chatId != 0L && settings.supergroupId != 0
    }


  //        Settings settings = SettingsHelper.get().getSettings();
  //        FileHelper fileHelper = FileHelper.get();
  //        if(settings != null && settings.getPath() != null && fileHelper != null){
  //            List<FileData> files = fileHelper.getFiles(
  //                FileTable.Cols.MESSAGE_ID + " IS NOT NULL" + " OR " + FileTable.Cols.MESSAGE_ID + " <> ?",
  //                new String[]{"0"});
  //            ConcurrentMap<Long, TdApi.Message> messageMap = null;
  //            if(Messages.get().mLastMessageMap != null && Messages.get().mLastMessageMap.size() > 0){
  //                messageMap = Messages.get().mLastMessageMap;
  //            }
  //            for(FileData file : files){
  //                String absPath = Fs.getFileAbsPath(file.getPath());
  //                File localFile = new File(absPath);
  //                if(messageMap != null){
  //                    long messageId = file.getMessageId();
  //                    if(messageId != 0){
  //                        TdApi.Message message = messageMap.get(messageId);
  //                        if(localFile.exists()){
  //                            if(message == null){
  //                                file.setUploaded(false);
  //                                file.setMessageId(0);
  //                                file.setFileId(0);
  //                                file.setFileUniqueId("");
  //                                fileHelper.updateFile(file);
  //                            }
  //                        } else {
  //                            if(message == null){
  //                                FileHelper.get().delete(file);
  //                            }
  //                        }
  //                    }
  //                }
  //            }
  //        }
  //    }
}
