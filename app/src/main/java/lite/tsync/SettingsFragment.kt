package lite.tsync

import com.obsez.android.lib.filechooser.ChooserDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import lite.tsync.databinding.SettingsBinding
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment: Fragment() {

  @Inject lateinit var settings: Settings
  @Inject lateinit var tg: Tg
  val viewModel by viewModels<SettingsViewModel>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {

    val binding: SettingsBinding = DataBindingUtil.inflate(inflater, R.layout.settings, container, false)
    val view = binding.root
    binding.lifecycleOwner = this
    binding.fragment = this
    binding.viewModel = viewModel

    Log.e("settings", settings.toString())

    viewModel.apply {
      settingsLiveData.observe(viewLifecycleOwner, Observer {
        binding.settings = it
      })

      message.observe(viewLifecycleOwner, Observer {
        if(it != null){
          Log.e("msg", "testMsg")
          activity?.apply { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
      })

      dialog.observe(viewLifecycleOwner, Observer {
        if(it != null){
          if(it == Dialog.PHONE){
            phoneDialog()
          }
        }
      })
    }

//    val v = inflater.inflate(R.layout.settings, container, false)

    Log.e("viewModel", viewModel.toString())

    val syncStatus = SyncStatus()
    Sync.syncStatus = syncStatus
    tg.syncStatus = syncStatus
    tg.settingsFragment = this

    return view
  }

  fun editGroup(): Unit {
    if(settings.authenticated) {
      groupDialog()
    } else {
      activity?.also { Toast.makeText(it, R.string.text_settings_not_logged, Toast.LENGTH_LONG).show() }
    }
  }

  fun warningDialog(){
    Log.e("warningDialog", "msg")
    val storagePath: String? = Fs.externalStoragePath
    if(settings.path != null){
      MaterialAlertDialogBuilder(context)
        .setTitle(R.string.text_settings_change_folder_warning)
        .setPositiveButton(R.string.button_ok) { _, _ -> chooserDialog() }
        .show()
    } else if(storagePath != null) chooserDialog()
  }

  private fun chooserDialog(){
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
          viewModel.setPath(path)
        }
        .build().show()
    }
  }

  private fun groupDialog() {
    MaterialAlertDialogBuilder(context)
      .setTitle(R.string.text_select_group)
      .setItems(R.array.group_list) { dialog, which ->
        dialog.dismiss()
        if(which == 0) {
          createGroupDialog()
        } else if(which == 1) {
          groupListDialog()
        }
      }.show()
  }

  private fun groupListDialog() {
    val groups: List<Group> = tg.groupList
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
            settings.groupId = group.groupId
            settings.chatId = group.chatId
            settings.title = group.title
            settings.isChannel = group.isChannel
            settings.canSend = group.creator || !group.isChannel
            if(!settings.canSend){
//              toggleUploadMissing(isChecked = false, isUser = false)
            }
            settings.save()
          }
        }.show()
    } else {
      MaterialAlertDialogBuilder(context)
        .setMessage(resources.getString(R.string.text_select_group_not_found))
        .show()
    }
  }

  private fun createGroupDialog() {
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
            tg.createNewSupergroup(newName)
          }
        }.show()
    }
  }

  fun setLogged(logged: Boolean) {
    for(ste in Thread.currentThread().stackTrace) {
      Log.d(null, ste.toString())
    }
    activity?.runOnUiThread {

      if(logged) {
        settings.authenticated = true
      } else {
        val chatId: Long = settings.chatId
//        if(chatId != 0L) {
//          FileHelper.deleteByChatId(chatId)
//        }
        settings.authenticated = false
        settings.groupId = 0
        settings.chatId = 0
        settings.title = null
      }
      settings.save()
    }
  }

  private fun phoneDialog() {
    val inflater = LayoutInflater.from(this.context)
    val editTextLayout = inflater.inflate(R.layout.outlined_text_field_phone, null) as LinearLayout
    val inputEditText: TextInputEditText = editTextLayout.findViewById(R.id.outlinedTextFieldEditText)
    MaterialAlertDialogBuilder(this.context)
      .setView(editTextLayout)
      .setPositiveButton(R.string.button_ok) { _, _ ->
        inputEditText.text?.toString()?.apply {
          if(this.isNotBlank()) viewModel.sendPhone(this.trim())
        }
      }
      .show()
  }

  fun codeDialog() {
    val inflater = LayoutInflater.from(this.context)
    val editTextLayout = inflater.inflate(R.layout.outlined_text_field_code, null) as LinearLayout
    val inputEditText: TextInputEditText =
      editTextLayout.findViewById(R.id.outlinedTextFieldEditText)
    MaterialAlertDialogBuilder(this.context)
      .setView(editTextLayout)
      .setPositiveButton(R.string.button_ok) { _, _ ->
        inputEditText.text
          ?.toString()?.also {
            if(it.isNotBlank()) tg.sendCode(it.trim())
          }
      }.show()
  }


  inner class SyncStatus {

    fun setInProgress(status: Boolean) {
      activity?.runOnUiThread {
        if(status) {
//          buttonLogin?.visibility = View.GONE
//          progressBar?.visibility = View.VISIBLE
          (activity as MainActivity).setSync(true)
        } else {
//          progressBar?.visibility = View.GONE
//          buttonLogin?.visibility = View.VISIBLE
          (activity as MainActivity).setSync(false)
        }
      }
    }

    fun setGroupName(name: String?) {
      activity?.runOnUiThread {
        if(name == null) {
          //TODO settings binding
        } else {
          //TODO settings binding
        }
      }
    }

    fun setSyncSwitch(enabled: Boolean) {
//      activity?.runOnUiThread { switchSync?.isChecked = enabled }
    }
  }
}