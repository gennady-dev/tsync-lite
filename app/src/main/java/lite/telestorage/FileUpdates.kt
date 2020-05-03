package lite.telestorage

import android.util.Log
//import lite.telestorage.kt.database.FileHelper
import lite.telestorage.models.FileData
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.concurrent.schedule

object FileUpdates {

  val queue: MutableList<FileData> = mutableListOf()
  val deleteFile: MutableList<FileData> = mutableListOf()
  var deleteTimerTask: TimerTask? = null
  var syncDelayTask: TimerTask? = null

  fun uploadingUpdate(file: FileData){
    Data.current.also { current ->
      if(current != null){
        if(
          file.uploading
          && file.messageId != 0L
          && file.fileId != 0
          && file.chatId != 0L
          && Settings.chatId == file.chatId
          && current.path == file.path
          && current.size == file.size
        ){
          if(file.uploaded){
            current.uploaded = true
            current.chatId = file.chatId
            current.fileId = file.fileId
            current.messageId = file.messageId
            current.fileUniqueId = file.fileUniqueId
            current.date = file.date
            current.editDate = file.editDate
            Data.remoteFileList.add(current)
            deleteUploaded(current)
            Data.dataTransferInProgress = 0
            nextDataTransfer()
          } else {
            current.uploading = true
            current.fileId = file.fileId
            Data.dataTransferInProgress = Date().time
          }
        }
      } else if(Data.dataTransferInProgress == 0L) nextDataTransfer()
    }
    if(Data.stop){
      Data.current?.fileId?.also {
        if(it != 0){
          TdApi.CancelUploadFile(it)
          Data.current = null
        }
      }
      Data.current?.also {
        TdApi.CancelUploadFile()
        Data.current = null
      }
      nextDataTransfer()
    }
  }

  private fun deleteUploaded(file: FileData){
    if(Settings.deleteUploaded){
      file.path?.also {
        Fs.getAbsPath(it)?.also { path ->
          val f = File(path)
          if(f.exists()) f.delete()
        }
      }
    }
  }

  fun downloadingUpdate(tgFile: TdApi.File){
    var next = false
    val update = FileUpdate(tgFile)
    val current = Data.current
    if(current != null){
      val fileId = update.fileId
      val fileUniqueId = update.fileUniqueId
      val path = current.path
      if(
        update.downloading
        && fileId != 0
        && current.fileId == fileId
        && fileUniqueId != null
        && current.fileUniqueId == fileUniqueId
        && path != null
      ){
        if(update.downloaded){
          val downloadedFile = update.localPath?.let { File(it) }
          val localFile = Fs.syncDirAbsPath?.let { File("${it}/${current.path}") }
          if(downloadedFile?.exists() == true && localFile != null){
            if(Fs.dirExist(path)){
              try {
                if(localFile.exists()) {
                  if(localFile.length() != downloadedFile.length()) localFile.delete()
                }
                if(!localFile.exists()) {
                  Fs.move(downloadedFile, localFile)
                } else downloadedFile.delete()
                current.downloaded = true
                current.lastModified = localFile.lastModified()
                current.size = localFile.length().toInt()
//                FileHelper.updateFile(file)
//                if(Data.dbFileList.find { f -> f == file } == null){
//                  Data.dbFileList.add(file)
//                }
//                cur.updated = false
                next = true
//                Data.dbFileList.find { l -> l.path == path }?.also { Data.remoteFileList.remove(it) }
//                Data.localFileList.find { l -> l.path == path }?.also { Data.localFileList.remove(it) }
//                Data.dbFileList.add(cur)
//                Data.localFileList.add(cur)
                Data.dataTransferInProgress = 0
                nextDataTransfer()
              } catch(e: IOException) {
                Log.d("update", "IOException $e")
              }
            }
          }
        } else {
          current.downloading = true
          Data.dataTransferInProgress = Date().time
        }
      }
    } else if(Data.dataTransferInProgress == 0L) nextDataTransfer(next)
    if(Data.stop){
      Data.current?.fileId?.also {
        if(it != 0){
          TdApi.CancelDownloadFile(it, false)
          Data.current = null
        }
      }
      Data.current?.also {
        TdApi.CancelDownloadFile()
        Data.current = null
      }
      nextDataTransfer()
    }
  }

  fun newMessageHandler(file: FileData){
    if(Settings.chatId != 0L && file.chatId == Settings.chatId){
      // если несколько устройств, отключаем текущее, если получено, что уже загружено с другого
//      Data.current?.also {
//        if(
//          Settings.uploadMissing
//          && !file.upload // не текущий выгружаемый файл
//          && it.path == file.path
//          && !it.uploaded && file.uploaded
//          && it.size == file.size // похоже это тот же файл выгружается с другого устройства
//        ){
//          Settings.uploadMissing = false
//          Settings.save()
//        }
//      }
      Data.addFromMsg(file)
//      if(!file.upload) {
//        Data.addFromMsg(file)
//      }
      syncDelayTask?.cancel()
      syncDelayTask = Timer().schedule(10000L) {
        syncDelayTask = null
        if(Data.dataTransferInProgress == 0L) nextDataTransfer()
      }

    }
  }

  fun nextDataTransfer(next: Boolean = true) {
    if(Sync.ready && !Data.stop){
      if(next) Data.current = Data.fileQueue.firstOrNull()?.also { Data.fileQueue.remove(it) }
      Data.current.also { current ->
        if(current == null) {
          Data.dataTransferInProgress = 0
          Messages.deleteMsgByIds()
          if(syncDelayTask == null){
//            Data.deletedMsgIds.clear()
//            Data.remoteFileList.clear()
//            Data.localFileList.clear()
//            Sync.syncFiles()
//            Messages.getMessages()
            when {
              Data.remoteFileList.isNotEmpty() -> {
                //          syncRemote()
                //          nextDataTransfer()
                Data.deletedMsgIds.clear()
                Data.remoteFileList.clear()
                Data.localFileList.clear()
                Sync.syncFiles()
                Messages.getMessages()
              }
              Data.localFileList.isNotEmpty() -> {
                Data.deletedMsgIds.clear()
                Data.remoteFileList.clear()
                Data.localFileList.clear()
                Sync.syncFiles()
                Messages.getMessages()
              }
              Data.deletedMsgIds.isNotEmpty() -> {
                Data.deletedMsgIds.clear()
                Data.remoteFileList.clear()
                Data.localFileList.clear()
                Sync.syncFiles()
                Messages.getMessages()
              }
            }
            val debug = true
          }
        } else {
          if(!current.uploaded){
            if(Settings.uploadMissing) Tg.uploadFile(current)
            else nextDataTransfer()
          } else if(!current.downloaded) {
            if(Settings.downloadMissing) Tg.downloadFile(current)
            else nextDataTransfer()
          }
        }
      }
    } else if(Data.stop){
      Data.dataTransferInProgress = 0
      Data.stop = false
      Data.fileQueue.clear()
      Data.current = null
    }
  }

  fun uploadFile(file: FileData){
    Tg.uploadFile(file)
  }

  fun downloadFile(file: FileData){
    if(Settings.downloadMissing){
      Tg.downloadFile(file)
    } else {
      file.downloaded = true
//      FileHelper.updateFile(file)
      nextDataTransfer()
    }
  }

  fun syncAll(){
    Data.deleteMsgDuplicates()

    val remotePathMap = Data.remoteFileList.groupBy { it.path }
    val remotePathList = remotePathMap.map { it.key }

    val localPathMap = Data.localFileList.associateBy { it.path }
    val localPathList = localPathMap.map { it.key }

    val newRemote = remotePathList.subtract(localPathList)
    val newLocal = localPathList.subtract(remotePathList)
    val shared = localPathList.intersect(remotePathList)

    for(path in shared) {
      localPathMap[path]?.also { local ->
        remotePathMap[path].also { remote ->
          if(remote == null){
            local.uploaded = false
            local.downloaded = true
            if(Settings.uploadMissing) Data.fileQueue.add(local)
          } else {
            remote.maxBy { it.messageId }?.also {
              if(it.size != local.size){
                if(it.date > local.lastModified){
                  it.downloaded = false
                  it.uploaded = true
                  if(Settings.downloadMissing) Data.fileQueue.add(it)
                } else {
                  local.downloaded = true
                  local.uploaded = false
                  if(Settings.uploadMissing) Data.fileQueue.add(local)
                }
              }
            }
          }
        }
      }
    }

    for(path in newRemote) {
      remotePathMap[path]?.maxBy { it.messageId }?.also { remote ->
        remote.downloaded = false
        remote.uploaded = true
        if(Settings.downloadMissing) Data.fileQueue.add(remote)
      }
    }

    for(path in newLocal) {
      localPathMap[path]?.also { local ->
        local.uploaded = false
        local.downloaded = true
        if(Settings.uploadMissing) Data.fileQueue.add(local)
      }
    }

    Data.fileQueue.sortBy { it.lastModified }

    val debug = null
    Data.remoteFileList.clear()
    Data.localFileList.clear()
  }


  fun syncRemote(){
    //TODO
    // проверить на дубли при итерации по списку из базы
//    val dbPathMap = Data.dbFileList.associateBy { it.path }
    Data.deleteMsgDuplicates()

//    if(Data.remoteToDelete.isNotEmpty()){
//      val ids = Data.remoteToDelete.map { it.messageId }
//      if(ids.isNotEmpty()){
//        Data.msgIdsForDelete.clear()
//        Data.msgIdsForDelete.addAll(ids.toMutableList())
//        Messages.deleteMsgByIds(ids.toLongArray())
//      }
//    }

    Data.remoteFileList.forEach {
      it.path?.also { path ->
        val file = Fs.getAbsPath(path)?.let { abs -> File(abs) }
        if(file != null && (!file.exists() || file.length().toInt() != it.size)) {
          it.downloaded = false
          it.uploaded = true
          Data.fileQueue.add(it)
        }
      }
    }
//      val path = it.path
//      var file: FileData? = null
//      file = dbPathMap[path]?.also { db ->
//        if(db.fileId == it.fileId){
//          if(db.messageId != it.messageId){
//            if(db.lastDate < it.lastDate) {
//              if(db.messageId != 0L && db.messageId != it.messageId) Data.msgIdsForDelete.add(db.messageId)
//              db.messageId = it.messageId
//              db.date = it.date
//              db.editDate = it.editDate
//            } else if(db.lastDate > it.lastDate) {
//              Data.msgIdsForDelete.add(it.messageId)
//            }
//          }
//          db.uploaded = true
//        } else if(db.size == it.size) {
//          if(db.messageId != it.messageId) {
//            if(db.lastDate < it.lastDate) {
//              if(db.messageId != 0L) Data.msgIdsForDelete.add(db.messageId)
//              db.messageId = it.messageId
//              db.fileUniqueId = it.fileUniqueId
//              db.fileId = it.fileId
//              db.date = it.date
//              db.editDate = it.editDate
//            } else if(db.lastDate > it.lastDate) {
//              Data.msgIdsForDelete.add(it.messageId)
//            }
//          }
//          db.uploaded = true
//        } else {
//          if(db.lastDate != 0L) {
//            if(db.lastDate < it.lastDate) {
//              if(db.messageId != 0L) Data.msgIdsForDelete.add(db.messageId)
//              db.messageId = it.messageId
//              db.fileUniqueId = it.fileUniqueId
//              db.fileId = it.fileId
//              db.date = it.date
//              db.editDate = it.editDate
//              db.size = it.size
//              db.uploaded = true
//              db.downloaded = false
//            } else {
//              db.uploaded = false
//              db.downloaded = true
//              Data.msgIdsForDelete.add(it.messageId)
//            }
//          } else {
//            if(db.lastModified > it.lastDate) {
//              if(db.messageId != 0L) {
//                db.uploaded = false
//                db.downloaded = true
//                Data.msgIdsForDelete.add(it.messageId)
//              } else {
//                db.messageId = it.messageId
//                db.fileUniqueId = it.fileUniqueId
//                db.fileId = it.fileId
//                db.date = it.date
//                db.editDate = it.editDate
//                db.uploaded = false
//                db.downloaded = true
//              }
//            }
//          }
//        }
//      } ?: it.also { f ->
//        f.uploaded = true
//        f.downloaded = false
//      }
//      addToQueue(file)
//    }
    Data.remoteFileList.clear()
  }

  fun syncLocal(){
    //TODO
  }

}
