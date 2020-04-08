package lite.telestorage.kt


import lite.telestorage.kt.database.FileHelper
import lite.telestorage.kt.database.SettingsHelper
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import org.drinkless.td.libcore.telegram.TdApi.GetChatHistory
import java.util.*


class Messages private constructor() {

  private var hasFullMessageMap = false
  private var lastMessage: TdApi.Message? = null
  private val messageMap: MutableMap<Long, TdApi.Message> = mutableMapOf()
  var limit = 50

  private val settingsHelper: SettingsHelper?
    get() {
      return SettingsHelper.get()
    }

  private val tg: Tg?
    get() {
      return Tg.get()
    }

  private val fileHelper: FileHelper?
    get() {
      return FileHelper.get()
    }

  fun getMessages() {
    val chatId: Long = settingsHelper?.settings?.chatId ?: 0
    if(chatId != 0L && !hasFullMessageMap) {
      val messageId: Long = lastMessage?.id ?: 0
      val client: Client? = tg?.client
      val updateHandler: Tg.UpdateHandler? = tg?.updateHandler
      if(messageId != 0L && client != null && updateHandler != null) {
        client.send(
          GetChatHistory(chatId, messageId, 0, limit, false), updateHandler
        )
      }
    }
  }

  fun messageUpdate(receivedMessages: TdApi.Messages?) {
    val messages = receivedMessages?.messages
    if(messages != null) {
      if(messages.isNotEmpty()) {
        for((i, message) in messages.withIndex()) {
          if(!messageMap.containsKey(message.id)) {
            messageMap[message.id] = message
          }
          if(i == messages.size - 1) {
            lastMessage = message
          }
        }
        getMessages()
      } else {
        hasFullMessageMap = true
        settingsHelper?.settings?.let {
          it.messagesDownloaded = Date().time
          settingsHelper?.updateSettings(it)
          it
        }
      }
    }
  }

  fun syncMessagesWithDb(){
    if(hasFullMessageMap) {
      val fileUpdate = FileUpdate(messageMap)
      fileUpdate.commitUpdates()
      Sync.afterMessageSynced()
      messageMap.clear()
      hasFullMessageMap = false
      lastMessage = null
      Sync.inProgress = 0
    }
  }

  companion object {
    private var instance: Messages? = null
    fun get(): Messages {
      val inst = instance ?: Messages()
      instance = inst
      return inst
    }
  }

}
