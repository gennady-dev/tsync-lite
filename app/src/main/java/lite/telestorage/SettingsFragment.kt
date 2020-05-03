package lite.telestorage

import com.obsez.android.lib.filechooser.ChooserDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class SettingsFragment : Fragment() {

  private var buttonLogin: Button? = null
  private var progressBar: ProgressBar? = null
  private var imageViewSync: ImageView? = null
  private var textViewSync: TextView? = null
  private var switchSync: Switch? = null
  private var buttonEditGroup: ImageView? = null
  private var buttonEditPath: ImageView? = null
  private var syncDirPathTextView: TextView? = null
  private var syncDirPath: String? = null
  private var groupNameTextView: TextView? = null
  private var groupName: String? = null
  private var imageViewDelete: ImageView? = null
  private var textViewDeleteUploadedCurrent: TextView? = null
  private var switchDeleteUploaded: Switch? = null
  private var imageViewUploadMissing: ImageView? = null
  private var textViewUploadMissingCurrent: TextView? = null
  private var switchUploadMissing: Switch? = null
  private var switchDownloadMissing: Switch? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val v = inflater.inflate(R.layout.settings, container, false)

    imageViewSync = v.findViewById(R.id.imageViewSyncEnabled)
    textViewSync = v.findViewById(R.id.textViewSyncState)
    syncDirPathTextView = v.findViewById(R.id.textViewFolderPath)
    imageViewDelete = v.findViewById(R.id.imageViewDelete)
    textViewDeleteUploadedCurrent = v.findViewById(R.id.textViewDeleteUploadedCurrent)
    switchDeleteUploaded = v.findViewById(R.id.switchDeleteUploaded)
    imageViewUploadMissing = v.findViewById(R.id.imageViewUploadMissing)
    textViewUploadMissingCurrent = v.findViewById(R.id.textViewUploadMissing)
    switchUploadMissing = v.findViewById(R.id.switchUploadMissing)
    switchDownloadMissing = v.findViewById(R.id.switchDownloadMissing)
    buttonLogin = v.findViewById(R.id.buttonLogin)
    progressBar = v.findViewById(R.id.loadingProgress)
    switchSync = v.findViewById(R.id.switchSync)
    groupNameTextView = v.findViewById(R.id.textViewSelectedGroupName)
    buttonEditGroup = v.findViewById(R.id.imageViewEditGroup)
    buttonEditPath = v.findViewById(R.id.imageViewEditFolder)

    buttonLogin?.setText(R.string.button_settings_login)
    buttonLogin?.visibility = View.GONE
    progressBar?.visibility = View.VISIBLE

    val syncStatus = SyncStatus()
    Sync.syncStatus = syncStatus
    Tg.syncStatus = syncStatus
    Tg.settingsFragment = this
    if(Settings.authenticated) {
      setLogged(true)
    } else setLogged(false)

    buttonLogin?.setOnClickListener {
      if(Settings.authenticated) {
        logout()
      } else {
        showPhoneDialog()
      }
    }

    Settings.path?.also {
      syncDirPath = it
      syncDirPathTextView?.text = it
    }

    Settings.title?.also {
      groupName = it
      groupNameTextView?.text = groupName
    }

    Settings.also {
      toggleSync(it.enabled, false)
      toggleDeleteUploaded(it.deleteUploaded, false)
      toggleDownloadMissing(it.downloadMissing, false)
      toggleUploadMissing(it.uploadMissing, false)
    }

    switchSync?.setOnCheckedChangeListener { _, isChecked ->
      toggleSync(isChecked, true)
    }

    switchDeleteUploaded?.setOnCheckedChangeListener { _, isChecked ->
      toggleDeleteUploaded(isChecked, true)
    }

    switchDownloadMissing?.setOnCheckedChangeListener { _, isChecked ->
      toggleDownloadMissing(isChecked, true)
    }

    switchUploadMissing?.setOnCheckedChangeListener { _, isChecked ->
      if(Settings.canSend){
        toggleUploadMissing(isChecked, true)
      } else {
        Toast.makeText(context, R.string.text_settings_upload_not_permitted, Toast.LENGTH_LONG).show()
        toggleUploadMissing(isChecked = false, isUser = false)
      }
    }

    buttonEditGroup?.setOnClickListener {
      if(Settings.authenticated) {
        showGroupDialog()
      } else {
        activity?.also { Toast.makeText(it, R.string.text_settings_not_logged, Toast.LENGTH_LONG).show() }
      }
    }

    buttonEditPath?.setOnClickListener {
      val storagePath: String? = Fs.externalStoragePath
      if(Settings.path != null){
        MaterialAlertDialogBuilder(context)
          .setTitle(R.string.text_settings_change_folder_warning)
          .setPositiveButton(R.string.button_ok) { _, _ -> showChooserDialog() }
          .show()
      } else if(storagePath != null) showChooserDialog()
    }
    return v
  }

  private fun showChooserDialog(){
    val chooseFolder = context?.getString(R.string.folder_chooser_choose_folder) ?: "Choose folder"
    val choose = context?.getString(R.string.folder_chooser_choose) ?: "Choose"
    val cancel = context?.getString(R.string.folder_chooser_cancel) ?: "Cancel"
    val storagePath: String? = Fs.externalStoragePath
    activity?.also { a ->
      ChooserDialog(a)
        .withFilter(true, false)
        .withStartFile(storagePath)
        .withDateFormat("dd.MM.yyyy")
        .withStringResources(chooseFolder, choose, cancel)
        .disableTitle(false)
        .enableOptions(true)
        .titleFollowsDir(true)
        .displayPath(false)
        .withChosenListener { path, _ ->
          Log.d("path", "storagePath $path")
          setPath(path)
        }
        .build().show()
    }
  }

  private fun showGroupDialog() {
    MaterialAlertDialogBuilder(context)
      .setTitle(R.string.text_select_group)
      .setItems(R.array.group_list) { dialog, which ->
        dialog.dismiss()
        if(which == 0) {
          showCreateGroupDialog()
        } else if(which == 1) {
          showGroupListDialog()
        }
      }.show()
  }

  private fun showGroupListDialog() {
    val groups: List<Group> = Tg.groupList
    val groupNameList = arrayOfNulls<String>(groups.size)
    for((i, group) in groups.withIndex()) {
      groupNameList[i] = group.title + if(group.isChannel && !group.creator){
        " (${resources.getString(R.string.text_group_readonly)})"
      } else ""
    }
    if(groupNameList.isNotEmpty()) {
      MaterialAlertDialogBuilder(context).setTitle(R.string.text_select_group)
        .setItems(groupNameList) { dialog, which ->
          dialog.dismiss()
          if(groups.size > which) {
            val group = groups[which]
//            if(
//              Settings.supergroupId != 0
//              && Settings.chatId != 0L
//              && group.superGroupId != Settings.supergroupId
//              && group.chatId != Settings.chatId
//            ) {
//              FileHelper.deleteByChatId(Settings.chatId)
//            }
            Settings.groupId = group.groupId
            Settings.chatId = group.chatId
            Settings.title = group.title
            Settings.isChannel = group.isChannel
            Settings.canSend = group.creator || !group.isChannel
            if(!Settings.canSend){
              toggleUploadMissing(isChecked = false, isUser = false)
            }
            Settings.save()
            group.title?.also { groupNameTextView?.text = it }
          }
        }.show()
    } else {
      MaterialAlertDialogBuilder(context)
        .setMessage(resources.getString(R.string.text_select_group_not_found))
        .show()
    }
  }

  private fun showCreateGroupDialog() {
    val editTextLayout = layoutInflater.inflate(R.layout.edit_text_material, null) as LinearLayout
    val inputEditText: TextInputEditText = editTextLayout.findViewById(R.id.text_input_edit_text)
    inputEditText.setText(R.string.text_settings_default_group_name_text)
    activity?.also {
      MaterialAlertDialogBuilder(it)
        .setTitle(R.string.text_settings_default_group_name_title)
        .setView(editTextLayout)
        .setPositiveButton(R.string.button_ok) { _, _ ->
          if(inputEditText.text != null) {
            val newName = inputEditText.text.toString().trim()
            Toast.makeText(it, R.string.text_settings_creating_group, Toast.LENGTH_LONG).show()
            Tg.createNewSupergroup(newName)
          }
        }.show()
    }
  }

  private fun toggleSync(isChecked: Boolean, isUser: Boolean) {
    var msg = R.string.text_settings_not_logged
    var canBeSynced = Settings.authenticated

    if(Settings.path == null && !canBeSynced) {
      canBeSynced = false
      msg = R.string.text_settings_folder_not_set
    } else if(Settings.chatId == 0L || Settings.groupId == 0) {
      canBeSynced = false
      msg = R.string.text_settings_not_set_group
    }

    imageViewSync?.setImageResource(R.drawable.ic_sync_disabled)
    textViewSync?.setText(R.string.text_settings_sync_disabled)

    if(canBeSynced) {
      if(!isUser) {
        switchSync?.isChecked = isChecked
      }
      Settings.enabled = isChecked
      Settings.save()

      if(isChecked) {
        imageViewSync?.setImageResource(R.drawable.ic_sync_enabled)
        textViewSync?.setText(R.string.text_settings_sync_enabled)
      }
    } else {
      switchSync?.isChecked = false
      if(isUser) {
        activity?.also { Toast.makeText(it, msg, Toast.LENGTH_LONG).show() }
      }
    }
  }

  private fun toggleDeleteUploaded(isChecked: Boolean, isUser: Boolean) {
    if(!isUser) {
      switchDeleteUploaded?.isChecked = isChecked
    }
    if(isChecked) {
      imageViewDelete?.setImageResource(R.drawable.ic_delete)
      textViewDeleteUploadedCurrent?.setText(R.string.text_settings_will_be_deleted)
    } else {
      imageViewDelete?.setImageResource(R.drawable.ic_not_delete)
      textViewDeleteUploadedCurrent?.setText(R.string.text_settings_will_not_be_deleted)
    }
    if(Settings.downloadMissing && isChecked) {
      toggleDownloadMissing(isChecked = false, isUser = false)
    }
    Settings.deleteUploaded = isChecked
    Settings.save()
  }

  private fun toggleDownloadMissing(isChecked: Boolean, isUser: Boolean) {
    if(!isUser) {
      switchDownloadMissing?.isChecked = isChecked
    }
    if(isChecked && Settings.deleteUploaded) {
      toggleDeleteUploaded(isChecked = false, isUser = false)
    }
    Settings.downloadMissing = isChecked
    if(!Settings.uploadMissing && !Settings.downloadMissing && isUser) toggleSync(
      isChecked = false,
      isUser = false
    )
    Settings.save()
  }

  private fun toggleUploadMissing(isChecked: Boolean, isUser: Boolean) {
    if(!isUser) {
      switchUploadMissing?.isChecked = isChecked
    }
    Settings.uploadMissing = isChecked
    if(!Settings.uploadMissing && !Settings.downloadMissing && isUser) toggleSync(
      isChecked = false,
      isUser = false
    )
    if(Settings.uploadMissing != isChecked) {
      Settings.uploadMissing = isChecked
      Settings.save()
      if(Settings.uploadMissing) {
        textViewUploadMissingCurrent?.setText(R.string.text_settings_upload_missing)
      } else {
        textViewUploadMissingCurrent?.setText(R.string.text_settings_not_upload_missing)
      }
    }
  }

  fun setLogged(logged: Boolean) {
    for(ste in Thread.currentThread().stackTrace) {
      Log.d(null, ste.toString())
    }
    activity?.runOnUiThread {
      progressBar?.visibility = View.GONE
      buttonLogin?.visibility = View.VISIBLE

      if(logged) {
        buttonLogin?.setText(R.string.button_settings_logout)
        buttonLogin?.setTextColor(resources.getColor(R.color.colorAccent))
        Settings.authenticated = true
      } else {
        buttonLogin?.setText(R.string.button_settings_login)
        buttonLogin?.setTextColor(resources.getColor(R.color.colorPrimary))
        val chatId: Long = Settings.chatId
//        if(chatId != 0L) {
//          FileHelper.deleteByChatId(chatId)
//        }
        Settings.authenticated = false
        Settings.groupId = 0
        Settings.chatId = 0
        Settings.title = null
        switchSync?.isChecked = false
        groupNameTextView?.setText(R.string.text_settings_group_not_set)
      }
      Settings.save()
    }
  }

  fun logout() {
    buttonLogin?.visibility = View.GONE
    progressBar?.visibility = View.VISIBLE
    Tg.logout()
  }

  private fun setPath(path: String) {
      val storagePath = Fs.externalStoragePath
      var relativePath: String? = null
      if(
        storagePath != null
        && path.matches("""${Regex.escape(storagePath)}.+""".toRegex(RegexOption.DOT_MATCHES_ALL))
      ) {
        relativePath = path.replace("$storagePath/", "")
      }
      if(relativePath != null) {
        if(!Settings.path.isNullOrBlank() && Settings.path != relativePath && Settings.authenticated){
          logout()
        }
        Settings.path = relativePath
        Settings.save()
        syncDirPath = relativePath
        syncDirPathTextView?.text = relativePath
        activity?.also { Toast.makeText(it, "Folder path was updated", Toast.LENGTH_SHORT).show() }
      }
  }

  private fun showPhoneDialogFn() {
    val inflater = LayoutInflater.from(this.context)
    val editTextLayout = inflater.inflate(R.layout.outlined_text_field_phone, null) as LinearLayout
    val inputEditText: TextInputEditText = editTextLayout.findViewById(R.id.outlinedTextFieldEditText)
    MaterialAlertDialogBuilder(this.context)
      .setView(editTextLayout)
      .setPositiveButton(R.string.button_ok) { _, _ ->
        inputEditText.text?.toString()?.also {
          if(it.isNotBlank()) Tg.sendPhone(it.trim())
        }
      }
      .show()
  }

  private fun showPhoneDialog() {
    activity?.runOnUiThread(Runnable { showPhoneDialogFn() })
  }

  private fun showCodeDialogFn() {
    val inflater = LayoutInflater.from(this.context)
    val editTextLayout = inflater.inflate(R.layout.outlined_text_field_code, null) as LinearLayout
    val inputEditText: TextInputEditText =
      editTextLayout.findViewById(R.id.outlinedTextFieldEditText)
    MaterialAlertDialogBuilder(this.context)
      .setView(editTextLayout)
      .setPositiveButton(R.string.button_ok) { _, _ ->
        inputEditText.text
          ?.toString()?.also {
            if(it.isNotBlank()) Tg.sendCode(it.trim())
          }
      }.show()
  }

  fun showCodeDialog() {
    activity?.runOnUiThread { showCodeDialogFn() }
  }


  inner class SyncStatus {

    fun setInProgress(status: Boolean) {
      activity?.runOnUiThread {
        if(status) {
          buttonLogin?.visibility = View.GONE
          progressBar?.visibility = View.VISIBLE
          (activity as MainActivity).setSync(true)
        } else {
          progressBar?.visibility = View.GONE
          buttonLogin?.visibility = View.VISIBLE
          (activity as MainActivity).setSync(false)
        }
      }
    }

    fun setGroupName(name: String?) {
      activity?.runOnUiThread {
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
      activity?.runOnUiThread { switchSync?.isChecked = enabled }
    }
  }
}