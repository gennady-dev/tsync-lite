package lite.telestorage

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment

class HelpFragment : Fragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.help, container, false)
  }

}