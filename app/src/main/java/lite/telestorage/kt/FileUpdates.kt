package lite.telestorage.kt

import android.util.Log
import lite.telestorage.kt.database.FileHelper
import lite.telestorage.kt.models.FileData
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File
import java.io.IOException

object FileUpdates {

  val queue: MutableList<FileData> = mutableListOf()
  val deleteFile: MutableList<FileData> = mutableListOf()
  var current: FileData? = null

  fun uploadingUpdate(file: FileData){
    synchronized(Data){
      var next = false
      Data.current?.also {
        if(
          file.upload
          && file.uploaded
          && file.messageId != 0L
          && file.fileId != 0
          && file.chatId != 0L
          && Settings.chatId == file.chatId
          && it.path == file.path
          && it.size == file.size
        ){
          it.uploaded = true
          it.chatId = file.chatId
          it.fileId = file.fileId
          it.messageId = file.messageId
          it.fileUniqueId = file.fileUniqueId
          it.date = file.date
          it.editDate = file.editDate
          FileHelper.updateFile(it)
          it.updated = false
          Data.dataTransferInProgress = 0
          next = true
          Data.remoteFileList.add(it)
          deleteUploaded(it)
        }
      }
      nextDataTransfer(next)
    }
  }

  fun deleteUploaded(file: FileData){
    if(Settings.deleteUploaded){
      file.path?.also {
        Fs.getAbsPath(it)?.also { path ->
          val f = File(path)
          if(f.exists()) f.delete()
        }
      }
    }
  }

  fun downloadingUpdate(file: TdApi.File){
    var next = false
    val update = FileUpdate(file)
    Data.current?.also { cur ->
      synchronized(cur){
        val fileId = update.fileId
        val fileUniqueId = update.fileUniqueId
        val path = cur.path
        if(
          update.download
          && update.downloaded
          && fileId != 0
          && cur.fileId == fileId
          && fileUniqueId != null
          && cur.fileUniqueId == fileUniqueId
          && path != null
        ){
          val downloadedFile = update.localPath?.let { File(it) }
          val localFile = Fs.syncDirAbsPath?.let { File("${it}/${cur.path}") }
          if(downloadedFile?.exists() == true && localFile?.exists() == false){
            if(Fs.dirExist(path)){
              try {
                Fs.move(downloadedFile, localFile)
                cur.downloaded = true
                cur.inProgress = false
                FileHelper.updateFile(cur)
                cur.updated = false
                Data.dataTransferInProgress = 0
                next = true
                Data.dbFileList.add(cur)
                Data.localFileList.add(cur)
              } catch(e: IOException) {
                Log.d("update", "IOException $e")
              }
            }
          }
        }
        nextDataTransfer(next)
      }
    }
  }

  fun newMessageHandler(file: FileData){
    if(Settings.chatId != 0L && file.chatId == Settings.chatId){
      var handled = false
      // если несколько устройств, отключаем текущее, если получено, что уже загружено с другого
      Data.current?.also {
        if(
          Settings.uploadMissing
          && !file.upload // не текущий выгружаемый файл
          && it.path == file.path
          && !it.uploaded && file.uploaded
          && it.size == file.size
        ){
          Settings.uploadMissing = false
          Settings.save()
          handled = true
          Data.dataTransferInProgress = 0
        }
      }
      if(!handled) {
        Data.addFromMsg(file)
        syncQueue(SyncType.NEW_REMOTE)
        if(Data.dataTransferInProgress == 0L) nextDataTransfer()
      }

//      if(!handled && file.uploaded && file.path != null) {
//        val dbFile = file.path?.let { Data.dbPathMap[it] ?: Data.localFilePathMap[it] }
//        if(dbFile == null) {
//          file.downloaded = Data.isDownloaded(file)
//          Data.addForTransfer(file)
//        } else if(dbFile.path == file.path) {
//          val dbDate = if(dbFile.editDate != 0L) dbFile.editDate else dbFile.date
//          val newDate = if(file.editDate != 0L) file.editDate else file.date
//          if(file.size != dbFile.size){
//            if(newDate > dbDate){
//              Data.dbMsgIdMap.remove(dbFile.messageId)
//              dbFile.downloaded = !Settings.downloadMissing
//              dbFile.messageId = file.messageId
//              dbFile.fileId = file.fileId
//              dbFile.fileUniqueId = file.fileUniqueId
//              dbFile.size = file.size
//              dbFile.date = file.date
//              dbFile.editDate = file.editDate
//              Data.dbMsgIdMap[file.messageId] = dbFile
//              Data.addForTransfer(dbFile)
//            } else {
//              Data.addForRemove(file)
//            }
//          } else {
//            if(newDate > dbDate){
//              Data.dbMsgIdMap.remove(dbFile.messageId)
//              dbFile.downloaded = Data.isDownloaded(dbFile.path)
//              dbFile.messageId = file.messageId
//              dbFile.fileId = file.fileId
//              dbFile.fileUniqueId = file.fileUniqueId
//              dbFile.size = file.size
//              dbFile.date = file.date
//              dbFile.editDate = file.editDate
//              Data.dbMsgIdMap[file.messageId] = dbFile
//              Data.addForTransfer(dbFile)
//            } else {
//              Data.addForRemove(file)
//            }
//          }
//
//        }
//      }
    }
  }

  fun nextDataTransfer(next: Boolean = true){
      if(next) Data.current = null
      var currentFile: FileData? = Data.current // ?: if(Data.forTransfer.isEmpty()) null else Data.forTransfer.firstOrNull()  removeAt(0)
      if(currentFile == null){
        currentFile = Data.fileQueue.firstOrNull()?.also { Data.fileQueue.remove(it) }
//        currentFile?.also {
//          Data.removeTransfer(it)
//        }
      }
      if(currentFile == null){
        Data.dataTransferInProgress = 0
//        Data.dbFileList.forEach { it.updated = false }
//        if(Data.msgIdsForDelete.isNotEmpty()){
//          Messages.deleteMsgByIds(Data.msgIdsForDelete)
//        }
      } else {
        Data.current = currentFile
        if(!currentFile.uploaded) uploadFile(currentFile)
        else if(!currentFile.downloaded) downloadFile(currentFile)
        else {
          FileHelper.updateFile(currentFile)// в процессе синхронизации дописались отсутствующие данные в файл
          currentFile.updated = false
          nextDataTransfer()
        }
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
      FileHelper.updateFile(file)
      nextDataTransfer()
    }
  }

  fun syncQueue(type: SyncType = SyncType.ON_START){
    //db, local, remote
    when(type){
      SyncType.ON_START -> {
        val dbPathMap = Data.dbFileList.associateBy { it.path }
        val dbPathList = dbPathMap.map { it.key }
        val remotePathMap = Data.remoteFileList.associateBy { it.path }
        val remotePathList = remotePathMap.map { it.key }
        val localPathMap = Data.localFileList.associateBy { it.path }
        val localPathList = localPathMap.map { it.key }
        val newRemote = remotePathList.subtract(localPathList)
        val newLocal = localPathList.subtract(remotePathList)
        val debug = null
        for(path in newRemote) {
          remotePathMap[path]?.also {
            it.uploaded = true
            it.downloaded = false
            Data.fileQueue.add(it)
          }
        }
        for(path in newLocal) {
          localPathMap[path]?.also {
            it.uploaded = false
            it.downloaded = true
            Data.clearMsgData(it)
            Data.fileQueue.add(it)
          }
        }
      }
      SyncType.NEW_REMOTE -> {
        val dbPathMap = Data.dbFileList.associateBy { it.path }

        Data.remoteFileList.groupBy { it.path }.also {
          for(list in it.values){
            list.maxBy { msg -> msg.messageId }?.also { f ->
              list.minus(f).also { l ->
                Data.remoteFileList.removeAll(l)
                Data.remoteToDelete.addAll(l)
              }
            }
          }
        }

        Data.remoteFileList.filter { it.updated }.forEach { file ->
          val path = file.path
          if(path != null){
            val dbFile = dbPathMap[path]
            if(dbFile == null){
              file.downloaded = !Settings.downloadMissing
              Data.fileQueue.add(file)
            } else {
              if(file.messageId > dbFile.messageId){
                if(file.size != dbFile.size){
                  dbFile.downloaded = !Settings.downloadMissing
                }
                Data.copyFile(file, dbFile)
                Data.fileQueue.add(dbFile)
                file.updated = false
              } else if(file.messageId == dbFile.messageId){
                val localDate = if(dbFile.editDate == 0L) dbFile.date else dbFile.editDate
                val remoteDate = if(file.editDate == 0L) file.date else file.editDate
                if(file.size != dbFile.size){
                  if(localDate < remoteDate){
                    dbFile.downloaded = !Settings.downloadMissing
                    Data.copyFile(file, dbFile)
                    Data.fileQueue.add(dbFile)
                  }
                  file.updated = false
                }
              } else {
                Data.remoteFileList.remove(file)
                Data.remoteToDelete.add(file)
              }
            }
          } else {
            Data.remoteFileList.remove(file)
            Data.remoteToDelete.add(file)
          }
        }

      }

//        for(file in Data.dbFileList){
//          val path = file.path
//          if(path != null) {
//            if(Data.localFilePathMap[path] != null && Data.remoteFilePathMap[path] == null) {
//              file.messageId = 0
//              file.fileId = 0
//              file.fileUniqueId = null
//              file.uploaded = false
//              file.date = 0
//              file.editDate = 0
//              Data.addForTransfer(file)
//            }
//            if(Data.localFilePathMap[path] == null && Data.remoteFilePathMap[path] == null) {
//              Data.remoteToDelete.add(file)
//            }
//          } else Data.remoteToDelete.add(file)
//        }
//      }
//      SyncType.NEW_REMOTE -> {
//        val newFile = Data.remoteFileList.subtract(Data.dbFileList)
//        for(file in newFile){
//          file.uploaded = true
//          file.downloaded = Data.isDownloaded(file.path)
//          Data.addForTransfer(file)
//        }
//      }
//      SyncType.NEW_LOCAL -> {
//        for(file in Data.newLocalFileList){
//          val path = file.path
//          if(path != null) {
//            if(Data.dbPathMap[path] == null) {
//              file.uploaded = false
//              Data.addForTransfer(file)
//            }
//          }
//        }
//      }
//      SyncType.REMOVED_REMOTE -> {
//        for(id in Data.deletedMsgIds) {
//          (Data.dbMsgIdMap[id] ?: Data.localFileMsgIdMap[id])?.also {
//            it.messageId = 0
//            it.fileId = 0
//            it.fileUniqueId = null
//            it.uploaded = !Settings.uploadMissing
//            it.date = 0
//            it.editDate = 0
//            Data.addForTransfer(it)
//          }
//        }
//        Data.deletedMsgIds.clear()
//      }
//      SyncType.REMOVED_LOCAL -> {
//        val removed = Data.dbFileList.subtract(Data.localFileList)
//        for(file in removed){
//          val path = file.path
//          if(path != null && Data.remoteFilePathMap[path] != null) {
//            file.downloaded = Data.isDownloaded(path)
//            file.uploaded = true
//            Data.addForTransfer(file)
//          }
//        }
//      }
    }
//    Data.debugInfo()
  }

}

enum class SyncType {
  ON_START, NEW_LOCAL, NEW_REMOTE, REMOVED_LOCAL, REMOVED_REMOTE
}