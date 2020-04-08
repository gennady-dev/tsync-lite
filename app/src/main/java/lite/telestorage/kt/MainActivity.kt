package lite.telestorage.kt

//TODO
//import net.sqlcipher.database.SQLiteDatabase;
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import lite.telestorage.kt.services.BackgroundJobManagerImpl
import lite.telestorage.kt.services.StartService


class MainActivity : AppCompatActivity() {

  var fragment: Fragment? = null
  var fm: FragmentManager? = null

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    //TODO
    //SQLiteDatabase.loadLibs(this);

    ContextHolder.init(this)

    fm = supportFragmentManager
    fragment = fm?.findFragmentById(R.id.fragment_container)
    if(fragment == null) {
      fragment = SettingsFragment()
      fragment?.also { fm?.beginTransaction()?.add(R.id.fragment_container, it)?.commit() }
    }
    BackgroundJobManagerImpl(applicationContext).scheduleContentObserverJob()
  }

  public override fun onDestroy() {
    sendBroadcast(
      Intent()
        .setAction("lite.telestorage.background.service")
        .setClass(this, StartService::class.java)
    )
    super.onDestroy()
  }

}
