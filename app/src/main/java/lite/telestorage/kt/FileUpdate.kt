package lite.telestorage.kt


import android.content.Context
import android.util.Log
import lite.telestorage.kt.Constants.FOLDER_ICON
import lite.telestorage.kt.database.FileHelper
import lite.telestorage.kt.models.FileData
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File


class FileUpdate internal constructor() {
  private var mContext: Context? = null
  var fileData: FileData? = null
  var isUploadingCompleted = false
    private set
  val DOCUMENT = "document"
  val PHOTO = "photo"
  val VIDEO = "video"
  val AUDIO = "audio"
  val ANIMATION = "animation"
  val STICKER = "sticker"
  val TAG = "Message instance"
  var fileType: String? = null
  var messageId: Long = 0
  var chatId: Long = 0
  var currentChatId: Long = 0
  var message: TdApi.Message? = null
  var messageContent: TdApi.MessageContent? = null
  var mDocument: TdApi.MessageDocument? = null
  var mVideo: TdApi.MessageVideo? = null
  var mAudio: TdApi.MessageAudio? = null
  var mPhoto: TdApi.MessagePhoto? = null
  var mAnimation: TdApi.MessageAnimation? = null


  var fileId: Int = 0
  var filePath: String? = null
  var local: TdApi.LocalFile? = null
  var downloaded = false
  var localPath: String? = null
  var remote: TdApi.RemoteFile? = null
  var fileUniqueId: String? = null
  var uploaded = false
  var upload = false
  var download = false
  var size: Int = 0


  internal constructor(updateFile: TdApi.File) : this() {
    local = updateFile.local
    remote = updateFile.remote
    fileId = updateFile.id
    size = updateFile.size
    remote?.let {
      uploaded = !it.isUploadingActive && it.isUploadingCompleted
      fileUniqueId = if(!it.uniqueId.isNullOrBlank()) it.uniqueId else null
    }
    local?.let {
      downloaded = !it.isDownloadingActive && it.isDownloadingCompleted
      localPath = if(!it.path.isNullOrBlank()) it.path else null
    }
    localPath?.also {
      if(it.matches("${Constants.tdLibPath}.+".toRegex(RegexOption.DOT_MATCHES_ALL))){
        download = true
      } else if(it.matches("${Fs.syncDirAbsPath}.+".toRegex(RegexOption.DOT_MATCHES_ALL))){
        upload = true
        filePath = it.replace("${Fs.syncDirAbsPath}/", "")
      }
    }
  }













  internal constructor(updateFile: TdApi.UpdateFile?) : this() {}

  fun setContext(context: Context?) {
    mContext = context
  }

  internal constructor(fileUpdate: TdApi.UpdateMessageSendSucceeded) : this() {
    fileUpdate.message?.also {
      if(it.id != 0L && it.chatId != 0L){
        message = it
        messageId = it.id
        chatId = it.chatId
        setUploadedFileData(it)
      }
    }
  }

  private fun setUploadedFileData(msg: TdApi.Message) {
    if(msg.content != null) {
      when(msg.content.constructor) {
        TdApi.MessageDocument.CONSTRUCTOR -> setUploadedDocumentFileData()
        TdApi.MessageVideo.CONSTRUCTOR -> setUploadedVideoFileData()
        TdApi.MessageAudio.CONSTRUCTOR -> setUploadedAudioFileData()
        TdApi.MessagePhoto.CONSTRUCTOR -> setUploadedPhotoFileData()
        TdApi.MessageAnimation.CONSTRUCTOR -> setUploadedAnimationFileData()
        TdApi.MessageSticker.CONSTRUCTOR -> setUploadedStickerFileData()
        else -> Log.d(TAG, "Receive an unknown message type")
      }
      fileData?.also {
        it.messageId = messageId
        it.chatId = chatId
      }
    }
  }

  private fun setUploadedDocumentFileData() {
    fileType = DOCUMENT
    val content = message?.content?.let { it as TdApi.MessageDocument }
    content?.document?.document?.let { setFileFileData(it) }
    content?.document?.let { file ->
      fileData?.let {
        if(it.name == null && file.fileName != null && file.fileName.isNotBlank()) {
          it.name = file.fileName
        }
        if(it.mimeType == null && file.mimeType != null && file.mimeType.isNotBlank()) {
          it.mimeType = file.mimeType
        }
      }
    }
  }

  private fun setUploadedVideoFileData() {
    fileType = VIDEO
    val content = message?.content?.let { it as TdApi.MessageVideo }
    content?.video?.video?.let { setFileFileData(it) }
    content?.video?.let { file ->
      fileData?.let {
        if(it.name == null && file.fileName != null && file.fileName.isNotBlank()) {
          it.name = file.fileName
        }
        if(it.mimeType == null && file.mimeType != null && file.mimeType.isNotBlank()) {
          it.mimeType = file.mimeType
        }
      }
    }
  }

  private fun setUploadedAudioFileData() {
    fileType = AUDIO
    val content = message?.content?.let { it as TdApi.MessageAudio }
    content?.audio?.audio?.let { setFileFileData(it) }
    content?.audio?.let { file ->
      fileData?.let {
        if(it.name == null && file.fileName != null && file.fileName.isNotBlank()) {
          it.name = file.fileName
        }
        if(it.mimeType == null && file.mimeType != null && file.mimeType.isNotBlank()) {
          it.mimeType = file.mimeType
        }
      }
    }
  }

  private fun setUploadedAnimationFileData() {
    fileType = ANIMATION
    val content = message?.content?.let { it as TdApi.MessageAnimation }
    content?.animation?.animation?.let { setFileFileData(it) }
    content?.animation?.let { file ->
      fileData?.let {
        if(it.name == null && file.fileName != null && file.fileName.isNotBlank()) {
          it.name = file.fileName
        }
        if(it.mimeType == null && file.mimeType != null && file.mimeType.isNotBlank()) {
          it.mimeType = file.mimeType
        }
      }
    }
  }

  private fun setUploadedPhotoFileData() {
    fileType = PHOTO
    val content = message?.content?.let { it as TdApi.MessagePhoto }
    content?.photo?.sizes?.let {
      var height = 0
      var width = 0
      for(photo in it) {
        if(photo != null && photo.height > height && photo.width > width) {
          height = photo.height
          width = photo.width
          setFileFileData(photo.photo)
        }
      }
    }
  }

  private fun setUploadedStickerFileData() {
    fileType = STICKER
    val content = message?.content?.let { it as TdApi.MessageSticker }
    content?.sticker?.sticker?.also { setFileFileData(it) }
  }

  private fun setFileFileData(file: TdApi.File) {
    val responsePath: String? = file.local?.path?.let { path ->
      Fs.syncDirAbsPath?.let { path.replace("$it/", "") }
    }
    val fileData = responsePath?.let { FileHelper.getFileByPath(it) }
    if(fileData != null) {
      if(file.id != 0) {
        fileData.fileId = file.id
      }
      file.remote?.also {
        if(it.uniqueId != null && it.uniqueId.isNotBlank()) {
          fileData.fileUniqueId = it.uniqueId
        }
        if(!it.isUploadingActive && it.isUploadingCompleted) {
          isUploadingCompleted = true
          fileData.uploaded = true
          fileData.inProgress = false
          getLocalFile(fileData)?.also { file ->
            Settings.also { s -> if(s.deleteUploaded) file.delete() }
            if(file.exists()) {
              fileData.lastModified = file.lastModified()
            }
          }
        }
      }
    }
  }

  internal constructor(fileUpdate: TdApi.UpdateMessageContent?) : this() {
    if(fileUpdate?.newContent != null && fileUpdate.messageId != 0L) {
      messageContent = fileUpdate.newContent
      messageId = fileUpdate.messageId
      chatId = fileUpdate.chatId
      fileData = FileHelper.getFileByMsgId(messageId)
      fileData?.also { setEditedFileData() }
    }
  }

  internal constructor(msg: TdApi.Message?) : this() {
    if(msg?.content != null && msg.id != 0L) {
      messageContent = msg.content
      messageId = msg.id
      chatId = msg.chatId
      fileData = FileHelper.getFileByMsgId(messageId)
      fileData?.also { setEditedFileData() }
    }
  }

  private fun setEditedFileData() {
    messageContent?.also {
      when(it.constructor) {
        TdApi.MessageDocument.CONSTRUCTOR -> setEditedDocumentFileData()
        TdApi.MessageVideo.CONSTRUCTOR -> setEditedVideoFileData()
        TdApi.MessageAudio.CONSTRUCTOR -> setEditedAudioFileData()
        TdApi.MessagePhoto.CONSTRUCTOR -> setEditedPhotoFileData()
        TdApi.MessageAnimation.CONSTRUCTOR -> setEditedAnimationFileData()
        TdApi.MessageSticker.CONSTRUCTOR -> setEditedStickerFileData()
        else -> Log.d(TAG, "Receive an unknown message type")
      }
      fileData?.also { f ->
        if(f.messageId == 0L && messageId != 0L) {
          f.messageId = messageId
        }
        if(f.chatId == 0L && chatId != 0L) {
          f.chatId = chatId
        }
      }
    }
  }

  private fun setEditedDocumentFileData() {
    fileType = DOCUMENT
    val content = messageContent as TdApi.MessageDocument?
    content?.document?.document?.also { setEditedFileFileData(it) }
    content?.document?.also {
      if(fileData?.name == null && it.fileName?.isNotBlank() == true) {
        fileData?.name = it.fileName
      }
      if(fileData?.mimeType == null && it.mimeType?.isNotBlank() == true) {
        fileData?.mimeType = it.mimeType
      }
    }
  }

  private fun setEditedVideoFileData() {
    fileType = VIDEO
    val content = messageContent as TdApi.MessageVideo?
    content?.video?.video?.also { setEditedFileFileData(it) }
    content?.video?.also {
      if(fileData?.name == null && it.fileName?.isNotBlank() == true) {
        fileData?.name = it.fileName
      }
      if(fileData?.mimeType == null && it.mimeType?.isNotBlank() == true) {
        fileData?.mimeType = it.mimeType
      }
    }
  }

  private fun setEditedAudioFileData() {
    fileType = AUDIO
    val content = messageContent as TdApi.MessageAudio?
    content?.audio?.audio?.also { setEditedFileFileData(it) }
    content?.audio?.also {
      if(fileData?.name == null && it.fileName?.isNotBlank() == true) {
        fileData?.name = it.fileName
      }
      if(fileData?.mimeType == null && it.mimeType?.isNotBlank() == true) {
        fileData?.mimeType = it.mimeType
      }
    }
  }

  private fun setEditedAnimationFileData() {
    fileType = ANIMATION
    val content = messageContent as TdApi.MessageAnimation?
    content?.animation?.animation?.also { setEditedFileFileData(it) }
    content?.animation?.also {
      if(fileData?.name == null && it.fileName?.isNotBlank() == true) {
        fileData?.name = it.fileName
      }
      if(fileData?.mimeType == null && it.mimeType?.isNotBlank() == true) {
        fileData?.mimeType = it.mimeType
      }
    }
  }

  private fun setEditedPhotoFileData() {
    fileType = PHOTO
    (messageContent as TdApi.MessagePhoto?)?.photo?.sizes?.let {
        var height = 0
        var width = 0
        for(photo in it) {
          if(photo != null && photo.height > height && photo.width > width) {
            height = photo.height
            width = photo.width
            setEditedFileFileData(photo.photo)
          }
        }
      }
  }

  private fun setEditedStickerFileData() {
    fileType = STICKER
    (messageContent as TdApi.MessageSticker?)
      ?.sticker?.sticker?.also { setEditedFileFileData(it) }
  }

  private fun setEditedFileFileData(file: TdApi.File) {
    fileData?.also {
      if(file.id != 0) {
        it.fileId = file.id
      }
      if(file.remote != null) {
        if(file.remote?.uniqueId?.isNotBlank() == true) {
          it.fileUniqueId = file.remote.uniqueId
        }
        if(
          file.remote?.isUploadingActive == false
          && file.remote?.isUploadingCompleted == true
        ) {
          isUploadingCompleted = true
          it.uploaded = true
          it.inProgress = false
          val localFile: File? = getLocalFile(it)
          if(localFile != null) {
            it.lastModified = localFile.lastModified()
          }
        }
      }
    }
  }

  //    void setMessageFileData(TdApi.Message message) {
  ////        FileData fileData = null;
  //        mFileData = mFileHelper.getFileByMsgId(mId);
  //        if(message.content != null){
  //            switch(message.content.getConstructor()){
  //                case TdApi.MessageDocument.CONSTRUCTOR:
  //                    setMessageDocumentFileData();
  //                    break;
  //                case TdApi.MessageVideo.CONSTRUCTOR:
  //                    setMessageVideoFileData();
  //                    break;
  //                case TdApi.MessageAudio.CONSTRUCTOR:
  //                    setMessageAudioFileData();
  //                    break;
  //                case TdApi.MessagePhoto.CONSTRUCTOR:
  //                    setMessagePhotoFileData();
  //                    break;
  //                case TdApi.MessageAnimation.CONSTRUCTOR:
  //                    setMessageAnimationFileData();
  //                    break;
  //                case TdApi.MessageSticker.CONSTRUCTOR:
  //                    setMessageStickerFileData();
  //                    break;
  //                default:
  //                    Log.d(TAG, "Receive an unknown message type");
  //            }
  //        }
  //        File localFile = getFile();
  //        if(mFileData != null){
  //            if(localFile == null){
  //                mFileData.setDownloaded(false);
  //            } else {
  //                if(mFileData.getLastModified() != 0 && localFile.lastModified() != mFileData.getLastModified()){
  //                    mFileData.setUploaded(false);
  //                }
  //            }
  //            if(!mSettingsHelper.getSettings().downloadMissing()){
  //                mFileData.setDownloaded(true);
  //            }
  //        }
  ////        if(mFileData != null){
  ////            if(mFileData.path != null){
  ////                String fullPath = Fs.getFileAbsPath(mFileData.path);
  ////                File file;
  ////                if(fullPath != null){
  ////                    file = new File(fullPath);
  ////                    if(!file.exists()){
  ////                        mFileData.setDownloaded(false);
  ////                    } else {
  ////                        System.out.println("getLastModified " + mFileData.getLastModified());
  ////                        System.out.println("getLastModified " + file.lastModified());
  ////                        if(mFileData.getLastModified() != 0 && file.lastModified() != mFileData.getLastModified()){
  ////                            System.out.println("addFile");
  ////                            mFileData.setUploaded(false);
  //////                            mFileData.setLastModified(file.lastModified());
  ////                        }
  ////                    }
  ////                }
  ////            }
  ////            if(!mSettingsHelper.getSettings().downloadMissing()){
  ////                mFileData.setDownloaded(true);
  ////            }
  ////        }
  ////        if(mFileData.uploaded && mFileData.isDownloaded()){
  ////            mFileData.setInProgress(false);
  ////        }
  //    }
  //    void setMessageDocumentFileData(){
  //        mType = DOCUMENT;
  //        TdApi.MessageDocument content = null;
  //        if(mMessage != null && mMessage.content != null){
  //            content = (TdApi.MessageDocument) mMessage.content;
  //        }
  //        if(mFileData == null
  //            && content != null
  //            && content.caption != null
  //            && content.caption.text != null
  //            && !content.caption.text.trim().equals("")
  //        ){
  //            mFileData = mFileHelper.getFileByPath(content.caption.text.trim());
  //        }
  //        if(content != null && content.document != null){
  //            if(content.document.document != null){
  //                setMessageFileFileData(content.document.document);
  //            }
  //            if(mFileData != null){
  //                if(mFileData.name == null && content.document.fileName != null && !content.document.fileName.trim().equals("")){
  //                    mFileData.setName(content.document.fileName);
  //                }
  //                if(mFileData.mimeType == null && content.document.mimeType != null && !content.document.mimeType.trim().equals("")){
  //                    mFileData.setMimeType(content.document.mimeType);
  //                }
  //                if(mFileData.path == null && content.caption != null && content.caption.text != null && !content.caption.text.trim().equals("")){
  //                    mFileData.setPath(content.caption.text.trim());
  //                }
  //            }
  //        }
  //    }
  //    void setMessageVideoFileData(){
  //        mType = VIDEO;
  //        TdApi.MessageVideo content = null;
  //        if(mMessage != null && mMessage.content != null){
  //            content = (TdApi.MessageVideo) mMessage.content;
  //        }
  //        if(mFileData == null
  //            && content != null
  //            && content.caption != null
  //            && content.caption.text != null
  //            && !content.caption.text.trim().equals("")
  //        ){
  //            mFileData = mFileHelper.getFileByPath(content.caption.text.trim());
  //        }
  //        if(content != null && content.video != null){
  //            if(content.video.video != null){
  //                setMessageFileFileData(content.video.video);
  //            }
  //            if(mFileData != null){
  //                if(mFileData.name == null && content.video.fileName != null && !content.video.fileName.trim().equals("")){
  //                    mFileData.setName(content.video.fileName);
  //                }
  //                if(mFileData.mimeType == null && content.video.mimeType != null && !content.video.mimeType.trim().equals("")){
  //                    mFileData.setMimeType(content.video.mimeType);
  //                }
  //                if(mFileData.path == null && content.caption != null && content.caption.text != null && !content.caption.text.trim().equals("")){
  //                    mFileData.setPath(content.caption.text.trim());
  //                }
  //            }
  //        }
  //    }
  //    void setMessageAudioFileData(){
  //        mType = AUDIO;
  //        TdApi.MessageAudio content = null;
  //        if(mMessage != null && mMessage.content != null){
  //            content = (TdApi.MessageAudio) mMessage.content;
  //        }
  //        if(mFileData == null
  //            && content != null
  //            && content.caption != null
  //            && content.caption.text != null
  //            && !content.caption.text.trim().equals("")
  //        ){
  //            mFileData = mFileHelper.getFileByPath(content.caption.text.trim());
  //        }
  //        if(content != null && content.audio != null){
  //            if(content.audio.audio != null){
  //                setMessageFileFileData(content.audio.audio);
  //            }
  //            if(mFileData != null){
  //                if(mFileData.name == null && content.audio.fileName != null && !content.audio.fileName.trim().equals("")){
  //                    mFileData.setName(content.audio.fileName);
  //                }
  //                if(mFileData.mimeType == null && content.audio.mimeType != null && !content.audio.mimeType.trim().equals("")){
  //                    mFileData.setMimeType(content.audio.mimeType);
  //                }
  //                if(mFileData.path == null && content.caption != null && content.caption.text != null && !content.caption.text.trim().equals("")){
  //                    mFileData.setPath(content.caption.text.trim());
  //                }
  //            }
  //        }
  //    }
  //    void setMessageAnimationFileData(){
  //        mType = ANIMATION;
  //        TdApi.MessageAnimation content = null;
  //        if(mMessage != null && mMessage.content != null){
  //            content = (TdApi.MessageAnimation) mMessage.content;
  //        }
  //        if(mFileData == null
  //            && content != null
  //            && content.caption != null
  //            && content.caption.text != null
  //            && !content.caption.text.trim().equals("")
  //        ){
  //            mFileData = mFileHelper.getFileByPath(content.caption.text.trim());
  //        }
  //        if(content != null && content.animation != null){
  //            if(content.animation.animation != null){
  //                setMessageFileFileData(content.animation.animation);
  //            }
  //            if(mFileData != null){
  //                if(mFileData.name == null && content.animation.fileName != null && !content.animation.fileName.trim().equals("")){
  //                    mFileData.setName(content.animation.fileName);
  //                }
  //                if(mFileData.mimeType == null && content.animation.mimeType != null && !content.animation.mimeType.trim().equals("")){
  //                    mFileData.setMimeType(content.animation.mimeType);
  //                }
  //                if(mFileData.path == null && content.caption != null && content.caption.text != null && !content.caption.text.trim().equals("")){
  //                    mFileData.setPath(content.caption.text.trim());
  //                }
  //            }
  //        }
  //    }
  //    void setMessagePhotoFileData(){
  //        mType = PHOTO;
  //        TdApi.MessagePhoto content = (TdApi.MessagePhoto) mMessage.content;
  //        if(mFileData == null
  //            && content != null
  //            && content.caption != null
  //            && content.caption.text != null
  //            && !content.caption.text.trim().equals("")
  //        ){
  //            mFileData = mFileHelper.getFileByPath(content.caption.text.trim());
  //        }
  //        if(
  //            content != null
  //                && content.photo != null
  //                && content.photo.sizes != null
  //                && content.photo.sizes.length > 0
  //        ){
  //            int height = 0;
  //            int width = 0;
  //            for(TdApi.PhotoSize photo : content.photo.sizes) {
  //                if(photo.height > height && photo.width > width){
  //                    height = photo.height;
  //                    width = photo.width;
  //                    setMessageFileFileData(photo.photo);
  //                    Log.d(TAG, "width " + width + " " + "height " + height);
  //                }
  //            }
  //        }
  //
  //        if(mFileData != null && mFileData.path == null && content != null && content.caption != null && content.caption.text != null && !content.caption.text.trim().equals("")){
  //            mFileData.setPath(content.caption.text.trim());
  //        }
  //    }
  //    void setMessageStickerFileData(){
  //        mType = STICKER;
  //        TdApi.MessageSticker content = null;
  //        if(mMessage != null && mMessage.content != null){
  //            content = (TdApi.MessageSticker) mMessage.content;
  //        }
  //        if(content != null && content.sticker != null){
  //            if(content.sticker.sticker != null){
  //                setMessageFileFileData(content.sticker.sticker);
  //            }
  //        }
  //    }
  //    void setMessageFileFileData(TdApi.File file){
  //        if(file != null){
  //            if(mFileData == null){
  //                mFileData = new FileData();
  //            }
  //            if(mId != 0){
  //                mFileData.setMessageId(mId);
  //            }
  //            if(file.id != 0){
  //                mFileData.setFileId(file.id);
  //            }
  //            if(file.remote != null){
  //                if(file.remote.uniqueId != null
  //                    && !file.remote.uniqueId.trim().equals("")
  //                    && mFileData.getFileUniqueId() == null
  //                ){
  //                    mFileData.setFileUniqueId(file.remote.uniqueId);
  //                }
  //                if(!file.remote.isUploadingActive && file.remote.isUploadingCompleted){
  //                    mUploadingCompleted = true;
  //                    mFileData.setUploaded(true);
  //                    mFileData.setInProgress(false);
  //                }
  //            }
  //        }
  //    }

  internal constructor(messageMap: MutableMap<Long, TdApi.Message>) : this() {
    currentChatId = Settings.chatId
    updateFileDataMap(messageMap)
  }

  private fun updateFileDataMap(messageMap: Map<Long, TdApi.Message>) {
    for(msg in messageMap.values) {
      message = msg
      messageId = 0
      chatId = 0
      fileData = null
      setMapFileData()
    }
    for(data in Sync.fileByPathMap.values) {
      val path: String? = data.path
      if(path != null) {
        val absPath = Fs.getAbsPath(path)
        if(absPath != null) {
          val file = File(absPath)
          if(file.exists()) {
            data.messageId = 0
            data.fileId = 0
            data.fileUniqueId = null
            data.uploaded = false
          } else {
            FileHelper.delete(data)
            Sync.fileByPathMap.remove(path)
          }
        }
      }
    }
  }

  private fun setMapFileData() {
    var path: String?
    val msg = message
    if(msg != null && msg.id != 0L && msg.chatId != 0L) {
      messageId = msg.id
      if(currentChatId != 0L && currentChatId == msg.chatId) {
        chatId = msg.chatId
      }
      path = Sync.pathByMsgIdMap[msg.id]
      if(path != null && Sync.fileByPathMap.containsKey(path)) {
        fileData = Sync.fileByPathMap[path]
      }
    }
    if(msg?.content != null) {
      when(msg.content.constructor) {
        TdApi.MessageDocument.CONSTRUCTOR -> setMapDocumentFileData()
        TdApi.MessageVideo.CONSTRUCTOR -> setMapVideoFileData()
        TdApi.MessageAudio.CONSTRUCTOR -> setMapAudioFileData()
        TdApi.MessagePhoto.CONSTRUCTOR -> setMapPhotoFileData()
        TdApi.MessageAnimation.CONSTRUCTOR -> setMapAnimationFileData()
        TdApi.MessageSticker.CONSTRUCTOR -> setMapStickerFileData()
        else -> Log.d(TAG, "Receive an unknown message type")
      }
    }
    val fileData = fileData
    val localFile = fileData?.let { getLocalFile(it) }
    if(fileData != null) {
      if(fileData.messageId == 0L && messageId != 0L) {
        fileData.messageId = messageId
        putToUpdateMap()
      }
      if(fileData.chatId == 0L && chatId != 0L) {
        fileData.chatId = chatId
        putToUpdateMap()
      }
      if(localFile == null) {
        if(fileData.downloaded && Settings.downloadMissing == true) {
          fileData.downloaded = false
          putToUpdateMap()
        }
        if(!fileData.downloaded && Settings.downloadMissing == false) {
          fileData.downloaded = true
          putToUpdateMap()
        }
      } else {
        if(localFile.lastModified() != fileData.lastModified) {
          fileData.uploaded = false
          putToUpdateMap()
        }
        if(!fileData.downloaded) {
          fileData.downloaded = true
          putToUpdateMap()
        }
      }
      path = fileData.path
      if(path != null) {
        Sync.fileByPathMap.remove(path)
      }
    }
  }

  private fun setMapDocumentFileData() {
    fileType = DOCUMENT
    val content = message?.content?.let { it as TdApi.MessageDocument }
    val path: String? = content?.caption?.text?.let { getPathFromCaption(it) }
    if(fileData == null && path != null) {
      fileData = Sync.fileByPathMap[path]
      putToUpdateMap()
    }
    content?.document?.document?.let { setMapFileFileData(it) }
    content?.document?.let { file ->
      fileData?.let {
        if(it.name == null && file.fileName != null && file.fileName.isNotBlank()) {
          it.name = file.fileName
        }
        if(it.mimeType == null && file.mimeType != null && file.mimeType.isNotBlank()) {
          it.mimeType = file.mimeType
        }
        if(it.path == null && path != null) {
          it.path = path
        }
      }
    }
  }

  private fun setMapVideoFileData() {
    fileType = VIDEO
    val content = message?.content?.let { it as TdApi.MessageVideo }
    val path: String? = content?.caption?.text?.let { getPathFromCaption(it) }
    if(fileData == null && path != null) {
      fileData = Sync.fileByPathMap[path]
      putToUpdateMap()
    }
    content?.video?.video?.let { setMapFileFileData(it) }
    content?.video?.let { file ->
      fileData?.let {
        if(it.name == null && file.fileName != null && file.fileName.isNotBlank()) {
          it.name = file.fileName
        }
        if(it.mimeType == null && file.mimeType != null && file.mimeType.isNotBlank()) {
          it.mimeType = file.mimeType
        }
        if(it.path == null && path != null) {
          it.path = path
        }
      }
    }
  }

  private fun setMapAudioFileData() {
    fileType = AUDIO
    val content = message?.content?.let { it as TdApi.MessageAudio }
    val path: String? = content?.caption?.text?.let { getPathFromCaption(it) }
    if(fileData == null && path != null) {
      fileData = Sync.fileByPathMap[path]
      putToUpdateMap()
    }
    content?.audio?.audio?.let { setMapFileFileData(it) }
    content?.audio?.let { file ->
      fileData?.let {
        if(it.name == null && file.fileName != null && file.fileName.isNotBlank()) {
          it.name = file.fileName
        }
        if(it.mimeType == null && file.mimeType != null && file.mimeType.isNotBlank()) {
          it.mimeType = file.mimeType
        }
        if(it.path == null && path != null) {
          it.path = path
        }
      }
    }
  }

  private fun setMapAnimationFileData() {
    fileType = ANIMATION
    val content = message?.content?.let { it as TdApi.MessageAnimation }
    val path: String? = content?.caption?.text?.let { getPathFromCaption(it) }
    if(fileData == null && path != null) {
      fileData = Sync.fileByPathMap[path]
      putToUpdateMap()
    }
    content?.animation?.animation?.let { setMapFileFileData(it) }
    content?.animation?.let { file ->
      fileData?.let {
        if(it.name == null && file.fileName != null && file.fileName.isNotBlank()) {
          it.name = file.fileName
        }
        if(it.mimeType == null && file.mimeType != null && file.mimeType.isNotBlank()) {
          it.mimeType = file.mimeType
        }
        if(it.path == null && path != null) {
          it.path = path
        }
      }
    }
  }

  private fun setMapPhotoFileData() {
    fileType = PHOTO
    val content = message?.content?.let { it as TdApi.MessagePhoto }
    val path: String? = content?.caption?.text?.let { getPathFromCaption(it) }
      if(fileData == null && path != null) {
        fileData = Sync.fileByPathMap[path]
        putToUpdateMap()
      }
    content?.photo?.sizes?.let {
      var height = 0
      var width = 0
      for(photo in it) {
        if(photo != null && photo.height > height && photo.width > width) {
          height = photo.height
          width = photo.width
          setMapFileFileData(photo.photo)
        }
      }
    }
    if(path != null) {
      fileData?.let {
        if(it.path == null) it.path = path
      }
    }
  }

  private fun setMapStickerFileData() {
    fileType = STICKER
    val content = message?.content?.let { it as TdApi.MessageSticker }
    content?.sticker?.sticker?.also { setMapFileFileData(it) }
  }

  private fun setMapFileFileData(tdFile: TdApi.File) {
    val file: FileData = fileData ?: FileData()
    fileData = fileData ?: file
    if(tdFile.id != 0 && tdFile.id != file.fileId) {
      file.fileId = tdFile.id
      putToUpdateMap()
    }
    tdFile.remote?.also {
      if(it.uniqueId != null && it.uniqueId.isNotBlank() && it.uniqueId != file.fileUniqueId) {
        file.fileUniqueId = it.uniqueId
        putToUpdateMap()
        if(file.name == null && file.path == null && file.mimeType == null && fileType === STICKER) {
          file.name = it.uniqueId + ".webp"
          file.path = "webp_stickers/${it.uniqueId}.webp"
          file.mimeType = "image/webp"
          file.uploaded = true
        }
      }
      if(!it.isUploadingActive && it.isUploadingCompleted && (!file.uploaded || file.inProgress)) {
        file.uploaded = true
        file.inProgress = false
        putToUpdateMap()
      }
    }
  }

  private fun putToUpdateMap() {
//        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
//            System.out.println(ste);
//        }
    val msgId = messageId
    val fd = fileData
    if(msgId != 0L && fd != null) {
      if(!Sync.updateMap.containsKey(msgId)) {
        Sync.updateMap[msgId] = fd
      }
    }
  }

  fun commitUpdates() {
    if(Sync.updateMap.isNotEmpty()) {
      for(file in Sync.updateMap.values) {
        fileData = file
        addFile()
      }
      Sync.updateMap.clear()
    }
    if(Sync.fileByPathMap.isNotEmpty()) {
      for(file in Sync.fileByPathMap.values) {
        fileData = file
        addFile()
      }
      Sync.fileByPathMap.clear()
    }
  }

  private fun getLocalFile(fileData: FileData): File? {
    var file: File? = fileData.path?.let { relativeFilePath ->
      Fs.getAbsPath(relativeFilePath)?.let { File(it) }
    }
    if(file != null && !file.exists()) {
      file = null
    }
    return file
  }

  fun addFile() {
    val fileData = fileData
    val path = fileData?.path
    if(fileData != null) {
      if(fileData.messageId != 0L && FileHelper.getFileByMsgId(fileData.messageId) != null) {
        FileHelper.updateFileByMsgId(fileData)
      } else if(path != null && FileHelper.getFileByPath(path) != null) {
        FileHelper.updateFileByPath(fileData)
      } else {
        FileHelper.addFile(fileData)
      }
    }
  }

  fun updateFile() {
    fileData?.let { FileHelper.updateFile(it) }
  }

  companion object {
    fun getPathFromCaption(caption: String): String? {
      return Regex("""$FOLDER_ICON[^\r\n\t]*""", RegexOption.MULTILINE)
        .find(caption)
        ?.value
        ?.replace(FOLDER_ICON, "")
    }
  }

}
