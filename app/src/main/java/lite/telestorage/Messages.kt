package lite.telestorage


import android.util.Log
//import lite.telestorage.kt.database.FileHelper
import org.drinkless.td.libcore.telegram.TdApi
import org.drinkless.td.libcore.telegram.TdApi.GetChatHistory
import java.util.*
import kotlin.concurrent.withLock


object Messages {

  var hasFullMessageMap = false
  var lastMessage: TdApi.Message? = null
  val messageMap: MutableMap<Long, TdApi.Message> = mutableMapOf()
  var limit = 50

  fun getMessages(start: Boolean = true) {
    if(start){
      hasFullMessageMap = false
      lastMessage = null
      messageMap.clear()
    }
    val chatId: Long = Settings.chatId
    if(chatId != 0L && !hasFullMessageMap) {
      Data.dataTransferInProgress = Date().time
      val messageId: Long = lastMessage?.id ?: 0
      Tg.client?.send(
        GetChatHistory(chatId, messageId, 0, limit, false),
        Tg.updateHandler
      )
    }
  }


  fun messageUpdate(receivedMessages: TdApi.Messages?) {
    val messages = receivedMessages?.messages
    if(!messages.isNullOrEmpty()) {
      for((i, message) in messages.withIndex()) {
        if(message is TdApi.Message){
          if(!messageMap.containsKey(message.id)) {
            Message(message).fileData?.also { Data.addFromMsg(it) }
            messageMap[message.id] = message
          }
          if(i == messages.size - 1) {
            lastMessage = message
          }
        }
      }
      getMessages(false)
    } else {
      hasFullMessageMap = true
      Log.d("hasFullMessageMap", messageMap.toString())

      Data.dataTransferInProgress = Date().time
      FileUpdates.syncAll()
      Data.dataTransferInProgress = 0

//      if(Data.toDeleteFromDb.isNotEmpty()) {
//        for(file in Data.toDeleteFromDb) {
//          FileHelper.delete(file)
//        }
//        Data.toDeleteFromDb.clear()
//      }

//      deleteMsgByIds()

      FileUpdates.nextDataTransfer()

      Data.debugInfo()

    }
  }

  fun deleteMsgByIds(){
    if(Settings.chatId != 0L && Data.msgIdsForDelete.isNotEmpty()){
      val ids = Data.msgIdsForDelete.toLongArray()
      Tg.client?.send(TdApi.DeleteMessages(Settings.chatId, ids, true), Tg.updateHandler)
      Data.msgIdsForDelete.clear()
    }
  }

}
