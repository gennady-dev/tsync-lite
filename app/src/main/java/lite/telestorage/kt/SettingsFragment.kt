package lite.telestorage.kt


import com.obsez.android.lib.filechooser.ChooserDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import lite.telestorage.kt.database.FileHelper
import lite.telestorage.kt.database.SettingsHelper
import lite.telestorage.kt.models.Settings
import java.io.File
import java.util.*


class SettingsFragment : Fragment() {

  private var fragmentLayoutInflater: LayoutInflater? = null
  private var tg: Tg? = null
  private var buttonLogin: Button? = null
  private var progressBar: ProgressBar? = null
  private var imageViewSync: ImageView? = null
  private var textViewSync: TextView? = null
  private var switchSync: Switch? = null
  private var syncEnabled = false
  private var buttonEditGroup: ImageView? = null
  private var buttonEditPath: ImageView? = null
  private var settings: Settings? = null
  private var syncDirPathTextView: TextView? = null
  private var syncDirPath: String? = null
  private var groupNameTextView: TextView? = null
  private var groupName: String? = null
  private var imageViewDelete: ImageView? = null
  private var textViewDeleteUploadedCurrent: TextView? = null
  private var switchDeleteUploaded: Switch? = null
  private var deleteUploaded = false
  private var imageViewUploadAsMedia: ImageView? = null
  private var textViewUploadAsMediaCurrent: TextView? = null
  private var switchUploadAsMedia: Switch? = null
  private var uploadAsMedia = false
  private var switchDownloadMissing: Switch? = null
  private var downloadMissing = false
  private var floatingActionButton: FloatingActionButton? = null
  private var groups: List<Group>? = null

  private val settingsHelper: SettingsHelper?
    get() = SettingsHelper.get()

  val mainActivity: MainActivity?
    get() = ContextHolder.getActivity()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {

    settingsHelper?.settings?.let {
      if(it.authenticated) {
        tg = Tg.get()
        setLogged(true)
      }
    }
    fragmentLayoutInflater = inflater
    val v = inflater.inflate(R.layout.fragment_settings, container, false)

    buttonLogin = v.findViewById(R.id.buttonLogin)
    buttonLogin?.setText(R.string.button_settings_login)
    buttonLogin?.visibility = View.GONE

    progressBar = v.findViewById(R.id.loadingProgress)
    progressBar?.visibility = View.VISIBLE

    switchSync = v.findViewById(R.id.switchSync)
    groupNameTextView = v.findViewById(R.id.textViewSelectedGroupName)
    Sync.syncStatus = SyncStatus()
    Tg.syncStatus = Sync.syncStatus
    tg?.settingsFragment = this

    buttonLogin?.setOnClickListener(View.OnClickListener {
      settingsHelper?.settings?.also { settings ->
        if(settings.authenticated) {
          logout()
        } else {
          tg = Tg.get()
          tg?.settingsFragment = this@SettingsFragment
          showPhoneDialog()
        }
      }
    })
    imageViewSync = v.findViewById(R.id.imageViewSyncEnabled)
    textViewSync = v.findViewById(R.id.textViewSyncState)
    syncDirPathTextView = v.findViewById(R.id.textViewFolderPath)
    imageViewDelete = v.findViewById(R.id.imageViewDelete)
    textViewDeleteUploadedCurrent = v.findViewById(R.id.textViewDeleteUploadedCurrent)
    switchDeleteUploaded = v.findViewById(R.id.switchDeleteUploaded)
    imageViewUploadAsMedia = v.findViewById(R.id.imageViewAsMedia)
    textViewUploadAsMediaCurrent = v.findViewById(R.id.textViewUploadAsMediaCurrent)
    switchUploadAsMedia = v.findViewById(R.id.switchUploadAsMedia)
    switchDownloadMissing = v.findViewById(R.id.switchDownloadMissing)
    settings = settingsHelper?.settings
    syncDirPath = settings?.path
    syncDirPathTextView?.let { it.text = syncDirPath }
    groupName = settings?.title
    groupNameTextView?.let { it.text = groupName }
    if(settings == null) {
      settings = Settings()
    }

    settings?.let { it -> {
      toggleSync(it.enabled, false)
      toggleDeleteUploaded(it.deleteUploaded, false)
      toggleDownloadMissing(it.downloadMissing, false)
      toggleUploadAsMedia(it.uploadAsMedia, false)
    }}

    switchSync?.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
      toggleSync(isChecked, true)
    })

    switchDeleteUploaded?.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
      toggleDeleteUploaded(isChecked, true)
    })

    switchDownloadMissing?.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
      toggleDownloadMissing(isChecked, true)
    })

    switchUploadAsMedia?.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
      toggleUploadAsMedia(isChecked, true)
    })

    floatingActionButton = v.findViewById(R.id.floatingActionButton)
    floatingActionButton?.setOnClickListener(View.OnClickListener { Thread(Runnable { Sync.start() }).start() })

    buttonEditGroup = v.findViewById(R.id.imageViewEditGroup)
    buttonEditGroup?.setOnClickListener(View.OnClickListener {
      if(Tg.haveAuthorization) {
        showGroupDialog()
      } else {
        Toast.makeText(mainActivity, R.string.text_settings_not_logged, Toast.LENGTH_LONG).show()
      }
    })

    buttonEditPath = v.findViewById(R.id.imageViewEditFolder)
    buttonEditPath?.setOnClickListener(View.OnClickListener {
      val chooseFolder = context?.getString(R.string.folder_chooser_choose_folder) ?: "Choose folder"
      val choose = context?.getString(R.string.folder_chooser_choose) ?: "Choose"
      val cancel = context?.getString(R.string.folder_chooser_cancel) ?: "Cancel"

      try {
        ChooserDialog(mainActivity).withFilter(true, false)
          .withStartFile(Fs.externalStoragePath)
          .withDateFormat("dd.MM.yyyy")
          .withStringResources(chooseFolder, choose, cancel)
          .disableTitle(false)
          .enableOptions(true)
          .titleFollowsDir(true)
          .displayPath(false)
          .withChosenListener(ChooserDialog.Result { _: String, _: File ->
            fun onChoosePath(path: String, pathFile: File?) {
              setPath(path)
            }
          })
          .build()
          .show()
      } catch(e: NullPointerException) {}
    })
    return v
  }

  fun showGroupDialog() {
    MaterialAlertDialogBuilder(context).setTitle(R.string.text_select_group)
      .setItems(R.array.group_list) { dialog, which ->
        dialog.dismiss()
        if(mainActivity != null) {
          if(which == 0) {
            showCreateGroupDialog()
          } else if(which == 1) {
            showGroupListDialog()
          }
        }
      }.show()
  }

  fun showGroupListDialog() {
    if(tg != null) {
      val groups: List<Group> = tg?.groupList ?: ArrayList<Group>()
      val groupNameList = arrayOfNulls<String>(groups.size)
      val settings = settingsHelper?.settings ?: Settings()
      for((i, group) in groups.withIndex()) {
        groupNameList[i] = group.title
      }
      if(groupNameList.isNotEmpty()) {
        MaterialAlertDialogBuilder(context).setTitle(R.string.text_select_group)
          .setItems(groupNameList) { dialog, which ->
            dialog.dismiss()
            if(groups.size > which) {
              val group = groups[which]
              if(
                FileHelper.get() != null
                && settings.supergroupId != 0
                && settings.chatId != 0L
                && group.superGroupId != settings.supergroupId
                && group.chatId != settings.chatId
              ){
                FileHelper.get()?.deleteByChatId(settings.chatId)
              }
              settings.supergroupId = group.superGroupId
              settings.chatId = group.chatId
              settings.title = group.title
              settings.lastUpdate = Date().time
              settings.isChannel = group.isChannel
              settingsHelper?.updateSettings(settings)
              if(group.title != null) {
                groupNameTextView?.text = group.title
              }
            }
          }.show()
      }
    }
  }

  fun showCreateGroupDialog() {
    val editTextLayout = fragmentLayoutInflater?.inflate(R.layout.edit_text_material, null) as LinearLayout
    val inputEditText: TextInputEditText = editTextLayout.findViewById(R.id.text_input_edit_text)
    inputEditText.setText(R.string.text_settings_default_group_name_text)
    MaterialAlertDialogBuilder(mainActivity)
      .setTitle(R.string.text_settings_default_group_name_title)
      .setView(editTextLayout)
      .setPositiveButton(R.string.button_ok) { dialog, which ->
        if(inputEditText.text != null) {
          val newName = inputEditText.text.toString().trim()
          Toast.makeText(mainActivity, R.string.text_settings_creating_group, Toast.LENGTH_LONG)
            .show()
          Tg.get()?.createNewSupergroup(newName)
        }
      }.show()
  }

  fun toggleSync(isChecked: Boolean, isUser: Boolean) {
    val settings = settingsHelper?.settings
    if(settings != null){
      var canBeSynced = true
      var msg = 0

      if(!settings.authenticated) {
        canBeSynced = false
        msg = R.string.text_settings_not_logged
      } else if(settings.path == null) {
        canBeSynced = false
        msg = R.string.text_settings_folder_not_set
      } else if(settings.chatId == 0L || settings.supergroupId == 0) {
        canBeSynced = false
        msg = R.string.text_settings_not_set_group
      }

      imageViewSync?.setImageResource(R.drawable.ic_sync_disabled)
      textViewSync?.setText(R.string.text_settings_sync_disabled)

      if(canBeSynced) {
        syncEnabled = isChecked
        if(!isUser) {
          switchSync?.isChecked = isChecked
        }
        settings.enabled = isChecked
        settingsHelper?.updateSettings(settings)

        if(isChecked) {
          imageViewSync?.setImageResource(R.drawable.ic_sync_enabled)
          textViewSync?.setText(R.string.text_settings_sync_enabled)
        }
      } else {
        switchSync?.isChecked = false
        if(isUser && mainActivity != null) {
          Toast.makeText(mainActivity, msg, Toast.LENGTH_LONG).show()
        }
      }
    }
  }

  fun toggleDeleteUploaded(isChecked: Boolean, isUser: Boolean) {
    val settings = settingsHelper?.settings
    if(settings != null) {
      deleteUploaded = isChecked
      if(!isUser) {
        switchDeleteUploaded?.isChecked = isChecked
      }
      settings.deleteUploaded = isChecked
      settingsHelper?.updateSettings(settings)
      if(isChecked) {
        imageViewDelete?.setImageResource(R.drawable.ic_delete)
        textViewDeleteUploadedCurrent?.setText(R.string.text_settings_will_be_deleted)
      } else {
        imageViewDelete?.setImageResource(R.drawable.ic_not_delete)
        textViewDeleteUploadedCurrent?.setText(R.string.text_settings_will_not_be_deleted)
      }
      if(downloadMissing && isChecked) {
        toggleDownloadMissing(isChecked = false, isUser = false)
      }
    }
  }

  fun toggleDownloadMissing(isChecked: Boolean, isUser: Boolean) {
    val settings = settingsHelper?.settings
    if(settings != null) {
      downloadMissing = isChecked
      if(!isUser) {
        switchDownloadMissing?.let { it.isChecked = isChecked }
      }
      settings.downloadMissing = isChecked
      settingsHelper?.updateSettings(settings)
      if(isChecked && deleteUploaded) {
        toggleDeleteUploaded(isChecked = false, isUser = false)
      }
    }
  }

  fun toggleUploadAsMedia(isChecked: Boolean, isUser: Boolean) {
    val settings = settingsHelper?.settings
    if(settings != null) {
      uploadAsMedia = isChecked
      if(!isUser) {
        switchUploadAsMedia?.isChecked = isChecked
      }
      settings.uploadAsMedia = isChecked
      settingsHelper?.updateSettings(settings)
      if(uploadAsMedia) {
        imageViewUploadAsMedia?.setImageResource(R.drawable.ic_media)
        textViewUploadAsMediaCurrent?.setText(R.string.text_settings_upload_as_media)
      } else {
        imageViewUploadAsMedia?.setImageResource(R.drawable.ic_document)
        textViewUploadAsMediaCurrent?.setText(R.string.text_settings_upload_as_document)
      }
    }
  }

  fun setLogged(logged: Boolean) {
    for(ste in Thread.currentThread().stackTrace) {
      Log.d(null, ste.toString())
    }
    val mA = mainActivity
    val bL = buttonLogin
    val pB = progressBar
    if(mA != null && bL != null && pB != null) {
      mA.runOnUiThread {
        pB.visibility = View.GONE
        bL.visibility = View.VISIBLE
        if(logged) {
          bL.setText(R.string.button_settings_logout)
          bL.setTextColor(resources.getColor(R.color.colorAccent))
        } else {
          bL.setText(R.string.button_settings_login)
          bL.setTextColor(resources.getColor(R.color.colorPrimary))
          val settings = settingsHelper?.settings
          if(settings != null) {
            val chatId: Long = settings.chatId
            if(chatId != 0L) {
              FileHelper.get()?.deleteByChatId(chatId)
            }
            settings.authenticated = false
            settings.supergroupId = 0
            settings.chatId = 0
            settings.title = null
            settingsHelper?.updateSettings(settings)
          }
          switchSync?.isChecked = false
          groupNameTextView?.setText(R.string.text_settings_group_not_set)
        }
      }
    }
  }

  fun logout() {
    buttonLogin?.visibility = View.GONE
    progressBar?.visibility = View.VISIBLE
    tg?.logout()
  }

  private fun setPath(path: String) {
    val activity = mainActivity
    val settings = settingsHelper?.settings
    if(activity != null && settings != null) {
      val storagePath = Fs.externalStoragePath
      var relativePath: String? = null
      if(storagePath != null && path.matches("$storagePath.+".toRegex())) {
        relativePath = path.replace("$storagePath/", "")
      }
      if(relativePath != null) {
        settings.path = relativePath
        settingsHelper?.updateSettings(settings)
        syncDirPath = relativePath
        syncDirPathTextView?.text = relativePath
        Toast.makeText(activity, "Folder path was updated", Toast.LENGTH_SHORT).show()
      }
    }
  }

  fun showPhoneDialogFn() {
    val inflater = LayoutInflater.from(this.context)
    val editTextLayout = inflater.inflate(R.layout.outlined_text_field_phone, null) as LinearLayout
    val inputEditText: TextInputEditText = editTextLayout.findViewById(R.id.outlinedTextFieldEditText)
    MaterialAlertDialogBuilder(this.context)
      .setView(editTextLayout)
      .setPositiveButton(R.string.button_ok) { _, _ ->
        inputEditText.text.toString().trim().let { Tg.get()?.sendPhone(it) }
      }
      .show()
  }

  fun showPhoneDialog() {
    mainActivity?.runOnUiThread(Runnable { showPhoneDialogFn() })
  }

  fun showCodeDialogFn() {
    val inflater = LayoutInflater.from(this.context)
    val editTextLayout = inflater.inflate(R.layout.outlined_text_field_code, null) as LinearLayout
    val inputEditText: TextInputEditText =
      editTextLayout.findViewById(R.id.outlinedTextFieldEditText)
    MaterialAlertDialogBuilder(this.context)
      .setView(editTextLayout)
      .setPositiveButton(R.string.button_ok) { _, _ ->
        inputEditText.text.toString().trim().let { Tg.get()?.sendCode(it) }
      }.show()
  }

  fun showCodeDialog() {
    mainActivity?.runOnUiThread(Runnable { showCodeDialogFn() })
  }


  inner class SyncStatus {

    fun setInProgress(status: Boolean) {
      mainActivity?.runOnUiThread {
        if(status) {
          buttonLogin?.visibility = View.GONE
          progressBar?.visibility = View.VISIBLE
        } else {
          progressBar?.visibility = View.GONE
          buttonLogin?.visibility = View.VISIBLE
        }
      }
    }

    fun setGroupName(name: String?) {
      mainActivity?.runOnUiThread {
        if(name == null) {
          groupNameTextView?.setText(R.string.text_settings_group_not_set)
          if(switchSync != null) {
            switchSync?.isChecked = false
          }
        } else {
          groupNameTextView?.text = name
        }
      }
    }

    fun setSyncSwitch(enabled: Boolean) {
      mainActivity?.runOnUiThread { switchSync?.isChecked = enabled }
    }
  }
}