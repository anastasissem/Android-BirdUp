package com.example.birdup.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.birdup.R
import com.example.birdup.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {

    private lateinit var aboutViewModel: AboutViewModel
    private var _binding: FragmentAboutBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        aboutViewModel =
            ViewModelProvider(this)[AboutViewModel::class.java]

        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        val root: View = binding.root

//        val textView: TextView = binding.About
//        aboutViewModel.text.observe(viewLifecycleOwner, Observer {
//            textView.text = it
//        })

        val aboutText = root.findViewById<TextView>(R.id.About)
        aboutText.text = getString(R.string.about_app)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}