package lite.telestorage

import android.util.Log
import lite.telestorage.models.FileData
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File
import java.util.Timer
import java.util.TimerTask
import java.util.Date
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.schedule
import kotlin.concurrent.timer
import kotlin.concurrent.withLock

object Data {

  val lock = ReentrantLock()
  private const val waitingTime = 60000L
  const val localSyncPeriod = 60000L
  var lastLocalSync: Long = 0
  private var progressTimer: Timer? = null
  var stop = false

  var inProgress: Long = 0
    set(value) {
      if((if(field == 0L) 0L else 1L) != (if(value == 0L) 0L else 1L)){
        if(value == 0L) Sync.syncStatus?.setInProgress(false)
        else Sync.syncStatus?.setInProgress(true)
      }
      field = value
    }
    get() {
//      val prev = field.let { if(it == 0L) 0L else 1L }
//      if(field != 0L && field + waitingTime < Date().time){
//        field = 0
//        resetProgressTask?.cancel()
//      }
//      val next = field.let { if(it == 0L) 0L else 1L }
//      if(prev != next){
//        if(field == 0L) Sync.syncStatus?.setInProgress(false)
//        else Sync.syncStatus?.setInProgress(true)
//      }
      return field
    }


  var dataTransferInProgress: Long = 0
    set(value) {
      field = value
      scheduleProgressReset(value)
    }
    get() {
      if(field != 0L && field + waitingTime < Date().time) field = 0
      return field
    }

  val currentLock = ReentrantLock()
  var current: FileData? = null //TODO to lock by new lock only for this property

  var fileQueue: MutableList<FileData> = mutableListOf()
  var dataTimer: TimerTask? = null
  var resetProgressTask: TimerTask? = null

  val absPathList: MutableSet<String> = mutableSetOf()
  val pathMap: MutableMap<String, String> = mutableMapOf()
  private val oldAbsPathList: MutableSet<String> = mutableSetOf()

  val fileList: MutableList<FileData> = mutableListOf()
  val dbFileList: MutableList<FileData> = mutableListOf()
  val localFileList: MutableList<FileData> = mutableListOf()
//  val newLocalFileList: MutableList<FileData> = mutableListOf()
  val remoteFileList: MutableList<FileData> = mutableListOf()

  val msgIdsForDelete: MutableSet<Long> = mutableSetOf()
  val deletedMsgIds: MutableSet<Long> = mutableSetOf()

  val toDeleteFromDb: MutableList<FileData> = mutableListOf()
  val remoteToDelete: MutableList<FileData> = mutableListOf()

  fun setTimerTask(){
//    if(progressTimer == null){
      progressTimer = timer("progressTimer", false, 0, 60){
        if(inProgress + waitingTime < Date().time) inProgress = 0
      }
//    }
  }

  private fun scheduleProgressReset(value: Long){
    progressTimer?.cancel()
    progressTimer = Timer(Constants.progressTimer, false)
    if(value == 0L){
      progressTimer?.schedule(1000L){
        inProgress = 0
        Log.d("progressTimer", "task done")
      }
    } else {
      inProgress = value
      progressTimer?.schedule(value + waitingTime){
        inProgress = 0
        Log.d("progressTimer", "task done")
      }
    }
//    resetProgressTask?.cancel()
//    if(value == 0L){
//      resetProgressTask = Timer().schedule(1000L){ inProgress = 0 }
//    } else {
//      inProgress = value
//      resetProgressTask = Timer().schedule(value + waitingTime){ inProgress = 0 }
//    }
//    Timer().schedule(5000L){ scheduleProgressReset(5000L) }
  }

  fun deleteMsgDuplicates(){
    remoteFileList.groupBy { it.path }.also { byPath ->
      for(path in byPath.values){
        val sortedById = path.sortedByDescending { it.messageId }
        val last = sortedById.first()
        val lastSize = last.size
        val lastId = last.messageId
        val toDelete: MutableSet<FileData> = mutableSetOf()
        for(f in sortedById){
          if(f.messageId == lastId) continue
          else if(f.messageId < lastId && f.size == lastSize){
            toDelete.add(f)
          } else break;
        }
        remoteFileList.removeAll(toDelete)
        msgIdsForDelete.addAll(toDelete.map { f -> f.messageId })


//        path.groupBy { it.size }.also { bySize ->
//          bySize[lastSize]
//            ?.sortedByDescending { it.messageId }?.also { sorted ->
//            for((i, f) in sorted.withIndex()){
//              if(f.messageId == sortedById[i].messageId){
//                if(f.messageId != lastId) toDelete.add(f)
//              } else break
//            }
//          }
//          remoteFileList.removeAll(toDelete)
//          msgIdsForDelete.addAll(toDelete.map { f -> f.messageId })
//          if(toDelete.isNotEmpty()){
//            println(msgIdsForDelete)
//          }
//
////          for(size in bySize.values){
////            size.maxBy { it.messageId }?.also { file ->
////              size.minus(file).also {
////                remoteFileList.removeAll(it)
////                msgIdsForDelete.addAll(it.map { f -> f.messageId })
////              }
////            }
////          }
//        }
      }
    }
  }

  fun deleteDbDuplicates(){
    dbFileList.groupBy { it.path }.also { byPath ->
      for(path in byPath.values){
        path.groupBy { it.size }.also { bySize ->
          for(size in bySize.values){
            size.maxBy { msg -> msg.messageId }?.also { file ->
              size.minus(file).also { l ->
                dbFileList.removeAll(l)
                toDeleteFromDb.addAll(l)
              }
            }
          }
        }
      }
    }
  }

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

//  fun clearLocal(){
//    dbFileList.clear()
//    localFileList.clear()
////    newLocalFileList.clear()
//  }
//
//  fun clearData(){
//    clearLocal()
//    remoteToDelete.clear()
//    remoteFileList.clear()
//    msgIdsForDelete.clear()
//    deletedMsgIds.clear()
//  }

  fun addFromMsg(file: FileData){
    val path = file.path
    if(
      Settings.chatId != 0L
      && Settings.chatId == file.chatId
      && file.messageId != 0L
      && file.fileId != 0
      && file.fileUniqueId != null
      && file.uploaded
    ){
      remoteFileList.find { it.messageId == file.messageId }
        .also { if(it == null) remoteFileList.add(file) }
//      syncLock.withLock {
//        val local = fileQueue.find { it.fileId == file.fileId }?.also { q ->
//          if(q.messageId == file.messageId){
//            q.uploaded = true
//            q.date = file.date
//            q.editDate = file.editDate
//          } else if(q.messageId < file.messageId){
//            msgIdsForDelete.add(q.messageId)
//            q.messageId = file.messageId
//            q.uploaded = true
//            q.date = file.date
//            q.editDate = file.editDate
//          } else if(q.messageId > file.messageId){
//            msgIdsForDelete.add(file.messageId)
//          }
//        } ?: fileQueue.find { it.path == file.path }?.also { q ->
//          if(q.lastDate == 0L){
//            if(q.size == file.size){
//              q.messageId = file.messageId
//              q.fileId = file.fileId
//              q.fileUniqueId = file.fileUniqueId
//              q.uploaded = true
//              q.date = file.date
//              q.editDate = file.editDate
//            } else {
//
//            }
//          }
//
//
//          if(q.messageId == file.messageId){
//            if(q.size == file.size){
//              q.fileId = file.fileId
//              q.fileUniqueId = file.fileUniqueId
//              q.uploaded = true
//              q.date = file.date
//              q.editDate = file.editDate
//            } else {
//              if(q.lastDate < file.lastDate) {
//                q.fileId = file.fileId
//                q.fileUniqueId = file.fileUniqueId
//                q.uploaded = true
//                q.date = file.date
//                q.editDate = file.editDate
//              } else {
//                q.fileId = file.fileId
//                q.fileUniqueId = file.fileUniqueId
//                q.uploaded = false
//                q.date = file.date
//                q.editDate = file.editDate
//              }
//            }
//          } else if(q.messageId < file.messageId){
//            msgIdsForDelete.add(q.messageId)
//            q.messageId = file.messageId
//            q.downloaded = true
//            q.uploaded = true
//            q.date = file.date
//            q.editDate = file.editDate
//          } else if(q.messageId > file.messageId){
//            msgIdsForDelete.add(file.messageId)
//          }
//        }
//
//
//        local?.also { f -> FileUpdates.addToQueue(f) }
//      }

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

  fun syncFiles() {
    oldAbsPathList.clear()
    oldAbsPathList.addAll(absPathList)
    Fs.scanPath()
    val removedFiles = oldAbsPathList.subtract(absPathList)
    val newFiles = absPathList.subtract(oldAbsPathList)
    val dbPathMap = dbFileList.groupBy { it.path }
    for(abs in removedFiles) {
      Fs.getRelPath(abs)?.also { path ->
        dbPathMap[path]?.maxBy { it.messageId }?.also { file ->
          file.downloaded = false
          fileQueue.add(file)
        }
      }
    }
    for(abs in newFiles) {
      Fs.getRelPath(abs)?.also { path ->
        val localFile = File(abs)
        val size = localFile.length().toInt()
        val file = FileData()
        file.path = path
        file.name = localFile.name
        file.mimeType = Fs.getMimeType(abs)
        file.lastModified = localFile.lastModified()
        file.size = size
        file.downloaded = true
        val list = dbPathMap[path]
        if(list == null) {
          file.uploaded = false
        } else {
          list.find { it.size == size }.also {
            if(it == null) {
              file.uploaded = false
            } else {
              file.uploaded = true
              file.messageId = it.messageId
              file.fileUniqueId = it.fileUniqueId
              file.fileId = it.fileId
              file.date = it.date
              file.editDate = it.editDate
            }
          }
        }
        fileQueue.add(file)
      }
    }
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

  fun deleteMessages(update: TdApi.UpdateDeleteMessages) {
    if(
      update.isPermanent
      && !update.fromCache
      && update.chatId == Settings.chatId
    ) {
//      FileUpdates.deleteTimerTask?.cancel()
//      val dbMsgIdMap = dbFileList.associateBy { it.messageId }
//      for(msgId in update.messageIds) {
//        dbMsgIdMap[msgId]?.also {
//          it.messageId = 0
//          it.fileId = 0
//          it.fileUniqueId = null
//          it.date = 0
//          it.editDate = 0
//          it.uploaded = false
//        }
//      }
      deletedMsgIds.addAll(update.messageIds.toSet())
      if(dataTransferInProgress == 0L) FileUpdates.nextDataTransfer()
//      Sync.syncFiles()
//      Messages.getMessages()
//      deletedMsgIds.addAll(update.messageIds.toSet())
//      if(dataTransferInProgress == 0L) FileUpdates.nextDataTransfer()
//      if(remoteFileList.isNotEmpty()){
//
//      }
//      if(msgIdsForDelete.subtract(update.messageIds.toList()).isEmpty())
//      remoteFileList.clear()
//      localFileList.clear()
//      Sync.syncFiles()
//      Messages.getMessages()
    }
  }

//  fun deleteMessages(update: TdApi.UpdateDeleteMessages){
////    debugInfo()
//    val chatId = Settings.chatId
//    if(update.isPermanent && update.chatId == chatId){
//      FileUpdates.deleteTimerTask?.cancel()
//      val deletedMsgIds = update.messageIds.intersect(msgIdsForDelete)
//      val dbMsgIdsToDelete = update.messageIds.subtract(msgIdsForDelete)
//      if(dbMsgIdsToDelete.isNotEmpty()){
//        val dbMsgIdMap = dbFileList.associateBy { it.messageId }
//        for(id in dbMsgIdsToDelete){
//          dbMsgIdMap[id]?.also {
//            clearMsgData(it)
//            it.uploaded = false
//            it.downloaded = true
//            FileUpdates.addToQueue(it)
//          }
//        }
//      }
//      msgIdsForDelete.removeAll(deletedMsgIds)
//      if(msgIdsForDelete.isNotEmpty()){
//        Messages.deleteMsgByIds()
//      }
//      FileUpdates.deleteTimerTask = Timer().schedule(3000L) {
//        if(dataTransferInProgress == 0L) FileUpdates.nextDataTransfer()
//      }
//    }
////    debugInfo()
//  }

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

    absPathList
    oldAbsPathList

    dbFileList
    localFileList
    remoteFileList

  }


}