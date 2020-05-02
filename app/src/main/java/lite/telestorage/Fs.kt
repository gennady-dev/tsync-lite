package lite.telestorage

import android.os.Build.VERSION
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLConnection

object Fs {

  val externalStoragePath: String?
    get() {
      Log.d("SDK_INT", VERSION.SDK_INT.toString())
      return if(VERSION.SDK_INT < 29) {
        Environment.getExternalStorageDirectory().path
      } else {
        val externalFilesDir: File? = ContextHolder.context?.getExternalFilesDir(MediaStore.VOLUME_EXTERNAL)
        externalFilesDir?.path?.replace("/" + Constants.externalAppFilesPath, "")
      }
    }

  val syncDirAbsPath: String?
    get() {
      val storagePath: String? = externalStoragePath
      val path: String? = Settings.path
      return if(storagePath != null && path != null) {
        "$storagePath/$path"
      } else null
    }

  fun getAbsPath(relativeFilePath: String): String? {
    return syncDirAbsPath?.let { "$it/$relativeFilePath" }
  }

  fun getRelPath(absPath: String): String? {
    return syncDirAbsPath?.let {
      if(absPath.matches("""${Regex.escape(it)}.+""".toRegex(RegexOption.DOT_MATCHES_ALL))) absPath.replace("$it/", "")
      else null
    }
  }

  @Throws(IOException::class)
  fun move(src: File, dst: File): Boolean {
    var moved = false
    val inChannel = FileInputStream(src).channel
    val outChannel = FileOutputStream(dst).channel
    try {
      inChannel.transferTo(0, inChannel.size(), outChannel)
    } catch(e: Exception) {
      println("FileHelper move Exception $e")
    } finally {
      inChannel.close()
      outChannel.close()
    }
    if(dst.length() == src.length()) {
      dst.setLastModified(src.lastModified())
      src.delete()
      moved = true
    }
    return moved
  }

  fun dirExist(path: String): Boolean {
    var exist = true
    val pathParts: Array<String> = path.split("/").toTypedArray()
    if(pathParts.size > 1 && syncDirAbsPath != null) {
      val dirRelPath = pathParts.copyOfRange(0, pathParts.size - 1).joinToString("/")
      val dir = File("$syncDirAbsPath/$dirRelPath")
      if(!dir.exists()) {
        exist = dir.mkdirs()
      } else if(!dir.isDirectory) {
        exist = false
      }
    }
    return exist
  }



//    fun getFilePath(uri: Uri): String? {
//      var cursor: Cursor? = null
//      var path: String? = null
//      try {
//        val projection = arrayOf(MediaStore.Images.Media.DATA)
//        cursor = ContextHolder.get()?.contentResolver?.query(uri, projection, null, null, null)
//        if(cursor != null && cursor.count > 0) {
//          val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
//          cursor.moveToFirst()
//          path = cursor.getString(columnIndex)
//        }
//      } finally {
//        cursor?.close()
//      }
//      return path
//    }


  fun getMimeType(absPath: String): String? {
    return try {
      URLConnection.guessContentTypeFromName(File(absPath).name)
    } catch(e: StringIndexOutOfBoundsException) {
      null
    }
  }
  //
  fun scanPath(path: String? = syncDirAbsPath) {
    if(path == syncDirAbsPath) Data.absPathList.clear()
    if(path != null){
      val root = File(path)
      val list = root.listFiles()
      if(list != null) {
        for(file in list) {
          if(file.isDirectory) {
            scanPath(file.absolutePath)
          } else {
            if(!file.isHidden){
              file.absolutePath.also {
                Data.absPathList.add(it)
                getRelPath(it)?.also { p -> Data.pathMap[p] = it }
              }
            }
          }
        }
      }
    }
  }

//  }

}
