package lite.telestorage

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.appbar.MaterialToolbar
import lite.telestorage.services.BackgroundJobManagerImpl
import lite.telestorage.services.StartService
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

  var syncActionButton: MenuItem? = null

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main_activity)

    val topAppBar: MaterialToolbar = findViewById(R.id.topAppBar)

    ContextHolder.ctx(this)
    val settingsFragment = SettingsFragment()
    val helpFragment = HelpFragment()

    supportFragmentManager.beginTransaction()
      .add(R.id.fragment_container, settingsFragment)
      .add(R.id.fragment_container, helpFragment)
      .hide(helpFragment)
      .show(settingsFragment)
      .commit()

    syncActionButton = topAppBar.menu.findItem(R.id.syncActionButton)
    topAppBar.setOnMenuItemClickListener { menuItem ->
      when (menuItem.itemId) {
        R.id.syncActionButton -> {
          if(Data.inProgress == 0L) {
            if(Sync.ready){
              thread {
                Sync.start()
              }
              Toast.makeText(applicationContext, R.string.text_settings_start_sync, Toast.LENGTH_SHORT).show()
            }
          } else {
            thread {
              Sync.stop()
            }
            Toast.makeText(applicationContext, R.string.text_settings_stop_sync, Toast.LENGTH_SHORT).show()
          }
          true
        }
        R.id.helpButton -> {
          supportFragmentManager.beginTransaction()
            .hide(settingsFragment)
            .show(helpFragment)
            .commit()
          true
        }
        R.id.settingsButton -> {
          supportFragmentManager.beginTransaction()
            .hide(helpFragment)
            .show(settingsFragment)
            .commit()
          true
        }
        else -> false
      }
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

  fun setSync(status: Boolean) {
    runOnUiThread {
      syncActionButton?.also { btn ->
        if(status){
          btn.setIcon(R.drawable.ic_sync_stop_white)
        } else {
          btn.setIcon(R.drawable.ic_sync_white)
        }
      }
    }
  }

}
