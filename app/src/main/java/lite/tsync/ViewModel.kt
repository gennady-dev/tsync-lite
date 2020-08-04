package lite.tsync

import android.content.Context
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ViewModel @ViewModelInject constructor(
  @ApplicationContext app: Context
): ViewModel() {

  val settings = Settings
  val settingsLiveData: MutableLiveData<Settings> = MutableLiveData(settings)
//  @Inject lateinit var app: TSyncApp
  val tg = (app as TSyncApp).tg

  val uploadMissing: Boolean
    get() = Settings.uploadMissing

  init {
    Log.e("ViewModel", "init")
    Log.e("ViewModel application", app.toString())
    Log.e("ViewModel tg", tg.toString())
  }

  fun setUploadMissing(value: Boolean){
    value
  }

}