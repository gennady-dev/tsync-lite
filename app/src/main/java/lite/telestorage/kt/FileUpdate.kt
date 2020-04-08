package lite.telestorage.kt


import android.content.Context
import android.util.Log
import lite.telestorage.kt.database.FileHelper
import lite.telestorage.kt.database.SettingsHelper
import lite.telestorage.kt.models.FileData
import lite.telestorage.kt.models.Settings
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File
import java.util.Date


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
  var mType: String? = null
  var messageId: Long = 0
  var chatId: Long = 0
  var mCurrentChatId: Long = 0
  var message: TdApi.Message? = null
  var mContent: TdApi.MessageContent? = null
  var mDocument: TdApi.MessageDocument? = null
  var mVideo: TdApi.MessageVideo? = null
  var mAudio: TdApi.MessageAudio? = null
  var mPhoto: TdApi.MessagePhoto? = null
  var mAnimation: TdApi.MessageAnimation? = null

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

  internal constructor(updateFile: TdApi.UpdateFile?) : this() {}

  fun setContext(context: Context?) {
    mContext = context
  }

  fun setFileData(file: FileData?) {
    fileData = file
  }

  internal constructor(fileUpdate: TdApi.UpdateMessageSendSucceeded) : this() {
    if(
      settingsHelper?.settings != null
      && fileHelper != null
      && fileUpdate.message != null
      && fileUpdate.message.id != 0L
      && fileUpdate.message.chatId != 0L
    ) {
      message = fileUpdate.message
      chatId = fileUpdate.message.chatId
      messageId = fileUpdate.message.id
      setUploadedFileData(message)
    }
  }

  fun setUploadedFileData(msg: TdApi.Message?) {
    if(msg != null && msg.content != null) {
      when(msg.content.constructor) {
        TdApi.MessageDocument.CONSTRUCTOR -> setUploadedDocumentFileData()
        TdApi.MessageVideo.CONSTRUCTOR -> setUploadedVideoFileData()
        TdApi.MessageAudio.CONSTRUCTOR -> setUploadedAudioFileData()
        TdApi.MessagePhoto.CONSTRUCTOR -> setUploadedPhotoFileData()
        TdApi.MessageAnimation.CONSTRUCTOR -> setUploadedAnimationFileData()
        TdApi.MessageSticker.CONSTRUCTOR -> setUploadedStickerFileData()
        else -> Log.d(TAG, "Receive an unknown message type")
      }
      if(fileData != null) {
        if(fileData.messageId == 0L && messageId != 0L) {
          fileData.messageId = messageId
        }
        if(fileData.chatId == 0L && chatId != 0L) {
          fileData.chatId = chatId
        }
      }
    }
  }

  fun setUploadedDocumentFileData() {
    mType = DOCUMENT
    val content = message!!.content as TdApi.MessageDocument
    if(content != null && content.document != null) {
      if(content.document.document != null) {
        setFileFileData(content.document.document)
      }
      if(fileData != null) {
        if(fileData.name == null && content.document.fileName != null && content.document.fileName.trim { it <= ' ' } != "") {
          fileData.setName(content.document.fileName)
        }
        if(fileData.mimeType == null && content.document.mimeType != null && content.document.mimeType.trim { it <= ' ' } != "") {
          fileData.setMimeType(content.document.mimeType)
        }
      }
    }
  }

  fun setUploadedVideoFileData() {
    mType = VIDEO
    val content = message!!.content as TdApi.MessageVideo
    if(content != null && content.video != null) {
      if(content.video.video != null) {
        setFileFileData(content.video.video)
      }
      if(fileData != null) {
        if(fileData.name == null && content.video.fileName != null && content.video.fileName.trim { it <= ' ' } != "") {
          fileData.setName(content.video.fileName)
        }
        if(fileData.mimeType == null && content.video.mimeType != null && content.video.mimeType.trim { it <= ' ' } != "") {
          fileData.setMimeType(content.video.mimeType)
        }
      }
    }
  }

  fun setUploadedAudioFileData() {
    mType = AUDIO
    val content = message!!.content as TdApi.MessageAudio
    if(content != null && content.audio != null) {
      if(content.audio.audio != null) {
        setFileFileData(content.audio.audio)
      }
      if(fileData != null) {
        if(fileData.name == null && content.audio.fileName != null && content.audio.fileName.trim { it <= ' ' } != "") {
          fileData.setName(content.audio.fileName)
        }
        if(fileData.mimeType == null && content.audio.mimeType != null && content.audio.mimeType.trim { it <= ' ' } != "") {
          fileData.setMimeType(content.audio.mimeType)
        }
      }
    }
  }

  fun setUploadedAnimationFileData() {
    mType = ANIMATION
    val content = message!!.content as TdApi.MessageAnimation
    if(content != null && content.animation != null) {
      if(content.animation.animation != null) {
        setFileFileData(content.animation.animation)
      }
      if(fileData != null) {
        if(fileData.name == null && content.animation.fileName != null && content.animation.fileName.trim { it <= ' ' } != "") {
          fileData.setName(content.animation.fileName)
        }
        if(fileData.mimeType == null && content.animation.mimeType != null && content.animation.mimeType.trim { it <= ' ' } != "") {
          fileData.setMimeType(content.animation.mimeType)
        }
      }
    }
  }

  fun setUploadedPhotoFileData() {
    mType = PHOTO
    val content = message!!.content as TdApi.MessagePhoto
    if(content != null && content.photo != null && content.photo.sizes != null && content.photo.sizes.size > 0) {
      var height = 0
      var width = 0
      for(photo in content.photo.sizes) {
        if(photo.height > height && photo.width > width) {
          height = photo.height
          width = photo.width
          setFileFileData(photo.photo)
          Log.d(TAG, "width $width height $height")
        }
      }
    }
  }

  fun setUploadedStickerFileData() {
    mType = STICKER
    var content: TdApi.MessageSticker? = null
    if(message != null && message!!.content != null) {
      content = message!!.content as TdApi.MessageSticker
    }
    if(content != null && content.sticker != null) {
      if(content.sticker.sticker != null) {
        setFileFileData(content.sticker.sticker)
      }
    }
  }

  fun setFileFileData(file: TdApi.File?) {
    if(file != null) {
      val syncFolder: String? = Fs.storageAbsPath
      if(file.local != null && file.local.path != null && file.local.path.trim { it <= ' ' } != "" && syncFolder != null) {
        val responsePath = file.local.path.replace("$syncFolder/", "")
        fileData = fileHelper.getFileByPath(responsePath)
      }
      if(fileData != null) {
        if(file.id != 0) {
          fileData.setFileId(file.id)
        }
        if(file.remote != null) {
          if(file.remote.uniqueId != null && file.remote.uniqueId.trim { it <= ' ' } != "") {
            fileData.setFileUniqueId(file.remote.uniqueId)
          }
          if(!file.remote.isUploadingActive && file.remote.isUploadingCompleted) {
            isUploadingCompleted = true
            fileData.setUploaded(true)
            fileData.setInProgress(false)
            val localFile: java.io.File? = file
            if(localFile != null) {
              if(settingsHelper != null) {
                val settings: Settings = settingsHelper.getSettings()
                if(settings != null && settings.deleteUploaded()) {
                  localFile.delete()
                }
              }
              if(localFile.exists()) {
                fileData.setLastModified(localFile.lastModified())
              }
            }
          }
        }
        fileData.setUpdated(Date())
      }
    }
  }

  internal constructor(fileUpdate: TdApi.UpdateMessageContent?) : this() {
    if(settingsHelper != null && settingsHelper.getSettings() != null && fileHelper != null && fileUpdate != null && fileUpdate.newContent != null && fileUpdate.messageId != 0L) {
      mContent = fileUpdate.newContent
      messageId = fileUpdate.messageId
      chatId = fileUpdate.chatId
      fileData = fileHelper.getFileByMsgId(messageId)
      if(fileData != null) {
        setEditedFileData()
      }
    }
  }

  internal constructor(msg: TdApi.Message?) : this() {
    if(settingsHelper != null && settingsHelper.getSettings() != null && fileHelper != null && msg != null && msg.content != null && msg.id != 0L) {
      mContent = msg.content
      messageId = msg.id
      chatId = msg.chatId
      fileData = fileHelper.getFileByMsgId(messageId)
      if(fileData != null) {
        setEditedFileData()
      }
    }
  }

  fun setEditedFileData() {
    if(mContent != null) {
      when(mContent!!.constructor) {
        TdApi.MessageDocument.CONSTRUCTOR -> setEditedDocumentFileData()
        TdApi.MessageVideo.CONSTRUCTOR -> setEditedVideoFileData()
        TdApi.MessageAudio.CONSTRUCTOR -> setEditedAudioFileData()
        TdApi.MessagePhoto.CONSTRUCTOR -> setEditedPhotoFileData()
        TdApi.MessageAnimation.CONSTRUCTOR -> setEditedAnimationFileData()
        TdApi.MessageSticker.CONSTRUCTOR -> setEditedStickerFileData()
        else -> Log.d(TAG, "Receive an unknown message type")
      }
      if(fileData != null) {
        if(fileData.messageId === 0 && messageId != 0L) {
          fileData.setMessageId(messageId)
        }
        if(fileData.getChatId() === 0 && chatId != 0L) {
          fileData.setChatId(chatId)
        }
      }
    }
  }

  fun setEditedDocumentFileData() {
    mType = DOCUMENT
    val content = mContent as TdApi.MessageDocument?
    if(content != null && content.document != null) {
      if(content.document.document != null) {
        setEditedFileFileData(content.document.document)
      }
      if(fileData.name == null && content.document.fileName != null && content.document.fileName.trim { it <= ' ' } != "") {
        fileData.setName(content.document.fileName)
      }
      if(fileData.mimeType == null && content.document.mimeType != null && content.document.mimeType.trim { it <= ' ' } != "") {
        fileData.setMimeType(content.document.mimeType)
      }
    }
  }

  fun setEditedVideoFileData() {
    mType = VIDEO
    val content = mContent as TdApi.MessageVideo?
    if(content != null && content.video != null) {
      if(content.video.video != null) {
        setEditedFileFileData(content.video.video)
      }
      if(fileData.name == null && content.video.fileName != null && content.video.fileName.trim { it <= ' ' } != "") {
        fileData.setName(content.video.fileName)
      }
      if(fileData.mimeType == null && content.video.mimeType != null && content.video.mimeType.trim { it <= ' ' } != "") {
        fileData.setMimeType(content.video.mimeType)
      }
    }
  }

  fun setEditedAudioFileData() {
    mType = AUDIO
    val content = mContent as TdApi.MessageAudio?
    if(content != null && content.audio != null) {
      if(content.audio.audio != null) {
        setEditedFileFileData(content.audio.audio)
      }
      if(fileData.name == null && content.audio.fileName != null && content.audio.fileName.trim { it <= ' ' } != "") {
        fileData.setName(content.audio.fileName)
      }
      if(fileData.mimeType == null && content.audio.mimeType != null && content.audio.mimeType.trim { it <= ' ' } != "") {
        fileData.setMimeType(content.audio.mimeType)
      }
    }
  }

  fun setEditedAnimationFileData() {
    mType = ANIMATION
    val content = mContent as TdApi.MessageAnimation?
    if(content != null && content.animation != null) {
      if(content.animation.animation != null) {
        setEditedFileFileData(content.animation.animation)
      }
      if(fileData.name == null && content.animation.fileName != null && content.animation.fileName.trim { it <= ' ' } != "") {
        fileData.setName(content.animation.fileName)
      }
      if(fileData.mimeType == null && content.animation.mimeType != null && content.animation.mimeType.trim { it <= ' ' } != "") {
        fileData.setMimeType(content.animation.mimeType)
      }
    }
  }

  fun setEditedPhotoFileData() {
    mType = PHOTO
    val content = mContent as TdApi.MessagePhoto?
    if(content != null && content.photo != null && content.photo.sizes != null && content.photo.sizes.size > 0) {
      var height = 0
      var width = 0
      for(photo in content.photo.sizes) {
        if(photo.height > height && photo.width > width) {
          height = photo.height
          width = photo.width
          setEditedFileFileData(photo.photo)
          Log.d(TAG, "width $width height $height")
        }
      }
    }
  }

  fun setEditedStickerFileData() {
    mType = STICKER
    val content = mContent as TdApi.MessageSticker?
    if(content != null && content.sticker != null) {
      if(content.sticker.sticker != null) {
        setEditedFileFileData(content.sticker.sticker)
      }
    }
  }

  fun setEditedFileFileData(file: File?) {
    if(file != null) {
      if(file.id != 0) {
        fileData.setFileId(file.id)
      }
      if(file.remote != null) {
        if(file.remote.uniqueId != null && file.remote.uniqueId.trim { it <= ' ' } != "") {
          fileData.setFileUniqueId(file.remote.uniqueId)
        }
        if(!file.remote.isUploadingActive && file.remote.isUploadingCompleted) {
          isUploadingCompleted = true
          fileData.setUploaded(true)
          fileData.setInProgress(false)
          val localFile: java.io.File? = file
          if(localFile != null) {
            fileData.setLastModified(localFile.lastModified())
          }
        }
      }
      fileData.setUpdated(Date())
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
    if(settingsHelper != null && settingsHelper.getSettings() != null && fileHelper != null && messageMap != null && Sync.fileByPathMap != null && Sync.sPathByIdMap != null && Sync.updateMap != null) {
      mCurrentChatId = settingsHelper.getSettings().getChatId()
      updateFileDataMap(messageMap)
    }
  }

  fun updateFileDataMap(messageMap: Map<Long, TdApi.Message>) {
    for(msg in messageMap.values) {
      message = msg
      messageId = 0
      chatId = 0
      fileData = null
      setMapFileData()
    }
    for(data in Sync.fileByPathMap.values) {
      val path: String? = data?.path
      if(path != null) {
        val absPath = Fs.getFileAbsPath(path)
        if(absPath != null) {
          val file = java.io.File(absPath)
          if(file.exists()) {
            data.messageId = 0
            data.id = 0
            data.uniqueId = null
            data.uploaded = false
          } else {
            fileHelper?.delete(data)
            Sync.fileByPathMap?.remove(path)
          }
        }
      }
    }
  }

  fun setMapFileData() {
    var path: String
    if(message != null && message!!.id != 0L && message!!.chatId != 0L) {
      messageId = message!!.id
      if(mCurrentChatId != 0L && mCurrentChatId == message!!.chatId) {
        chatId = message!!.chatId
      }
      path = Sync.pathByMsgIdMap?.get(message!!.id)
      if(path != null && Sync.fileByPathMap.containsKey(path)) {
        fileData = Sync.fileByPathMap.get(path)
      }
      println("message id $messageId $path")
    }
    if(message!!.content != null) {
      when(message!!.content.constructor) {
        TdApi.MessageDocument.CONSTRUCTOR -> setMapDocumentFileData()
        TdApi.MessageVideo.CONSTRUCTOR -> setMapVideoFileData()
        TdApi.MessageAudio.CONSTRUCTOR -> setMapAudioFileData()
        TdApi.MessagePhoto.CONSTRUCTOR -> setMapPhotoFileData()
        TdApi.MessageAnimation.CONSTRUCTOR -> setMapAnimationFileData()
        TdApi.MessageSticker.CONSTRUCTOR -> setMapStickerFileData()
        else -> Log.d(TAG, "Receive an unknown message type")
      }
    }
    val localFile = file
    if(fileData != null) {
      if(fileData.messageId === 0 && messageId != 0L) {
        fileData.setMessageId(messageId)
        putToUpdateMap()
      }
      if(fileData.getChatId() === 0 && chatId != 0L) {
        fileData.setChatId(chatId)
        putToUpdateMap()
      }
      if(localFile == null) {
        if(fileData.isDownloaded() && settingsHelper.getSettings().downloadMissing()) {
          fileData.setDownloaded(false)
          putToUpdateMap()
        }
        if(!fileData.isDownloaded() && !settingsHelper.getSettings().downloadMissing()) {
          fileData.setDownloaded(true)
          putToUpdateMap()
        }
      } else {
        if(localFile.lastModified() != fileData.getLastModified()) {
          println("localFile.lastModified " + localFile.lastModified() + " mFileData.getLastModified " + fileData.getLastModified())
          fileData.setUploaded(false)
          putToUpdateMap()
        }
        if(!fileData.isDownloaded()) {
          fileData.setDownloaded(true)
          putToUpdateMap()
        }
      }
      path = fileData.path
      if(path != null) {
        Sync.fileByPathMap.remove(path)
      }
    }
  }

  fun setMapDocumentFileData() {
    mType = DOCUMENT
    var content: TdApi.MessageDocument? = null
    if(message != null && message!!.content != null) {
      content = message!!.content as TdApi.MessageDocument
    }
    if(content != null) {
      var path: String? = null
      if(content.caption != null) {
        path = getPathFromCaption(content.caption.text)
      }
      if(fileData == null && path != null) {
        fileData = Sync.fileByPathMap.get(path)
        putToUpdateMap()
      }
      if(content.document != null) {
        if(content.document.document != null) {
          setMapFileFileData(content.document.document)
        }
        if(fileData != null) {
          if(fileData.name == null && content.document.fileName != null && content.document.fileName.trim { it <= ' ' } != "") {
            fileData.setName(content.document.fileName)
          }
          if(fileData.mimeType == null && content.document.mimeType != null && content.document.mimeType.trim { it <= ' ' } != "") {
            fileData.setMimeType(content.document.mimeType)
          }
          if(fileData.path == null && path != null) {
            fileData.setPath(path)
          }
        }
      }
    }
  }

  fun setMapVideoFileData() {
    mType = VIDEO
    var content: TdApi.MessageVideo? = null
    if(message != null && message!!.content != null) {
      content = message!!.content as TdApi.MessageVideo
    }
    if(content != null) {
      var path: String? = null
      if(content.caption != null) {
        path = getPathFromCaption(content.caption.text)
      }
      if(fileData == null && path != null) {
        fileData = Sync.fileByPathMap.get(path)
        putToUpdateMap()
      }
      if(content.video != null) {
        if(content.video.video != null) {
          setMapFileFileData(content.video.video)
        }
        if(fileData != null) {
          if(fileData.name == null && content.video.fileName != null && content.video.fileName.trim { it <= ' ' } != "") {
            fileData.setName(content.video.fileName)
          }
          if(fileData.mimeType == null && content.video.mimeType != null && content.video.mimeType.trim { it <= ' ' } != "") {
            fileData.setMimeType(content.video.mimeType)
          }
          if(fileData.path == null && path != null) {
            fileData.setPath(path)
          }
        }
      }
    }
  }

  fun setMapAudioFileData() {
    mType = AUDIO
    var content: TdApi.MessageAudio? = null
    if(message != null && message!!.content != null) {
      content = message!!.content as TdApi.MessageAudio
    }
    if(content != null) {
      var path: String? = null
      if(content.caption != null) {
        path = getPathFromCaption(content.caption.text)
      }
      if(fileData == null && path != null) {
        fileData = Sync.fileByPathMap.get(path)
        putToUpdateMap()
      }
      if(content.audio != null) {
        if(content.audio.audio != null) {
          setMapFileFileData(content.audio.audio)
        }
        if(fileData != null) {
          if(fileData.name == null && content.audio.fileName != null && content.audio.fileName.trim { it <= ' ' } != "") {
            fileData.setName(content.audio.fileName)
          }
          if(fileData.mimeType == null && content.audio.mimeType != null && content.audio.mimeType.trim { it <= ' ' } != "") {
            fileData.setMimeType(content.audio.mimeType)
          }
          if(fileData.path == null && path != null) {
            fileData.setPath(path)
          }
        }
      }
    }
  }

  fun setMapAnimationFileData() {
    mType = ANIMATION
    var content: TdApi.MessageAnimation? = null
    if(message != null && message!!.content != null) {
      content = message!!.content as TdApi.MessageAnimation
    }
    if(content != null) {
      var path: String? = null
      if(content.caption != null) {
        path = getPathFromCaption(content.caption.text)
      }
      if(fileData == null && path != null) {
        fileData = Sync.fileByPathMap.get(path)
        putToUpdateMap()
      }
      if(content.animation != null) {
        if(content.animation.animation != null) {
          setMapFileFileData(content.animation.animation)
        }
        if(fileData != null) { //TODO change downloaded mime type to video/mp4
          if(fileData.name == null && content.animation.fileName != null && content.animation.fileName.trim { it <= ' ' } != "") {
            fileData.setName(content.animation.fileName)
            println("content.animation.fileName " + content.animation.fileName)
          }
          if(fileData.mimeType == null && content.animation.mimeType != null && content.animation.mimeType.trim { it <= ' ' } != "") {
            fileData.setMimeType(content.animation.mimeType)
            println("content.animation.mimeType " + content.animation.mimeType)
          }
          if(fileData.path == null && path != null) {
            fileData.setPath(path)
            println("path $path")
          }
        }
      }
    }
  }

  fun setMapPhotoFileData() {
    mType = PHOTO
    var content: TdApi.MessagePhoto? = null
    if(message != null && message!!.content != null) {
      content = message!!.content as TdApi.MessagePhoto
    }
    if(content != null) {
      var path: String? = null
      if(content.caption != null) {
        path = getPathFromCaption(content.caption.text)
      }
      if(fileData == null && path != null) {
        fileData = Sync.fileByPathMap.get(path)
        putToUpdateMap()
      }
      if(content.photo != null && content.photo.sizes != null && content.photo.sizes.size > 0) {
        var height = 0
        var width = 0
        for(photo in content.photo.sizes) {
          if(photo.height > height && photo.width > width) {
            height = photo.height
            width = photo.width
            setMapFileFileData(photo.photo)
            Log.d(TAG, "width $width height $height")
          }
        }
      }
      if(fileData != null && fileData.path == null && path != null) {
        fileData.setPath(path)
      }
    }
  }

  fun setMapStickerFileData() {
    mType = STICKER
    var content: TdApi.MessageSticker? = null
    if(message != null && message!!.content != null) {
      content = message!!.content as TdApi.MessageSticker
    }
    if(content != null && content.sticker != null) {
      if(content.sticker.sticker != null) {
        setMapFileFileData(content.sticker.sticker)
      }
    }
  }

  fun setMapFileFileData(file: TdApi.File?) {
    if(file != null) {
      if(fileData == null) {
        fileData = FileData()
        putToUpdateMap()
      }
      if(file.id != 0 && file.id != fileData.id) {
        fileData.id = file.id
        putToUpdateMap()
      }
      if(file.remote != null) {
        if(file.remote.uniqueId != null && file.remote.uniqueId.trim { it <= ' ' } != "" && file.remote.uniqueId != fileData.uniqueId) {
          fileData.uniqueId = file.remote.uniqueId
          putToUpdateMap()
          if(fileData.name == null && fileData.path == null && fileData.mimeType == null && mType === STICKER) {
            fileData.name = file.remote.uniqueId + ".webp"
            fileData.path = "webp_stickers/" + file.remote.uniqueId + ".webp"
            fileData.mimeType = "image/webp"
            fileData.uploaded = true
          }
        }
        if(!file.remote.isUploadingActive && file.remote.isUploadingCompleted && (!fileData.uploaded || fileData.inProgress)) {
          fileData.uploaded = true
          fileData.inProgress = false
          putToUpdateMap()
        }
      }
    }
  }

  fun putToUpdateMap() {
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

  val file: File?
    get() {
      var file: File? = null
      val path = fileData?.path
      if(path != null) {
        val fullPath = Fs.getFileAbsPath(path)
        if(fullPath != null) {
          file = File(fullPath)
          if(!file.exists()) {
            file = null
          }
        }
      }
      return file
    }

  fun addFile() {
    if(fileHelper != null && fileData != null) {
      println("addFile")
      if(fileData.messageId != 0L && fileHelper.getFileByMsgId(fileData.messageId) != null) {
        fileHelper.updateFileByMsgId(fileData)
        println("update file by message id")
      } else if(fileData.path != null && fileHelper.getFileByPath(fileData.path) != null) {
        fileHelper.updateFileByPath(fileData)
        println("update file by path")
      } else {
        println("addFile 2")
        fileHelper.addFile(fileData)
      }
      //
//
//            if(mSettingsHelper != null && mSettingsHelper.getSettings() != null && mSettingsHelper.getSettings().downloadMissing())
//            mFileHelper.addFile(mFileData);
    }
  }

  fun updateFile() {
    fileData?.let { fileHelper?.updateFile(it) }
  }

  companion object {
    fun getPathFromCaption(caption: String?): String? {
      var captionString: String? = null
      var path: String? = null
      if(caption != null && caption.trim { it <= ' ' } != "") {
        captionString = caption.trim { it <= ' ' }
      }
      if(captionString != null) {
        val captionArray = captionString.split("\n").toTypedArray()
        if(captionArray.size > 0 && captionArray[0] != "") {
          path = captionArray[0]
        }
      }
      return path
    }
  }

  init {
    fileHelper = FileHelper.get()
    settingsHelper = SettingsHelper.get()
  }
}
