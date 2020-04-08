package lite.telestorage.kt

import android.database.Cursor
import android.net.Uri
import android.os.Build.VERSION
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import lite.telestorage.kt.database.SettingsHelper
import lite.telestorage.kt.models.Settings
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLConnection

class Fs {

  companion object {

    val filePaths: MutableList<String> = ArrayList()

    private val settingsHelper: SettingsHelper?
      get() = SettingsHelper.get()

    private val settings: Settings?
      get() = settingsHelper?.settings

    val externalStoragePath: String?
      get() {
        Log.d("SDK_INT", VERSION.SDK_INT.toString())
        return if(VERSION.SDK_INT < 29) {
          Environment.getExternalStorageDirectory().path
        } else {
          val externalFilesDir: File? = ContextHolder.get()?.getExternalFilesDir(MediaStore.VOLUME_EXTERNAL)
          externalFilesDir?.path?.replace("/" + Constants.externalAppFilesPath, "")
        }
      }

    val storageAbsPath: String?
      get() {
        val storagePath: String? = externalStoragePath
        val path: String? = settings?.path
        return if(storagePath != null && path != null) {
          "$storagePath/$path"
        } else null
      }

    fun getFileAbsPath(relativeFilePath: String): String? {
      return storageAbsPath?.let { "$it/$relativeFilePath" }
    }

    fun getRelativePath(absPath: String): String? {
      return storageAbsPath?.let {
        if(absPath.matches("$it.+".toRegex())) absPath.replace("$it/", "")
        else null
      }
    }📁📁

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
        src.delete()
        moved = true
      }
      return moved
    }

    fun getFilePath(uri: Uri): String? {
      var cursor: Cursor? = null
      var path: String? = null
      try {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        cursor = ContextHolder.get()?.contentResolver?.query(uri, projection, null, null, null)
        if(cursor != null && cursor.count > 0) {
          val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
          cursor.moveToFirst()
          path = cursor.getString(columnIndex)
        }
      } finally {
        cursor?.close()
      }
      return path
    }

    fun getMimeType(absPath: String): String? {
      return try {
        URLConnection.guessContentTypeFromName(File(absPath).name)
      } catch(e: StringIndexOutOfBoundsException) {
        null
      }
    }

    fun scanPath(path: String? = storageAbsPath) {
      if(path != null){
        val root = File(path)
        val list = root.listFiles()
        if(list != null) {
          for(file in list) {
            if(file.isDirectory) {
              scanPath(file.absolutePath)
            } else {
              if(!filePaths.contains(file.absolutePath)) {
                filePaths.add(file.absolutePath)
              }
            }
          }
        }
      }
    }

  }

}
