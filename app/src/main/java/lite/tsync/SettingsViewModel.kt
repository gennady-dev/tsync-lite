package lite.tsync

import android.content.Context
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch

class SettingsViewModel @ViewModelInject constructor(
  @ApplicationContext app: Context,
  val settings: Settings,
  val tg: Tg
): ViewModel() {

  val settingsLiveData: MutableLiveData<Settings> = MutableLiveData(settings)
  val message = MutableLiveData<Int?>()
  val dialog = MutableLiveData<Dialog?>()

  val uploadMissing: Boolean
    get() = Settings2.uploadMissing

  init {
    Log.e("ViewModel", "init")
    Log.e("ViewModel application", app.toString())
    Log.e("ViewModel tg", tg.toString())
    settingsLiveData.postValue(settings)
  }

  fun login(){
    if(settings.authenticated) {
      logout()
    } else {
      dialog.postValue(Dialog.PHONE)
    }
  }

  fun logout(){
    tg.logout()
  }

  fun setSync(value: Boolean){
    viewModelScope.launch {
      val current = settings.enabled
      var msg: Int? = null
      if(!settings.authenticated){
        msg = R.string.text_settings_not_logged
      } else if(settings.path == null){
        msg = R.string.text_settings_folder_not_set
      } else if(settings.chatId == 0L || settings.groupId == 0){
        msg = R.string.text_settings_not_set_group
      }

      if(msg == null){
        settings.enabled = value
      } else {
        settings.enabled = false
        message.postValue(msg)
      }

      settings.save()
      settingsLiveData.postValue(settings)
    }
  }

  fun setPath(path: String) {
    viewModelScope.launch {
      val storagePath = Fs.externalStoragePath
      var relativePath: String? = null
      if(
        storagePath != null
        && path.matches("""${Regex.escape(storagePath)}.+""".toRegex(RegexOption.DOT_MATCHES_ALL))
      ) {
        relativePath = path.replace("$storagePath/", "")
      }
      if(relativePath != null) {
        if(!settings.path.isNullOrBlank() && settings.path != relativePath && settings.authenticated){
          logout()
        }
        settings.path = relativePath
        settings.save()
        settingsLiveData.postValue(settings)
        message.postValue(R.string.text_settings_folder_updated)
      }
    }
  }

  fun setDeleteUploaded(value: Boolean) {
    viewModelScope.launch {
      if(value && settings.downloadMissing) {
        setDownloadMissing(false)
      }
      settings.deleteUploaded = value
      settings.save()
      settingsLiveData.postValue(settings)
    }
  }

  fun setDownloadMissing(value: Boolean) {
    viewModelScope.launch {
      if(value && settings.deleteUploaded) {
        setDeleteUploaded(false)
      }
      settings.downloadMissing = value
      if(
        settings.authenticated
        && !settings.uploadMissing
        && !settings.downloadMissing
      ) setSync(false)
      settings.save()
      settingsLiveData.postValue(settings)
    }
  }

  fun setUploadMissing(value: Boolean){
    viewModelScope.launch {
      if(settings.canSend){
        settings.uploadMissing = value
      } else {
        message.postValue(R.string.text_settings_upload_not_permitted)
        settings.uploadMissing = false
      }
      if(
        settings.authenticated
        && !settings.uploadMissing
        && !settings.downloadMissing
      ) setSync(false)
      settings.save()
      settingsLiveData.postValue(settings)
    }
  }

}