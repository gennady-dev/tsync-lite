package lite.tsync


import android.util.Log
//import lite.telestorage.kt.database.FileHelper
import org.drinkless.td.libcore.telegram.TdApi
import org.drinkless.td.libcore.telegram.TdApi.GetChatHistory
import java.util.*


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
    val chatId: Long = Settings2.chatId
    if(chatId != 0L && !hasFullMessageMap) {
      Data.dataTransferInProgress = Date().time
      val messageId: Long = lastMessage?.id ?: 0
      Tg2.client?.send(
        GetChatHistory(chatId, messageId, 0, limit, false),
        Tg2.updateHandler
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
    if(Settings2.chatId != 0L && Data.msgIdsForDelete.isNotEmpty()){
      val ids = Data.msgIdsForDelete.toLongArray()
      Tg2.client?.send(TdApi.DeleteMessages(Settings2.chatId, ids, true), Tg2.updateHandler)
      Data.msgIdsForDelete.clear()
    }
  }

}
