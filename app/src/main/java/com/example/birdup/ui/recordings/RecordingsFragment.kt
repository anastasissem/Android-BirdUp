package com.example.birdup.ui.recordings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.birdup.R
import com.example.birdup.databinding.FragmentRecordingsBinding
import com.example.birdup.model.Bird
import com.example.birdup.utils.FileHelper

class RecordingsFragment : Fragment() {

    private lateinit var recordingsViewModel: RecordingsViewModel
    private var _binding: FragmentRecordingsBinding? = null

    //SavedAdapter Vars
    private var indexes: Int = 0
    private var names: String? = null
    private var latins: String? = null
    private var confidences: String? = null
    private var dates: String? = null


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        recordingsViewModel =
            ViewModelProvider(this)[RecordingsViewModel::class.java]

        _binding = FragmentRecordingsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        // DISPLAY SAVED FILES
        val savedList: RecyclerView = binding.recordingsView

//        if(indexes == 0){
//            savedList.visibility = View.INVISIBLE
//            binding.initText.visibility = View.VISIBLE
//        }
//        else {
        binding.initText.visibility = View.INVISIBLE
        savedList.visibility = View.VISIBLE

        //SHOW LIST OF SAVED RECORDINGS
        recordingsViewModel.setRecordings(FileHelper(requireContext()).getAllRecordings())
        savedList.layoutManager = LinearLayoutManager(context)
        savedList.itemAnimator = DefaultItemAnimator()
        savedList.adapter = SavedAdapter(recordingsViewModel.recordings, this)
   //     }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}