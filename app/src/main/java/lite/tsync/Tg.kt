package lite.tsync

import android.os.Build
import android.util.Log

//import lite.telestorage.kt.database.FileHelper
import lite.tsync.models.FileData
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.Client.ResultHandler
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File
import java.util.NavigableSet
import java.util.TreeSet
import java.util.Arrays
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.ArrayList

@Singleton
class Tg @Inject constructor() {

  var syncStatus: SettingsFragment.SyncStatus? = null
  val tdLibPath = Constants.tdLibPath
  private var authorizationState: TdApi.AuthorizationState? = null
  var settingsFragment: SettingsFragment? = null
  private var TAG: String? = null
  var client: Client? = null
  var updateHandler = UpdateHandler()
  private var authorizationRequestHandler = AuthorizationRequestHandler()
  var settings: Settings2? = null
  var waitingCreatedChat = false
  var waitingSuperGroupResponse = false
  var memberStatusBanned = false
  var defaultGroupName: String? = null
  var enteredGroupName: String? = null
  var uploadingInProgress: Long = 0
  private val uploadingLock = Any()
  private val downloadingLock = Any()
  private val mainChatList: NavigableSet<OrderedChat> = TreeSet()

  var haveAuthorization = false
  var authorizationStateWaitPhoneNumber = false
  var isConnected = false
  var needUpdate: Type? = null

  private val newLine = System.getProperty("line.separator")
  private val users: MutableMap<Int, TdApi.User> = mutableMapOf()
  private val chats: MutableMap<Long, TdApi.Chat> = mutableMapOf()
  private val basicGroups: MutableMap<Int, TdApi.BasicGroup> = mutableMapOf()
  private val superGroups: MutableMap<Int, TdApi.Supergroup> = mutableMapOf()
  private val secretChats: MutableMap<Int, TdApi.SecretChat> = mutableMapOf()
  private var haveFullMainChatList = false
  private val usersFullInfo: MutableMap<Int, TdApi.UserFullInfo> = mutableMapOf()
  private val basicGroupsFullInfo: MutableMap<Int, TdApi.BasicGroupFullInfo> = mutableMapOf()
  private val superGroupFullInfo: MutableMap<Int, TdApi.SupergroupFullInfo> = mutableMapOf()

  init {
    client = Client.create(updateHandler, UpdateExceptionHandler(), DefaultExceptionHandler())
  }

  fun logout() {
    client?.send(TdApi.LogOut(), updateHandler)
    client = null
  }

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
        settingsFragment?.codeDialog()
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
        setMainChatList(10)
        settingsFragment?.also {
          it.setLogged(true)
          Sync.start()
        }
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
        settingsFragment?.setLogged(false)
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

  fun sendPhone(phone: String, onResult: (() -> Unit)? = null) {
    if(client == null) client = Client.create(updateHandler, UpdateExceptionHandler(), DefaultExceptionHandler())
    client?.send(TdApi.SetAuthenticationPhoneNumber(phone, null), AuthorizationRequestHandler(onResult))
  }

  fun sendCode(code: String) {
    client?.send(TdApi.CheckAuthenticationCode(code), authorizationRequestHandler)
  }

  fun downloadFile(file: FileData) {
    if(Data.dataTransferInProgress == 0L) {
      if(file.fileId != 0) {
        Data.dataTransferInProgress = Date().time
        client?.send(TdApi.DownloadFile(file.fileId, 32, 0, 0, true), updateHandler)
      } else {
        FileUpdates.nextDataTransfer()
      }
    }
  }

  fun uploadFile(file: FileData) {
    val chatId = Settings2.chatId
    if(Data.dataTransferInProgress == 0L) {
      val mimeType: String? = file.mimeType
      if(
        mimeType != null
        && !mimeType.matches("""image/gif""".toRegex(RegexOption.IGNORE_CASE))
        && !mimeType.matches("""image/webp""".toRegex(RegexOption.IGNORE_CASE))
      ) {
        Data.dataTransferInProgress = Date().time
        when {
          mimeType.matches(".*video.*".toRegex(RegexOption.DOT_MATCHES_ALL)) -> {
            sendVideo(file)
          }
          mimeType.matches(".*audio.*".toRegex(RegexOption.DOT_MATCHES_ALL)) -> {
            sendAudio(file)
          }
          else -> sendDocument(file)
        }
      }
      else {
        FileUpdates.nextDataTransfer()
      }
//      if(Settings.uploadAsMedia) {
//        val mimeType: String? = file.mimeType
//        if(mimeType != null) {
//          if(mimeType.matches(".*video.*".toRegex(RegexOption.DOT_MATCHES_ALL))) {
//            sendVideo(file)
//          } else if(mimeType.matches(".*audio.*".toRegex(RegexOption.DOT_MATCHES_ALL))) {
//            sendAudio(file)
//          } else if(mimeType.matches(".*image.*".toRegex(RegexOption.DOT_MATCHES_ALL))) {
//            if(
//              mimeType.matches("(?i)image/gif".toRegex())
//              || mimeType.matches("(?i)image/webp".toRegex())
//            ) {
//              sendDocument(file)
//            } else {
//              sendPhoto(file)
//            }
//          } else {
//            sendDocument(file)
//          }
//        }
//      } else {
//        sendDocument(file)
//      }
    }
  }

  private fun getCaption(path: String): String {
    val before: String = ""
    val after: String = ""
    return before + path + after
  }

  private fun sendDocument(fileData: FileData) {
    //📂📂 "\uD83D\uDCC2/"
    val relativePath: String? = fileData.path
    val absPath: String? = relativePath?.let { Fs.getAbsPath(it) }
    val file = absPath?.let { File(it) }
    if(
      Settings2.chatId != 0L
      && file != null
      && file.exists()
      && file.isFile
      && file.length() > 0
    ) {
      val document: TdApi.InputMessageContent = TdApi.InputMessageDocument(
        TdApi.InputFileLocal(absPath), null,
        TdApi.FormattedText(getCaption(relativePath), null)
      )
      client?.send(TdApi.SendMessage(Settings2.chatId, 0, null, null, document), updateHandler)
//        if(fileData.messageId == 0L) {
//          client?.send(TdApi.SendMessage(Settings.chatId, 0, null, null, document), updateHandler)
//        } else {
//          client?.send(
//            TdApi.EditMessageMedia(
//              Settings.chatId, fileData.messageId, null, document
//            ), updateHandler
//          )
//        }
    } else {
      Data.dataTransferInProgress = 0
      FileUpdates.nextDataTransfer()
    }
  }

  private fun sendVideo(fileData: FileData) {
    val relativePath: String? = fileData.path
    val absPath: String? = relativePath?.let { Fs.getAbsPath(it) }
    val f = absPath?.let { File(it) }
    if(Settings2.chatId != 0L && f != null && f.exists() && f.isFile && f.length() > 0) {
        val video: TdApi.InputMessageContent = TdApi.InputMessageVideo(
          TdApi.InputFileLocal(absPath),
          null,
          null,
          0,
          0,
          0,
          true,
          TdApi.FormattedText(getCaption(relativePath), null),
          0
        )
        client?.send(TdApi.SendMessage(Settings2.chatId, 0, null, null, video), updateHandler)
//        if(fileData.messageId == 0L) {
//          client?.send(TdApi.SendMessage(Settings.chatId, 0, null, null, video), updateHandler)
//        } else {
//          client?.send(
//            TdApi.EditMessageMedia(Settings.chatId, fileData.messageId, null, video),
//            updateHandler
//          )
//        }
      } else {
//        FileHelper.delete(fileData)
        Data.dataTransferInProgress = 0
        FileUpdates.nextDataTransfer()
      }
  }

  private fun sendAudio(fileData: FileData) {
    val relativePath: String? = fileData.path
    val absPath: String? = relativePath?.let { Fs.getAbsPath(it) }
    val f = absPath?.let { File(it) }
    if(Settings2.chatId != 0L && f != null && f.exists() && f.isFile && f.length() > 0) {
      val audio: TdApi.InputMessageContent = TdApi.InputMessageAudio(
        TdApi.InputFileLocal(absPath),
        null,
        0,
        null,
        null, TdApi.FormattedText(getCaption(relativePath), null)
      )
      client?.send(
        TdApi.SendMessage(Settings2.chatId, 0, null, null, audio),
        updateHandler
      )
//        if(fileData.messageId == 0L) {
//          client?.send(
//            TdApi.SendMessage(Settings.chatId, 0, null, null, audio),
//            updateHandler
//          )
//        } else {
//          client?.send(
//            TdApi.EditMessageMedia(Settings.chatId, fileData.messageId, null, audio),
//            updateHandler
//          )
//        }
    } else {
//        FileHelper.delete(fileData)
      Data.dataTransferInProgress = 0
      FileUpdates.nextDataTransfer()
    }
  }

  private fun sendAnimation(fileData: FileData) {
    val relativePath: String? = fileData.path
    val absPath: String? = relativePath?.let { Fs.getAbsPath(it) }
    val f = absPath?.let { File(it) }
    if(
      Settings2.chatId != 0L
      && f != null
      && f.exists()
      && f.isFile
      && f.length() > 0
    ) {
      val animation: TdApi.InputMessageContent = TdApi.InputMessageAnimation(
        TdApi.InputFileLocal(absPath),
        null,
        0,
        0,
        0,
        TdApi.FormattedText(getCaption(relativePath), null)
      )
      client?.send(TdApi.SendMessage(Settings2.chatId, 0, null, null, animation), updateHandler)
//        if(fileData.messageId == 0L) {
//          client?.send(TdApi.SendMessage(Settings.chatId, 0, null, null, photo), updateHandler)
//        } else {
//          client?.send(
//            TdApi.EditMessageMedia(Settings.chatId, fileData.messageId, null, photo),
//            updateHandler
//          )
//        }
    } else {
//        FileHelper.delete(fileData)
      Data.dataTransferInProgress = 0
      FileUpdates.nextDataTransfer()
    }
  }

  private fun sendPhoto(fileData: FileData) {
    val relativePath: String? = fileData.path
    val absPath: String? = relativePath?.let { Fs.getAbsPath(it) }
    val f = absPath?.let { java.io.File(it) }
    if(Settings2.chatId != 0L){
      if(f != null && f.exists() && f.isFile && f.length() > 0) {
        val photo: TdApi.InputMessageContent = TdApi.InputMessagePhoto(
          TdApi.InputFileLocal(absPath), null, null, 0, 0, TdApi.FormattedText(getCaption(relativePath), null), 0
        )
        client?.send(TdApi.SendMessage(Settings2.chatId, 0, null, null, photo), updateHandler)
//        if(fileData.messageId == 0L) {
//          client?.send(TdApi.SendMessage(Settings.chatId, 0, null, null, photo), updateHandler)
//        } else {
//          client?.send(
//            TdApi.EditMessageMedia(Settings.chatId, fileData.messageId, null, photo),
//            updateHandler
//          )
//        }
      } else {
//        FileHelper.delete(fileData)
        Data.dataTransferInProgress = 0
        FileUpdates.nextDataTransfer()
      }
    }
  }

  private fun setChatOrder(chat: TdApi.Chat?, order: Long) {
    chat?.also {
      synchronized(mainChatList) {
        synchronized(it) {
          val chatList = it.chatList
          if(chatList == null || chatList.constructor != TdApi.ChatListMain.CONSTRUCTOR) {
            return
          }
          if(it.order != 0L) {
            val isRemoved = mainChatList.remove(OrderedChat(it.order, it.id))
            assert(isRemoved)
          }
          it.order = order
          if(it.order != 0L) {
            val isAdded = mainChatList.add(OrderedChat(it.order, it.id))
            assert(isAdded)
          }
        }
      }
    }
  }

  private fun setMainChatList(limit: Int) {
    synchronized(mainChatList) {
      if(!haveFullMainChatList && limit > mainChatList.size) {
        var offsetOrder = Long.MAX_VALUE
        var offsetChatId: Long = 0
        if(mainChatList.isNotEmpty()) {
          val last = mainChatList.last()
          offsetOrder = last.order
          offsetChatId = last.chatId
        }
        client?.send(
          TdApi.GetChats(TdApi.ChatListMain(), offsetOrder, offsetChatId, limit - mainChatList.size), ResultHandler {
            when(it.constructor) {
              TdApi.Error.CONSTRUCTOR -> {}
              TdApi.Chats.CONSTRUCTOR -> {
                val chatIds = (it as TdApi.Chats).chatIds
                if(chatIds.isEmpty()) {
                  synchronized(mainChatList) {
                    haveFullMainChatList = true
                    Log.d("haveFullMainChatList", chatIds.toString())
                  }
                } else {
                  setMainChatList(limit)
                }
              }
              else -> System.err.println("Receive wrong response from TDLib:$newLine$it")
            }
          }
        )
      }
    }
  }

  class OrderedChat(val order: Long, val chatId: Long) : Comparable<OrderedChat> {

    override fun compareTo(other: OrderedChat): Int {
      if(order != other.order) {
        return if(other.order < order) -1 else 1
      }
      return if(chatId != other.chatId) {
        if(other.chatId < chatId) -1 else 1
      } else 0
    }

    override fun equals(other: Any?): Boolean {
      val o = other as OrderedChat
      return order == o.order && chatId == o.chatId
    }

  }

  inner class UpdateHandler : ResultHandler {

    override fun onResult(received: TdApi.Object) {
      if(TAG != null) {
        Log.d("UpdatesHandler", "UpdatesHandler $TAG $received")
      } else {
        Log.d("UpdatesHandler", "UpdatesHandler $received")
      }

      Log.d("Settings.uploadMissing", Settings2.uploadMissing.toString())

      val fileUpdate: FileUpdate
      when(received.constructor) {
        TdApi.Chats.CONSTRUCTOR -> {
          val chatIds = (received as TdApi.Chats).chatIds
          Log.d("chat array", Arrays.toString(chatIds))
          if(chatIds.isEmpty()) {
            synchronized(mainChatList) { haveFullMainChatList = true }
            val iter: Iterator<OrderedChat> = mainChatList.iterator()
            println()
            var i = 0
            while(i < mainChatList.size) {
              val chatId = iter.next().chatId
              val chat = chats[chatId]
              synchronized(chat!!) { Log.d("ID $chatId", chat.toString()) }
              i++
            }
          } else {
            // chats had already been received through updates, let's retry request
            setMainChatList(10)
          }
        }
        TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
          Log.d(null, "UpdatesHandler UpdateAuthorizationState $received")
          onAuthorizationStateUpdated((received as TdApi.UpdateAuthorizationState).authorizationState)
        }
        TdApi.UpdateConnectionState.CONSTRUCTOR -> {
          if((received as TdApi.UpdateConnectionState).state.constructor == TdApi.ConnectionStateReady.CONSTRUCTOR) {
            isConnected = true
            needUpdate?.also {
//              Sync.start(it)
              Sync.start()
              needUpdate = null
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
          if(
            updateSupergroup.supergroup.id == Settings2.groupId
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
//            val chatId: Long = Settings?.chatId ?: 0
//            if(chatId != 0L) {
//              FileHelper.deleteByChatId(chatId)
//            }
            Settings2.enabled = false
            Settings2.chatId = 0
            Settings2.groupId = 0
            Settings2.title = null
            Settings2.save()
          }
        }
        TdApi.UpdateSecretChat.CONSTRUCTOR -> {
          val updateSecretChat = received as TdApi.UpdateSecretChat
          secretChats[updateSecretChat.secretChat.id] = updateSecretChat.secretChat
        }
        TdApi.UpdateNewChat.CONSTRUCTOR -> {
          val updateNewChat = received as TdApi.UpdateNewChat
          val chat = updateNewChat.chat
          synchronized(chat) {
            chats[chat.id] = chat
            val order = chat.order
            chat.order = 0
            setChatOrder(chat, order)
          }
        }
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
        TdApi.UpdateChatChatList.CONSTRUCTOR -> {
          val updateChat = received as TdApi.UpdateChatChatList
          val chat = chats[updateChat.chatId]
          synchronized(mainChatList) { // to not change Chat.chatList while mainChatList is locked
            synchronized(chat!!) {
              assert(chat.order == 0L)
              chat.chatList = updateChat.chatList
            }
          }
        }
        TdApi.UpdateChatLastMessage.CONSTRUCTOR -> {
          val updateChat = received as TdApi.UpdateChatLastMessage
          val chat = chats[updateChat.chatId]
          synchronized(chat!!) {
            chat.lastMessage = updateChat.lastMessage
            setChatOrder(chat, updateChat.order)
          }
        }
        TdApi.UpdateChatOrder.CONSTRUCTOR -> {
          val updateChat = received as TdApi.UpdateChatOrder
          val chat = chats[updateChat.chatId]
          synchronized(chat!!) { setChatOrder(chat, updateChat.order) }
        }
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
          chats[update.chatId]?.also {
            synchronized(it) { it.isMarkedAsUnread = update.isMarkedAsUnread }
          }
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

        TdApi.UpdateMessageSendSucceeded.CONSTRUCTOR -> {//TODO
          (received as TdApi.UpdateMessageSendSucceeded).also {
            Message(it).fileData?.also { file -> FileUpdates.uploadingUpdate(file) }
          }
          val debug = null
        }

        TdApi.UpdateMessageSendFailed.CONSTRUCTOR -> {
          Data.dataTransferInProgress = 0
          FileUpdates.nextDataTransfer(false)
          val debug = null
        }

        TdApi.UpdateMessageContent.CONSTRUCTOR -> {
//          fileUpdate = FileUpdate(received as TdApi.UpdateMessageContent)
//          if(fileUpdate.isUploadingCompleted) {
//            fileUpdate.addFile()
//            Data.dataTransferInProgress = 0
//            FileUpdates.current = null
//            FileUpdates.nextDataTransfer()
//          }
          val debug = null
        }
        TdApi.UpdateNewMessage.CONSTRUCTOR -> {
          (received as TdApi.UpdateNewMessage).also {
            Message(it.message).fileData?.also { file -> FileUpdates.newMessageHandler(file) }
          }
          val debug = null
        }
        TdApi.Message.CONSTRUCTOR -> {
          (received as TdApi.Message).also {
            Message(it).fileData?.also { file -> FileUpdates.newMessageHandler(file) }
          }
          val debug = null
        }
        TdApi.Messages.CONSTRUCTOR -> {
          Messages.messageUpdate(received as TdApi.Messages)
        }
        TdApi.UpdateDeleteMessages.CONSTRUCTOR -> {
          (received as TdApi.UpdateDeleteMessages)
            .also { Data.deleteMessages(it) }
          // Sync.start() //TODO
        }
        TdApi.File.CONSTRUCTOR -> {
          synchronized(Data){
            FileUpdates.downloadingUpdate(received as TdApi.File)
          }
//          Data.lock.withLock {
//            FileUpdates.downloadingUpdate(received as TdApi.File)
//          }
//          downloadingUpdate(received as TdApi.File)
//          uploadingUpdate(received)
////          val file = received as TdApi.File
////          file.let {
////            synchronized(downloadingLock) { downloadingUpdate(it) }
////            uploadingUpdate(it)
////          }
          val debug = null
        }
        TdApi.UpdateFile.CONSTRUCTOR -> {
          synchronized(Data){
            (received as TdApi.UpdateFile).file?.also { FileUpdates.downloadingUpdate(it) }
          }
//          Data.lock.withLock {
//            (received as TdApi.UpdateFile).file?.also { FileUpdates.downloadingUpdate(it) }
//          }
//          FileUpdates.downloadingUpdate((received as TdApi.UpdateFile).file)
//          val updateFile = received as TdApi.UpdateFile
//          updateFile.file?.also {
//            downloadingUpdate(it)
//            uploadingUpdate(it)
//          }
////          if(updateFile.file != null) {
////            synchronized(downloadingLock) { downloadingUpdate(updateFile.file) }
////            uploadingUpdate(updateFile.file)
////          }
          val debug = null
        }
        TdApi.Error.CONSTRUCTOR -> {
          Log.d("UpdatesHandler", "UpdatesHandler $received")
        }
        else -> {
//          FileUpdates.nextDataTransfer(false)
//          if(!Updates.updated) Updates.messageIterator?.also { Updates.updateMessagePath(true) }
          println("UpdatesHandler unsupported update:$newLine$received")
          val debug = null
        }
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
              ) {
//                if(
//                  Settings.chatId != 0L
//                  && Settings.supergroupId != 0
//                  && Settings.chatId != chat.id
//                  && Settings.supergroupId != supergroup.id
//                ) FileHelper.deleteByChatId(Settings.chatId)

                Settings2.groupId = supergroup.id
                Settings2.chatId = chat.id
                Settings2.title = chat.title
                Settings2.isChannel = type.isChannel
                Settings2.save()

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
      val groups: MutableList<Group> = ArrayList()
      for(chat in chats.values) {
        if(chat.id != 777000L){
          val group = Group()
          if(chat.type.constructor == TdApi.ChatTypeSupergroup.CONSTRUCTOR) {
            val type = chat.type as TdApi.ChatTypeSupergroup
            superGroups.values.find { it.id == type.supergroupId }?.also {
              group.groupId = it.id
              group.date = it.date.toLong()
              group.isChannel = type.isChannel
              if(it.status.constructor == TdApi.ChatMemberStatusCreator.CONSTRUCTOR) {
                group.creator = true
              }
              if(
                it.status.constructor == TdApi.ChatMemberStatusAdministrator.CONSTRUCTOR
                || it.status.constructor == TdApi.ChatMemberStatusMember.CONSTRUCTOR
              ) {
                group.member = true
              }
            }
          } else if(chat.type.constructor == TdApi.ChatTypeBasicGroup.CONSTRUCTOR){
            val type = chat.type as TdApi.ChatTypeBasicGroup
            basicGroups.values.find { it.id == type.basicGroupId }?.also {
              group.groupId = it.id
              if(it.status.constructor == TdApi.ChatMemberStatusCreator.CONSTRUCTOR) {
                group.creator = true
              }
              if(
                it.status.constructor == TdApi.ChatMemberStatusAdministrator.CONSTRUCTOR
                || it.status.constructor == TdApi.ChatMemberStatusMember.CONSTRUCTOR
              ) {
                group.member = true
              }
            }
          }
          group.chatId = chat.id
          group.title = chat.title
          if(group.member || group.creator){
            groups.add(group)
          }
        }
      }
      return groups
    }

  val mainActivity: MainActivity?
    get() {
      return ContextHolder.activity
    }

  private class UpdateExceptionHandler : Client.ExceptionHandler {
    override fun onException(e: Throwable) {
      e.printStackTrace()
    }
  }

  private class DefaultExceptionHandler : Client.ExceptionHandler {
    override fun onException(e: Throwable) {
      e.printStackTrace()
    }
  }

  inner class AuthorizationRequestHandler(var onResult: (() -> Unit)? = null) : ResultHandler {
    override fun onResult(update: TdApi.Object) {
      when(update.constructor) {
        TdApi.Error.CONSTRUCTOR -> onAuthorizationStateUpdated(null) // repeat last action
        TdApi.Ok.CONSTRUCTOR -> Log.d(null, "AuthorizationRequestHandler TdApi.Ok$newLine $update")
        else -> Log.d(null, "Unsupported AuthorizationRequestHandler default$newLine $update")
      }
    }
  }

}