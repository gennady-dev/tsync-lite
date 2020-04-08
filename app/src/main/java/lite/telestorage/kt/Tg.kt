package lite.telestorage.kt

import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.widget.Switch
import android.widget.TextView
import lite.telestorage.kt.database.FileHelper
import lite.telestorage.kt.database.SettingsHelper
import lite.telestorage.kt.models.FileData
import lite.telestorage.kt.models.Settings
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.Client.ResultHandler
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


class Tg private constructor() {

  private fun createClient() {
    if(client == null) {
      updateHandler = UpdateHandler()
      client = Client.create(
        updateHandler, UpdateExceptionHandler(), DefaultExceptionHandler()
      )
    }
  }

  fun logout() {
    if(updateHandler != null) {
      client?.send(TdApi.LogOut(), updateHandler)
    }
  }

  var settingsFragment: SettingsFragment? = null
  var groupNameTextView: TextView? = null
  var mSwitchSync: Switch? = null
//  var mainActivity: MainActivity? = null
  var mLimit = 100
  var mI = 0
  private var TAG: String? = null
  var client: Client? = null
  var updateHandler: UpdateHandler? = null
  private var authorizationRequestHandler = AuthorizationRequestHandler()
  var settings: Settings? = null
//  var settingsHelper: SettingsHelper? = null
//  var fileHelper: FileHelper? = null
  var waitingCreatedChat = false
  var waitingSuperGroupResponse = false
  var memberStatusBanned = false
  var defaultGroupName: String? = null
  var enteredGroupName: String? = null
  var uploadingInProgress: Long = 0
  private val uploadingLock = Any()
  private val downloadingLock = Any()
  var authorizationState: TdApi.AuthorizationState? = null

  private fun onAuthorizationStateUpdated(state: TdApi.AuthorizationState?) {

    state?.let { authorizationState = it }

    when(authorizationState?.constructor) {

      TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
        Log.d(null, "AuthorizationStateWaitTdlibParameters")
        haveAuthorization = false
        val parameters = TdApi.TdlibParameters()
        // parameters.useTestDc = true;
        parameters.databaseDirectory = tdLibPath
        parameters.useFileDatabase = false
        parameters.useMessageDatabase = true
        parameters.useSecretChats = true
        parameters.apiId = BuildConfig.TG_APP_ID
        parameters.apiHash = BuildConfig.TG_API_HASH
        parameters.systemLanguageCode = "en"
        parameters.systemVersion = "SDK " + Build.VERSION.SDK_INT
        parameters.applicationVersion = "0.01"
        parameters.enableStorageOptimizer = true
        try {
          // systemLangCode = LocaleController.getSystemLocaleStringIso639().toLowerCase();
          // langCode = LocaleController.getLocaleStringIso639().toLowerCase();
          parameters.deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        } catch(e: Exception) {
          parameters.deviceModel = "Android unknown"
        }
        if(parameters.deviceModel.isEmpty() || parameters.deviceModel.isBlank()) {
          parameters.deviceModel = "Android unknown"
        }
        if(parameters.systemVersion.isEmpty() || parameters.systemVersion.isBlank()) {
          parameters.systemVersion = "SDK Unknown"
        }
        client?.send(TdApi.SetTdlibParameters(parameters), authorizationRequestHandler)
      }
      TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR -> {
        Log.d(null, "AuthorizationStateWaitEncryptionKey")
        haveAuthorization = false
        client?.send(TdApi.CheckDatabaseEncryptionKey(), authorizationRequestHandler)
      }
      TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
        Log.d(null, "AuthorizationStateWaitPhoneNumber")
        haveAuthorization = false
        settingsFragment?.setLogged(false)
      }
      TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
        Log.d(null, "AuthorizationStateWaitCode")
        haveAuthorization = false
        settingsFragment?.showCodeDialog()
      }
      TdApi.AuthorizationStateWaitRegistration.CONSTRUCTOR -> {
        Log.d(null, "AuthorizationStateWaitRegistration")
        haveAuthorization = false
      }
      TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> {
        Log.d(null, "AuthorizationStateWaitPassword")
        haveAuthorization = false
      }
      TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
        Log.d(null, "AuthorizationStateReady")
        haveAuthorization = true
        settingsFragment?.setLogged(true)
        settings = settingsHelper?.settings
        settings?.authenticated = true
        settings?.let { settingsHelper?.updateSettings(it) }
        Sync.start()
      }
      TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR -> {
        Log.d(null, "AuthorizationStateLoggingOut")
        haveAuthorization = false
        Log.d(TAG, "Logging out")
      }
      TdApi.AuthorizationStateClosing.CONSTRUCTOR -> {
        Log.d(null, "AuthorizationStateClosing")
        haveAuthorization = false
        Log.d(TAG, "Closing")
      }
      TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
        Log.d(null, "AuthorizationStateClosed")
        haveAuthorization = false
        Log.d(TAG, "Closed")
        if(settingsFragment != null) {
          settingsFragment?.setLogged(false)
          instance = null
        }
      }
      else -> Log.d(null, "Unsupported authorization state $state")
    }
  }

  fun createNewSupergroup(name: String) {
    if(name.isNotBlank()) {
      enteredGroupName = name.trim()
      client?.send(
        TdApi.CreateNewSupergroupChat(name.trim(), true, "My group", null),
        updateHandler
      )
      waitingCreatedChat = true
    }
  }

  fun sendPhone(phone: String) {
    client?.send(TdApi.SetAuthenticationPhoneNumber(phone, null), authorizationRequestHandler)
  }

  fun sendCode(code: String) {
    client?.send(TdApi.CheckAuthenticationCode(code), authorizationRequestHandler)
  }

  fun downloadNextFile() {
    Sync.updateDataTransferProgressStatus()
    val settings = settingsHelper?.settings
    if(Sync.dataTransferInProgress == 0L && settings != null && settings.downloadMissing) {
      val nextFile: FileData? = fileHelper?.nextDownloadingFile
      if(nextFile != null && nextFile.id != 0 && nextFile.uniqueId != null) {
        nextFile.inProgress = true
        fileHelper?.updateFile(nextFile)
        Sync.dataTransferInProgress = Date().time
        Sync.updateProgressStatus()
        client?.send(TdApi.DownloadFile(nextFile.id, 32, 0, 0, true), updateHandler)
      }
    }
    Sync.updateProgressStatus()
  }

  fun dataTransferUpdateHandler(file: TdApi.File?) {
    var completed = true
    if(file != null) {
      if(file.local != null) {
        if(file.local.isDownloadingActive && !file.local.isDownloadingCompleted) {
          completed = false
        }
      }
      if(file.remote != null) {
        if(file.remote.isUploadingActive && !file.remote.isUploadingCompleted) {
          completed = false
        }
      }
    }
    if(completed) {
      Sync.dataTransferInProgress = 0
      Sync.nextDataTransfer()
    }
  }

  fun downloadingUpdate(file: TdApi.File) {
    if(file.local != null && file.local.isDownloadingActive && !file.local.isDownloadingCompleted) {
      Sync.dataTransferInProgress = Date().time
      Sync.updateProgressStatus()
      System.out.println("downloadingUpdate Sync.dataTransferInProgress " + Sync.dataTransferInProgress)
    }
    if(
      file.local != null
      && !file.local.isDownloadingActive
      && file.local.isDownloadingCompleted
      && file.local.path != null
      && file.local.path.matches("$tdLibPath.+".toRegex())
      && file.id != 0
      && file.remote != null
      && file.remote.uniqueId != null
      && file.remote.uniqueId != ""
      && fileHelper != null
    ) {
      val localFiles: List<FileData> = fileHelper?.getFiles(
        "file_id = ? AND file_unique_id = ? AND downloaded = 0",
        arrayOf(file.id.toString(), file.remote.uniqueId)
      ) ?: ArrayList<FileData>()
      if(localFiles.size == 1) {
        val fileData: FileData = localFiles[0]
        if(fileData.path != null) {
          val pathParts: Array<String> = fileData.path?.let { it.split("/").toTypedArray() } ?: arrayOf()
          val settings = settingsHelper?.settings
          val downloadedFile = File(file.local.path)
          if(settings != null) {
            val localFile = fileData.path?.let {path ->
              Fs.getFileAbsPath(path)?.let {File(it)}
            }
            if(localFile != null && !localFile.exists() && downloadedFile.exists()) {
              if(pathParts.size == 1 && pathParts[0] != "") {
                try {
                  Fs.move(downloadedFile, localFile)
                } catch(e: IOException) {
                  println("DownloadingHandler IOException $e")
                }
              } else if(pathParts.size > 1 && Fs.storageAbsPath != null) {
                val relativePath = TextUtils.join(
                  "/", pathParts.copyOfRange(0, pathParts.size - 1)
                )
                val pathDirectory: File =
                  java.io.File(Fs.storageAbsPath + "/" + relativePath)
                var directoryExist: Boolean
                if(pathDirectory.exists()) {
                  directoryExist = true
                  if(!pathDirectory.isDirectory) {
                    directoryExist = false
                  }
                } else {
                  directoryExist = pathDirectory.mkdirs()
                }
                if(directoryExist) {
                  try {
                    Fs.move(downloadedFile, localFile)
                  } catch(e: IOException) {
                    println("DownloadingHandler IOException $e")
                  }
                }
              }
            }
            if(localFile != null && localFile.exists()) {
              fileData.downloaded = true
              fileData.lastModified = localFile.lastModified()
              fileData.inProgress = false
              fileHelper?.updateFile(fileData)
            }
          }
        }
      }
      Sync.dataTransferInProgress = 0
      Sync.nextDataTransfer()
    }
    //        downloadNextFile();
  }

  fun uploadingUpdate(file: TdApi.File?) {
    if(file?.remote != null && file.remote.isUploadingActive && !file.remote.isUploadingCompleted) {
      Sync.dataTransferInProgress = Date().time
      Sync.updateProgressStatus()
      System.out.println("uploadingUpdate Sync.dataTransferInProgress " + Sync.dataTransferInProgress)
    }
  }

  fun uploadNextFile() {
    for(ste in Thread.currentThread().stackTrace) {
      Log.d(null, ste.toString())
    }
    Sync.updateDataTransferProgressStatus()
    Log.d(null, "dataTransferInProgress ${Sync.dataTransferInProgress}")
    val settings: Settings? = settingsHelper?.settings
    val fileHelper: FileHelper? = fileHelper
    if(Sync.dataTransferInProgress == 0L && fileHelper != null && settings != null && settings.chatId != 0L) {
      val nextFileData: FileData? = fileHelper.nextFile
      if(nextFileData != null) {
        Sync.dataTransferInProgress = Date().time
        Sync.updateProgressStatus()
        nextFileData.inProgress = true
        fileHelper.updateFile(nextFileData)
        if(settings.uploadAsMedia) {
          val mimeType: String? = nextFileData.mimeType
          if(mimeType != null) {
            if(mimeType.matches(".*video.*".toRegex())) {
              sendVideo(nextFileData)
            } else if(mimeType.matches(".*audio.*".toRegex())) {
              sendAudio(nextFileData)
            } else if(mimeType.matches(".*image.*".toRegex())) {
              if(mimeType.matches("(?i)image/gif".toRegex()) || mimeType.matches("(?i)image/webp".toRegex())) {
                sendDocument(nextFileData)
              } else {
                sendPhoto(nextFileData)
              }
            } else {
              sendDocument(nextFileData)
            }
          }
        } else {
          sendDocument(nextFileData)
        }
      }
    }
  }

  private fun sendDocument(fileData: FileData) {
    //📂📂 "\uD83D\uDCC2/"
    val relativePath: String? = fileData.path
    val absPath: String? = relativePath?.let { Fs.getFileAbsPath(it) }
    val settings: Settings? = settingsHelper?.settings
    val f = absPath?.let { java.io.File(it) }
    if(settings !== null) {
      if(f != null && f.exists() && f.isFile && f.length() > 0) {
        val document: TdApi.InputMessageContent = TdApi.InputMessageDocument(
          TdApi.InputFileLocal(absPath), null,
          TdApi.FormattedText("\uD83D\uDDC0/$relativePath", null)
        )
        if(fileData.messageId == 0L) {
          client?.send(TdApi.SendMessage(settings.chatId, 0, null, null, document), updateHandler)
        } else {
          client?.send(
            TdApi.EditMessageMedia(
              settings.chatId, fileData.messageId, null, document
            ), updateHandler
          )
        }
      } else {
        fileHelper?.delete(fileData)
        Sync.dataTransferInProgress = 0
        Sync.nextDataTransfer()
      }
    }
  }

  private fun sendVideo(fileData: FileData) {
    val relativePath: String? = fileData.path
    val absPath: String? = relativePath?.let { Fs.getFileAbsPath(it) }
    val settings: Settings? = settingsHelper?.settings
    val f = absPath?.let { java.io.File(it) }
    if(settings !== null) {
      if(f != null && f.exists() && f.isFile && f.length() > 0) {
        val video: TdApi.InputMessageContent = TdApi.InputMessageVideo(
          TdApi.InputFileLocal(absPath),
          null,
          null,
          0,
          0,
          0,
          true,
          TdApi.FormattedText("\uD83D\uDDC0/$relativePath", null),
          0
        )
        if(fileData.messageId == 0L) {
          client?.send(TdApi.SendMessage(settings.chatId, 0, null, null, video), updateHandler)
        } else {
          client?.send(
            TdApi.EditMessageMedia(settings.chatId, fileData.messageId, null, video),
            updateHandler
          )
        }
      } else {
        fileHelper?.delete(fileData)
        Sync.dataTransferInProgress = 0
        Sync.nextDataTransfer()
      }
    }
  }

  private fun sendAudio(fileData: FileData) {
    val relativePath: String? = fileData.path
    val absPath: String? = relativePath?.let { Fs.getFileAbsPath(it) }
    val settings: Settings? = settingsHelper?.settings
    val f = absPath?.let { java.io.File(it) }
    if(settings !== null) {
      if(f != null && f.exists() && f.isFile && f.length() > 0) {
        val audio: TdApi.InputMessageContent = TdApi.InputMessageAudio(
          TdApi.InputFileLocal(absPath),
          null,
          0,
          null,
          null, TdApi.FormattedText("\uD83D\uDDC0/$relativePath", null)
        )
        if(fileData.messageId == 0L) {
          client?.send(TdApi.SendMessage(settings.chatId, 0, null, null, audio), updateHandler)
        } else {
          client?.send(
            TdApi.EditMessageMedia(settings.chatId, fileData.messageId, null, audio),
            updateHandler
          )
        }
      } else {
        fileHelper?.delete(fileData)
        Sync.dataTransferInProgress = 0
        Sync.nextDataTransfer()
      }
    }
  }

  private fun sendPhoto(fileData: FileData) {
    val relativePath: String? = fileData.path
    val absPath: String? = relativePath?.let { Fs.getFileAbsPath(it) }
    val settings: Settings? = settingsHelper?.settings
    val f = absPath?.let { java.io.File(it) }
    if(settings !== null){
      if(f != null && f.exists() && f.isFile && f.length() > 0) {
        val photo: TdApi.InputMessageContent = TdApi.InputMessagePhoto(
          TdApi.InputFileLocal(absPath), null, null, 0, 0, TdApi.FormattedText("\uD83D\uDDC0/$relativePath", null), 0
        )
        if(fileData.messageId == 0L) {
          client!!.send(TdApi.SendMessage(settings.chatId, 0, null, null, photo), updateHandler)
        } else {
          client!!.send(
            TdApi.EditMessageMedia(settings.chatId, fileData.messageId, null, photo),
            updateHandler
          )
        }
      } else {
        fileHelper?.delete(fileData)
        Sync.dataTransferInProgress = 0
        Sync.nextDataTransfer()
      }
    }
  }

//  private fun setChatOrder(chat: TdApi.Chat?, order: Long) {
//    synchronized(mainChatList) {
//      synchronized(chat!!) {
//        if(chat.chatList == null || chat.chatList!!.constructor != TdApi.ChatListMain.CONSTRUCTOR) {
//          return
//        }
//        if(chat.order != 0L) {
//          val isRemoved = mainChatList.remove(OrderedChat(chat.order, chat.id))
//          assert(isRemoved)
//        }
//        chat.order = order
//        if(chat.order != 0L) {
//          val isAdded = mainChatList.add(OrderedChat(chat.order, chat.id))
//          assert(isAdded)
//        }
//      }
//    }
//  }

//  private fun setMainChatList(limit: Int) {
//    mLimit = limit
//    synchronized(mainChatList) {
//      if(!haveFullMainChatList && limit > mainChatList.size) {
//        // have enough chats in the chat list or chat list is too small
//        var offsetOrder = Long.MAX_VALUE
//        var offsetChatId: Long = 0
//        if(!mainChatList.isEmpty()) {
//          val last = mainChatList.last()
//          offsetOrder = last.order
//          offsetChatId = last.chatId
//        }
//        //                Tg.UpdatesHandler handler = new Tg.UpdatesHandler();
//        //                handler.handlerId = 5;
//        //                client.send(new TdApi.GetChats(new TdApi.ChatListMain(), offsetOrder, offsetChatId, limit - mainChatList.size()), handler);
//        client!!.send(
//          TdApi.GetChats(
//            TdApi.ChatListMain(), offsetOrder, offsetChatId, limit - mainChatList.size
//          )
//        ) { `object` ->
//          when(`object`.constructor) {
//            TdApi.Error.CONSTRUCTOR -> {
//            }
//            TdApi.Chats.CONSTRUCTOR -> {
//              val chatIds = (`object` as TdApi.Chats).chatIds
//              Log.d("chat array", Arrays.toString(chatIds))
//              if(chatIds.size == 0) {
//                synchronized(
//                  mainChatList
//                ) { haveFullMainChatList = true }
//              } else {
//                // chats had already been received through updates, let's retry request
//                setMainChatList(limit)
//              }
//            }
//            else -> System.err.println("Receive wrong response from TDLib:$newLine$`object`")
//          }
//        }
//      }
//      val iterator: Iterator<OrderedChat> = mainChatList.iterator()
//      println()
//      println("First " + mLimit + " chat(s) out of " + mainChatList.size + " known chat(s):")
//      for(i in mainChatList.indices) {
//        val chatId = iterator.next().chatId
//        val chat = chats[chatId]
//        synchronized(chat!!) {
//          //                    System.out.println(chatId + ": " + chat.title);
//          Log.d("ID $chatId", chat.toString())
//        }
//      }
//      for(i in mainChatList.indices) {
//        val chatId = iterator.next().chatId
//        Log.d("ID", java.lang.Long.toString(chatId))
//      }
//    }
//  }

//  private class OrderedChat internal constructor(val order: Long, val chatId: Long) :
//    Comparable<OrderedChat?> {
//    override operator fun compareTo(o: OrderedChat): Int {
//      if(order != o.order) {
//        return if(o.order < order) -1 else 1
//      }
//      return if(chatId != o.chatId) {
//        if(o.chatId < chatId) -1 else 1
//      } else 0
//    }
//
//    override fun equals(obj: Any?): Boolean {
//      val o = obj as OrderedChat?
//      return order == o!!.order && chatId == o.chatId
//    }
//
//  }

  inner class UpdateHandler : ResultHandler {

    override fun onResult(received: TdApi.Object) {
      if(TAG != null) {
        Log.d(null, "UpdatesHandler $TAG $received")
      } else {
        Log.d(null, "UpdatesHandler $received")
      }
      val fileUpdate: FileUpdate
      when(received.constructor) {
//        TdApi.Chats.CONSTRUCTOR -> {
//          val chatIds = (received as TdApi.Chats).chatIds
//          Log.d("chat array", Arrays.toString(chatIds))
//          if(chatIds.size == 0) {
//            synchronized(mainChatList) { haveFullMainChatList = true }
//            val iter: Iterator<OrderedChat> = mainChatList.iterator()
//            println()
//            var i = 0
//            while(i < mainChatList.size) {
//              val chatId = iter.next().chatId
//              val chat = chats[chatId]
//              synchronized(chat!!) { Log.d("ID $chatId", chat.toString()) }
//              i++
//            }
//          } else {
//            // chats had already been received through updates, let's retry request
//            setMainChatList(mLimit)
//          }
//        }
        TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
          Log.d(null, "UpdatesHandler UpdateAuthorizationState $received")
          onAuthorizationStateUpdated((received as TdApi.UpdateAuthorizationState).authorizationState)
        }
        TdApi.UpdateConnectionState.CONSTRUCTOR -> {
          if((received as TdApi.UpdateConnectionState).state.constructor == TdApi.ConnectionStateReady.CONSTRUCTOR) {
            isConnected = true
            if(needUpdate) {
              Sync.start()
              needUpdate = false
            }
          } else {
            isConnected = false
          }
          Log.d(null, "UpdateConnectionState $received")
        }
        TdApi.UpdateUser.CONSTRUCTOR -> {
          val updateUser = received as TdApi.UpdateUser
          users[updateUser.user.id] = updateUser.user
        }
        TdApi.UpdateUserStatus.CONSTRUCTOR -> {
          val updateUserStatus = received as TdApi.UpdateUserStatus
          val user = users[updateUserStatus.userId]
          synchronized(user!!) { user.status = updateUserStatus.status }
        }
        TdApi.UpdateBasicGroup.CONSTRUCTOR -> {
          val updateBasicGroup = received as TdApi.UpdateBasicGroup
          basicGroups[updateBasicGroup.basicGroup.id] = updateBasicGroup.basicGroup
        }
        TdApi.UpdateSupergroup.CONSTRUCTOR -> {
          val updateSupergroup = received as TdApi.UpdateSupergroup
          superGroups[updateSupergroup.supergroup.id] = updateSupergroup.supergroup
          settings = SettingsHelper.get()?.settings
          if(settings != null
            && updateSupergroup.supergroup.id == settings?.supergroupId
            && (
              updateSupergroup.supergroup.status.constructor == TdApi.ChatMemberStatusBanned.CONSTRUCTOR
              || updateSupergroup.supergroup.status.constructor == TdApi.ChatMemberStatusCreator.CONSTRUCTOR
                && !(updateSupergroup.supergroup.status as TdApi.ChatMemberStatusCreator).isMember
              )
          ) {
            Log.d("UpdateSupergroup", " NOT FOUND $received")
            memberStatusBanned = true
            if(!waitingCreatedChat) {
              syncStatus?.setGroupName(null)
              syncStatus?.setSyncSwitch(false)
            }
            val chatId: Long = settings?.chatId ?: 0
            if(chatId != 0L) {
              fileHelper?.deleteByChatId(chatId)
            }
            settings?.enabled = false
            settings?.chatId = 0
            settings?.supergroupId = 0
            settings?.title = null
            settings?.let { settingsHelper?.updateSettings(it) }
          }
        }
        TdApi.UpdateSecretChat.CONSTRUCTOR -> {
          val updateSecretChat = received as TdApi.UpdateSecretChat
          secretChats[updateSecretChat.secretChat.id] = updateSecretChat.secretChat
        }
//        TdApi.UpdateNewChat.CONSTRUCTOR -> {
//          val updateNewChat = received as TdApi.UpdateNewChat
//          val chat = updateNewChat.chat
//          synchronized(chat) {
//            chats[chat.id] = chat
//            val order = chat.order
//            chat.order = 0
//            setChatOrder(chat, order)
//          }
//        }
        TdApi.UpdateChatTitle.CONSTRUCTOR -> {
          val updateChat = received as TdApi.UpdateChatTitle
          val chat = chats[updateChat.chatId]
          if(chat != null) {
            synchronized(chat) { chat.title = updateChat.title }
          }
        }
        TdApi.UpdateChatPhoto.CONSTRUCTOR -> {
          val updateChat = received as TdApi.UpdateChatPhoto
          val chat = chats[updateChat.chatId]
          synchronized(chat!!) { chat.photo = updateChat.photo }
        }
//        TdApi.UpdateChatChatList.CONSTRUCTOR -> {
//          val updateChat = received as TdApi.UpdateChatChatList
//          val chat = chats[updateChat.chatId]
//          synchronized(
//            mainChatList
//          ) { // to not change Chat.chatList while mainChatList is locked
//            synchronized(chat!!) {
//              assert(chat.order == 0L)
//              chat.chatList = updateChat.chatList
//            }
//          }
//        }
//        TdApi.UpdateChatLastMessage.CONSTRUCTOR -> {
//          val updateChat = received as TdApi.UpdateChatLastMessage
//          val chat = chats[updateChat.chatId]
//          synchronized(chat!!) {
//            chat.lastMessage = updateChat.lastMessage
//            setChatOrder(chat, updateChat.order)
//          }
//        }
//        TdApi.UpdateChatOrder.CONSTRUCTOR -> {
//          val updateChat = received as UpdateChatOrder
//          val chat = chats[updateChat.chatId]
//          synchronized(chat!!) { setChatOrder(chat, updateChat.order) }
//        }
//        TdApi.UpdateChatIsPinned.CONSTRUCTOR -> {
//          val updateChat = received as UpdateChatIsPinned
//          val chat = chats[updateChat.chatId]
//          synchronized(chat!!) {
//            chat.isPinned = updateChat.isPinned
//            setChatOrder(chat, updateChat.order)
//          }
//        }
//        TdApi.UpdateChatReadInbox.CONSTRUCTOR -> {
//          val updateChat = received as UpdateChatReadInbox
//          val chat = chats[updateChat.chatId]
//          synchronized(chat!!) {
//            chat.lastReadInboxMessageId = updateChat.lastReadInboxMessageId
//            chat.unreadCount = updateChat.unreadCount
//          }
//        }
        TdApi.UpdateChatReadOutbox.CONSTRUCTOR -> {
          val updateChat = received as TdApi.UpdateChatReadOutbox
          val chat = chats[updateChat.chatId]
          synchronized(chat!!) { chat.lastReadOutboxMessageId = updateChat.lastReadOutboxMessageId }
        }
        TdApi.UpdateChatUnreadMentionCount.CONSTRUCTOR -> {
          val updateChat = received as TdApi.UpdateChatUnreadMentionCount
          val chat = chats[updateChat.chatId]
          synchronized(chat!!) { chat.unreadMentionCount = updateChat.unreadMentionCount }
        }
        TdApi.UpdateMessageMentionRead.CONSTRUCTOR -> {
          val updateChat = received as TdApi.UpdateMessageMentionRead
          val chat = chats[updateChat.chatId]
          synchronized(chat!!) { chat.unreadMentionCount = updateChat.unreadMentionCount }
        }
        TdApi.UpdateChatReplyMarkup.CONSTRUCTOR -> {
          val updateChat = received as TdApi.UpdateChatReplyMarkup
          val chat = chats[updateChat.chatId]
          synchronized(chat!!) { chat.replyMarkupMessageId = updateChat.replyMarkupMessageId }
        }
//        TdApi.UpdateChatDraftMessage.CONSTRUCTOR -> {
//          val updateChat = received as TdApi.UpdateChatDraftMessage
//          val chat = chats[updateChat.chatId]
//          synchronized(chat!!) {
//            chat.draftMessage = updateChat.draftMessage
//            setChatOrder(chat, updateChat.order)
//          }
//        }
        TdApi.UpdateChatNotificationSettings.CONSTRUCTOR -> {
          val update = received as TdApi.UpdateChatNotificationSettings
          val chat = chats[update.chatId]
          synchronized(chat!!) { chat.notificationSettings = update.notificationSettings }
        }
        TdApi.UpdateChatDefaultDisableNotification.CONSTRUCTOR -> {
          val update = received as TdApi.UpdateChatDefaultDisableNotification
          val chat = chats[update.chatId]
          synchronized(
            chat!!
          ) { chat.defaultDisableNotification = update.defaultDisableNotification }
        }
        TdApi.UpdateChatIsMarkedAsUnread.CONSTRUCTOR -> {
          val update = received as TdApi.UpdateChatIsMarkedAsUnread
          val chat = chats[update.chatId]
          synchronized(chat!!) { chat.isMarkedAsUnread = update.isMarkedAsUnread }
        }
//        TdApi.UpdateChatIsSponsored.CONSTRUCTOR -> {
//          val updateChat = received as TdApi.UpdateChatIsSponsored
//          val chat = chats[updateChat.chatId]
//          synchronized(chat!!) {
//            chat.isSponsored = updateChat.isSponsored
//            setChatOrder(chat, updateChat.order)
//          }
//        }
        TdApi.UpdateUserFullInfo.CONSTRUCTOR -> {
          val updateUserFullInfo = received as TdApi.UpdateUserFullInfo
          usersFullInfo[updateUserFullInfo.userId] = updateUserFullInfo.userFullInfo
        }
        TdApi.UpdateBasicGroupFullInfo.CONSTRUCTOR -> {
          val updateBasicGroupFullInfo = received as TdApi.UpdateBasicGroupFullInfo
          basicGroupsFullInfo[updateBasicGroupFullInfo.basicGroupId] =
            updateBasicGroupFullInfo.basicGroupFullInfo
        }
        TdApi.UpdateSupergroupFullInfo.CONSTRUCTOR -> {
          val updateSupergroupFullInfo = received as TdApi.UpdateSupergroupFullInfo
          superGroupFullInfo[updateSupergroupFullInfo.supergroupId] =
            updateSupergroupFullInfo.supergroupFullInfo
        }
        TdApi.UpdateMessageSendSucceeded.CONSTRUCTOR -> {
          fileUpdate = FileUpdate(received as TdApi.UpdateMessageSendSucceeded)
          if(fileUpdate.isUploadingCompleted) {
            fileUpdate.addFile()
            Sync.dataTransferInProgress = 0
            Sync.nextDataTransfer()
          }
        }
        TdApi.UpdateMessageContent.CONSTRUCTOR -> {
          fileUpdate = FileUpdate(received as TdApi.UpdateMessageContent)
          if(fileUpdate.isUploadingCompleted) {
            fileUpdate.addFile()
            Sync.dataTransferInProgress = 0
            Sync.nextDataTransfer()
          }
        }
        TdApi.Message.CONSTRUCTOR -> {
          fileUpdate = FileUpdate(received as TdApi.Message)
          if(fileUpdate.isUploadingCompleted) {
            fileUpdate.addFile()
            Sync.dataTransferInProgress = 0
            Sync.nextDataTransfer()
          }
        }
        TdApi.File.CONSTRUCTOR -> {
          val file = received as TdApi.File
          file.let {
            synchronized(downloadingLock) { downloadingUpdate(it) }
            uploadingUpdate(it)
          }
        }
        TdApi.UpdateFile.CONSTRUCTOR -> {
          val updateFile = received as TdApi.UpdateFile
          if(updateFile.file != null) {
            synchronized(downloadingLock) { downloadingUpdate(updateFile.file) }
            uploadingUpdate(updateFile.file)
          }
        }
        TdApi.Messages.CONSTRUCTOR -> {
          val messages: Messages = Messages.get()
          messages.messageUpdate(received as TdApi.Messages)
        }
        TdApi.UpdateDeleteMessages.CONSTRUCTOR -> {
          println("UpdateDeleteMessages")
          Sync.start() //TODO
        }
        else -> println("UpdatesHandler unsupported update:$newLine$received")
      }

      if(waitingCreatedChat) {
        Log.d(null, "waitingCreatedChat $waitingCreatedChat")
        for(chat in chats.values) {
          for(supergroup in superGroups.values) {
            if(
              chat.type.constructor == TdApi.ChatTypeSupergroup.CONSTRUCTOR
              && supergroup.status.constructor == TdApi.ChatMemberStatusCreator.CONSTRUCTOR
            ) {
              val type = chat.type as TdApi.ChatTypeSupergroup
              val status = supergroup.status as TdApi.ChatMemberStatusCreator
              if(
                type.supergroupId == supergroup.id
                && type.isChannel
                && supergroup.isChannel
                && "" == supergroup.username
                && "" == status.customTitle
                && status.isMember
                && enteredGroupName != null
                && enteredGroupName == chat.title
                && settingsHelper != null
              ) {
                settings = settingsHelper?.settings.let {
                  val tmp = it ?: Settings()
                  if(
                    tmp.chatId != 0L
                    && tmp.supergroupId != 0
                    && tmp.chatId != chat.id
                    && tmp.supergroupId != supergroup.id
                  ) {
                    fileHelper?.deleteByChatId(tmp.chatId)
                  }
                  tmp.supergroupId = supergroup.id
                  tmp.chatId = chat.id
                  tmp.title = chat.title
                  tmp.lastUpdate = supergroup.date.toLong()
                  tmp.isChannel = type.isChannel
                  tmp
                }

                settings?.let { settingsHelper?.updateSettings(it) }

                syncStatus?.setGroupName(enteredGroupName)
                enteredGroupName = null
                waitingCreatedChat = false
              }
            }
          }
        }
      }

    }
  }

  val groupList: List<Group>
    get() {
      val groups: MutableList<Group> = ArrayList<Group>()
      for(chat in chats.values) {
        for(supergroup in superGroups.values) {
          if(
            chat.type.constructor == TdApi.ChatTypeSupergroup.CONSTRUCTOR
            && supergroup.status.constructor == TdApi.ChatMemberStatusCreator.CONSTRUCTOR
          ) {
            val type = chat.type as TdApi.ChatTypeSupergroup
            val status = supergroup.status as TdApi.ChatMemberStatusCreator
            if(type.supergroupId == supergroup.id && status.isMember) {
              val g = Group()
              g.superGroupId = supergroup.id
              g.chatId = chat.id
              g.title = chat.title
              g.date = supergroup.date.toLong()
              g.isChannel = type.isChannel
              groups.add(g)
            }
          }
        }
      }
      return groups
    }

  val mainActivity: MainActivity?
    get() {
      return ContextHolder.getActivity()
    }

  val settingsHelper: SettingsHelper?
    get() {
      return SettingsHelper.get()
    }

  val fileHelper: FileHelper?
    get() {
      return FileHelper.get()
    }

  private inner class UpdateExceptionHandler : Client.ExceptionHandler {
    override fun onException(e: Throwable) {
      e.printStackTrace()
    }
  }

  private inner class DefaultExceptionHandler : Client.ExceptionHandler {
    override fun onException(e: Throwable) {
      e.printStackTrace()
    }
  }

  inner class AuthorizationRequestHandler : ResultHandler {
    override fun onResult(update: TdApi.Object) {
      when(update.constructor) {
        TdApi.Error.CONSTRUCTOR -> onAuthorizationStateUpdated(null) // repeat last action
        TdApi.Ok.CONSTRUCTOR -> Log.d(null, "AuthorizationRequestHandler TdApi.Ok$newLine $update")
        else -> Log.d(null, "Unsupported AuthorizationRequestHandler default$newLine $update")
      }
    }
  }

  companion object {
    private var instance: Tg? = null

    fun get(): Tg? {
      for(ste in Thread.currentThread().stackTrace) {
        Log.d(null, ste.toString())
      }
      if(instance == null) {
        instance = Tg()
      }
      return instance
    }

    var syncStatus: SettingsFragment.SyncStatus? = null
    const val tdLibPath = Constants.tdLibPath
    private var phoneNumber: String? = null
    private var code: String? = null
    private var authorizationState: TdApi.AuthorizationState? = null

    @Volatile
    var haveAuthorization = false
    var authorizationStateWaitPhoneNumber = false
    var isConnected = false
    var needUpdate = false

    private val newLine = System.getProperty("line.separator")
    private val users: MutableMap<Int, TdApi.User> = mutableMapOf()
    private val chats: MutableMap<Long, TdApi.Chat> = mutableMapOf()
//    private val mainChatList: NavigableSet<OrderedChat> = TreeSet()
    private val basicGroups: MutableMap<Int, TdApi.BasicGroup> = mutableMapOf()
    private val superGroups: MutableMap<Int, TdApi.Supergroup> = mutableMapOf()
    private val secretChats: MutableMap<Int, TdApi.SecretChat> = mutableMapOf()
    private var haveFullMainChatList = false
    private val usersFullInfo: MutableMap<Int, TdApi.UserFullInfo> = mutableMapOf()
    private val basicGroupsFullInfo: MutableMap<Int, TdApi.BasicGroupFullInfo> = mutableMapOf()
    private val superGroupFullInfo: MutableMap<Int, TdApi.SupergroupFullInfo> = mutableMapOf()
  }

  init {
    createClient()
  }

}