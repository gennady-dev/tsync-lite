package lite.telestorage.kt

import android.content.Context
import android.util.Log
import org.drinkless.td.libcore.telegram.TdApi

object Updates {

  var updated = false
  var messageIterator: MutableIterator<MutableMap.MutableEntry<Long, TdApi.Message>>? = null
  var tdMessage: TdApi.Message? = null
  var actions: MutableList<() -> Unit> = mutableListOf()

  private fun startAfterUpdate(){
    for(action in actions){
      action()
    }
    FileUpdates.syncQueue()
    actions = mutableListOf()
  }

  fun updateMessagePath(error: Boolean = false) {
    for (ste in Thread.currentThread().stackTrace) {
      println(ste);
    }
    if(!updated){
      if(!error) tdMessage = null
      val pref = ContextHolder.context
        ?.getSharedPreferences(Constants.updatePreferences, Context.MODE_PRIVATE)
      val prefEditor = pref?.edit()
      val iconUpdated = pref?.getBoolean(Constants.FOLDER_ICON_UPDATED, false) ?: false

      Log.d(Constants.FOLDER_ICON_UPDATED, iconUpdated.toString())

      if(!iconUpdated) {
        val msgIterator = messageIterator.let {
          val iter = it ?: Messages.messageMap.iterator()
          messageIterator = iter
          iter
        }
        val msg = tdMessage ?: if(msgIterator.hasNext()) msgIterator.next().value else null
        if(msg != null) {
          tdMessage = msg
          val message = Message(msg)
          val caption = message.caption
          val chatId = message.chatId
          val messageId = message.messageId
          Log.d("messageIterator", messageId.toString())
          val client = Tg.client
          if(caption?.matches(".*${Constants.FOLDER_ICON}.*".toRegex(RegexOption.DOT_MATCHES_ALL)) == false) {
            if(client != null && chatId != 0L && messageId != 0L) {
              val path = "${Constants.FOLDER_ICON}${caption.trim()}"
              client.send(
                TdApi.EditMessageCaption(
                  chatId, messageId, null, TdApi.FormattedText(path, null)
                ),
                Tg.updateHandler
              )
            }
          } else {
            updateMessagePath()
          }
        } else {
          updated = true
          tdMessage = null
          messageIterator = null
          prefEditor?.putBoolean(Constants.FOLDER_ICON_UPDATED, true)
          prefEditor?.apply()
          startAfterUpdate()
        }
      } else {
        updated = true
        startAfterUpdate()
      }
    }
  }

}