package lite.telestorage.kt

//TODO
//import net.sqlcipher.database.SQLiteDatabase;
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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


    //TODO
    //SQLiteDatabase.loadLibs(this);
    (findViewById<View>(R.id.action_bar) as Toolbar)
      .setTitleTextColor(Color.parseColor(getString(R.color.colorWhite)))

    ContextHolder.ctx(this)

    fm = supportFragmentManager
    fragment = fm?.findFragmentById(R.id.fragment_container)
    if(fragment == null) {
      fragment = SettingsFragment()
      fragment?.also { fm?.beginTransaction()?.add(R.id.fragment_container, it)?.commit() }
    }
    val jobManager = BackgroundJobManagerImpl(applicationContext)
    jobManager.scheduleContentObserverJob()
    jobManager.schedulePeriodicJob()
  }

  public override fun onDestroy() {
    super.onDestroy()
    sendBroadcast(
      Intent()
        .setAction("lite.telestorage.background.service")
        .setClass(this, StartService::class.java)
    )
  }

}
