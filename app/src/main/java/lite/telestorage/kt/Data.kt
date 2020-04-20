package lite.telestorage.kt

import lite.telestorage.kt.database.FileHelper
import lite.telestorage.kt.models.FileData
import org.drinkless.td.libcore.telegram.TdApi
import java.util.Date
import java.util.concurrent.locks.ReentrantLock

object Data {

  val lock = ReentrantLock()
  private const val waitingTime = 60000L

  var inProgress: Long = 0
    set(value) {
      if((if(field == 0L) 0L else 1L) != (if(value == 0L) 0L else 1L)){
        if(value == 0L) Sync.syncStatus?.setInProgress(false)
        else Sync.syncStatus?.setInProgress(true)
      }
      field = value
    }
    get() {
      val prev = field.let { if(it == 0L) 0L else 1L }
      if(field != 0L && field + waitingTime < Date().time) field = 0
      val next = field.let { if(it == 0L) 0L else 1L }
      if(prev != next){
        if(field == 0L) Sync.syncStatus?.setInProgress(false)
        else Sync.syncStatus?.setInProgress(true)
      }
      return field
    }


  var dataTransferInProgress: Long = 0
    set(value) {
      field = value
      inProgress = value
    }
    get() {
      if(field != 0L && field + waitingTime < Date().time) field = 0
      if(field == 0L) inProgress = 0
      return field
    }

  var current: FileData? = null
  var fileQueue: MutableList<FileData> = mutableListOf()

  val fileAbsPathList: MutableSet<String> = mutableSetOf()
  private val oldFileAbsPathList: MutableSet<String> = mutableSetOf()
  val dbFileList: MutableList<FileData> = mutableListOf()
  val localFileList: MutableList<FileData> = mutableListOf()
  val newLocalFileList: MutableList<FileData> = mutableListOf()
  val remoteFileList: MutableList<FileData> = mutableListOf()

  val msgIdsForDelete: MutableList<Long> = mutableListOf()
  val deletedMsgIds: MutableSet<Long> = mutableSetOf()

  val localToDelete: MutableList<FileData> = mutableListOf()
  val remoteToDelete: MutableList<FileData> = mutableListOf()

//  fun addDb(file: FileData){
//    dbFileList.
//    val path: String? = file.path
//    var last = file
//    var lastMsgId = file.messageId
//    if(path != null) {
//      val current = dbPathMap[path]
//      if(current != null) {
//        if(current.messageId > file.messageId){
//          lastMsgId = current.messageId
//          remoteToDelete.add(file)
//        } else {
//          remoteToDelete.add(file)
//          dbFileList.remove(current)
//          if(current.messageId != 0L){
//            dbMsgIdMap.remove(current.messageId)
//          }
//          dbPathMap[path] = last
//        }
//      }
//    }
//    if(lastMsgId != 0L) {
//      dbMsgIdMap[lastMsgId] = last
//    }
//    dbFileList.add(last)
//  }

//  fun addLocal(file: FileData){
//    localFileList.add(file)
//    file.path?.also {
//      localFilePathMap[it] = file
//    }
//    if(file.messageId != 0L) {
//      localFileMsgIdMap[file.messageId] = file
//    }
//  }

//  fun addNewLocal(file: FileData){
//    addLocal(file)
//    fileQueue.add(file)
//    newLocalFileList.add(file)
//    file.path?.also {
//      newLocalFilePathMap[it] = file
//    }
//  }
//
//  fun addRemote(file: FileData, srcFile: FileData? = null) {
//    remoteFileList.add(file)
//    file.path?.also {
//      remoteFilePathMap[it] = file
//    }
//    if(file.messageId != 0L) {
//      remoteFileMsgIdMap[file.messageId] = file
//    }
//    srcFile?.path?.also {
//      remoteDuplicateMap[it].also { set ->
//        if(set == null) remoteDuplicateMap[it] = mutableSetOf(srcFile)
//        else set.add(srcFile)
//      }
//    }
//  }

//  fun addAll(file: FileData){
//    addDb(file)
//    addLocal(file)
//    addRemote(file)
//  }

  fun clearLocal(){
    dbFileList.clear()
    localFileList.clear()
    newLocalFileList.clear()
  }

  fun clearData(){
    clearLocal()
    remoteToDelete.clear()
    remoteFileList.clear()
    msgIdsForDelete.clear()
    deletedMsgIds.clear()
  }

  fun addFromMsg(file: FileData){
    val path = file.path
    if(
      Settings.chatId != 0L
      && Settings.chatId == file.chatId
      && file.messageId != 0L
      && file.fileId != 0
      && file.fileUniqueId != null
      && path != null
      && file.uploaded
    ){
      remoteFileList.add(file)
//      val fileData: FileData = localFilePathMap[path]?.also {
//          if(it.size == file.size){
//            if(file.messageId > it.messageId){
//              it.downloaded = true
//              it.uploaded = true
//              removeByMsgId(it.messageId)
//              copyFile(file, it)
//            } else {
//              removeByMsgId(file.messageId)
//            }
//          } else {
//            if(file.messageId > it.messageId){
//              it.downloaded = !Settings.downloadMissing
//              it.uploaded = true
//              removeByMsgId(it.messageId)
//              copyFile(file, it)
//            } else {
//              removeByMsgId(file.messageId)
//            }
//          }
//      } ?: file.also {
//          it.downloaded = !Settings.downloadMissing
//      }
//      addRemote(fileData)
//      addForTransfer(fileData)
    }
  }

  fun copyFile(src: FileData, dst: FileData){
    dst.messageId = src.messageId
    dst.fileUniqueId = src.fileUniqueId
    dst.fileId = src.fileId
    dst.date = src.date
    dst.editDate = src.editDate
    dst.size = src.size
  }
//
//  fun addForTransfer(file: FileData){
//    if(file.updated) fileQueue.add(file)
//  }
//
//  fun isDownloaded(path: String?): Boolean {
//    return if(path == null) false else (Fs.getAbsPath(path)?.let { abs ->
//      if(File(abs).exists()) true
//      else !Settings.downloadMissing
//    } ?: false)
//  }
//
//  fun isDownloaded(file: FileData): Boolean {
//    val path = file.path
//    if(path == null) return false
//    val absPath = Fs.getAbsPath(path)
//    return if(absPath != null){
//      val localFile = File(absPath)
//      localFile.exists() && localFile.length().toInt() == file.size
//    } else false
//  }

//  fun isDownloaded(local: FileData, remote: FileData): Boolean {
//    val localDate = if(local.editDate == 0L) local.date else local.editDate
//    val remoteDate = if(remote.editDate == 0L) remote.date else remote.editDate
//    val localSize = local.size
//    val remoteSize = remote.size
//
//    return if(localSize != 0 && localSize == remoteSize) true
//    else localDate != 0L && localDate == remoteDate
//  }
//
//  fun removeRemoteByMsgId(msgId: Long) {
//    remoteFileMsgIdMap[msgId]?.also {
//      remoteFileList.remove(it)
//      it.path?.also { path -> remoteFilePathMap.remove(path) }
//      remoteFileMsgIdMap.remove(msgId)
//    }
//    msgIdsForDelete.remove(msgId)
//    deletedMsgIds.add(msgId)
//  }
//
//  fun removeByMsgId(msgId: Long) {
//    if(msgId != 0L){
//      remoteFileMsgIdMap.remove(msgId)
//      dbMsgIdMap.remove(msgId)
//      localFileMsgIdMap.remove(msgId)
//      msgIdsForDelete.add(msgId)
//    }
//  }

  fun syncFiles(){

//    oldFileAbsPathList.clear()
//    oldFileAbsPathList.addAll(fileAbsPathList)
//    Fs.scanPath()
//    val removedFiles = oldFileAbsPathList.subtract(fileAbsPathList)
//    val newFiles = fileAbsPathList.subtract(oldFileAbsPathList)
//    for(abs in removedFiles){
//      Fs.getRelPath(abs)?.also { path ->
//        dbPathMap[path]?.also { file ->
//          file.downloaded = isDownloaded(path)
//          addForTransfer(file)
//        }
//      }
//    }
//    for(abs in newFiles){
//      Fs.getRelPath(abs)?.also { path ->
//        dbPathMap[path].also {
//          var file = it
//          if(file == null) {
//            val localFile = File(abs)
//            file = FileData()
//            file.name = localFile.name
//            file.mimeType = Fs.getMimeType(abs)
//            file.downloaded = true
//            file.path = path
//            file.lastModified = localFile.lastModified()
//            file.size = localFile.length().toInt()
//            addNewLocal(file)
//          } else {
//            file.uploaded = false
//            addForTransfer(file)
//            addLocal(file)
//          }
//        }
//      }
//    }
//    debugInfo()
  }

//  fun checkDuplicate(){
//    val duplicates = remoteDuplicateMap.filter { it.value.size > 1 }
//    for(list in duplicates.values){
//      var isDuplicates = true
//      var lastMsgId = 0L
//      var size = 0
//      val duplicateSet = mutableSetOf<Long>()
//      for(file in list){
//        if(size == 0) size = file.size
//        else if(size != file.size) isDuplicates = false
//        if(lastMsgId < file.messageId) lastMsgId = file.messageId
//        duplicateSet.add(file.messageId)
//      }
//      if(isDuplicates){
//        duplicateSet.remove(lastMsgId)
//        msgIdsForDelete.addAll(duplicateSet)
//      }
//    }
//  }

  fun deleteMessages(update: TdApi.UpdateDeleteMessages){
//    debugInfo()
    val chatId = Settings.chatId
    if(update.isPermanent && update.chatId == chatId){
      val deletedMsg = update.messageIds.intersect(msgIdsForDelete)
      val newToDeleteLocal = update.messageIds.subtract(msgIdsForDelete)
      val remoteToDelete = msgIdsForDelete.subtract(update.messageIds.toList())
      if(newToDeleteLocal.isNotEmpty()){
        val dbMsgIdMap = dbFileList.associateBy { it.messageId }
        for(id in newToDeleteLocal){
          dbMsgIdMap[id]?.also {
            clearMsgData(it)
            it.uploaded = !Settings.uploadMissing
            it.downloaded = true
            fileQueue.add(it)
          }
        }
      }
      msgIdsForDelete.removeAll(deletedMsg)
      if(remoteToDelete.isNotEmpty()){
        Messages.deleteMsgByIds(remoteToDelete.toLongArray())
      } else {
        msgIdsForDelete.clear()
        dataTransferInProgress = 0
        FileUpdates.nextDataTransfer()
      }
    }
//    debugInfo()
  }

  fun clearMsgData(file: FileData){
    file.messageId = 0
    file.fileId = 0
    file.fileUniqueId = null
    file.date = 0
    file.editDate = 0
  }

  fun debugInfo(){

    inProgress
    dataTransferInProgress

    current
    fileQueue

    remoteToDelete

    msgIdsForDelete

    fileAbsPathList
    oldFileAbsPathList

    dbFileList

    localFileList

    newLocalFileList

    remoteFileList
  }


}