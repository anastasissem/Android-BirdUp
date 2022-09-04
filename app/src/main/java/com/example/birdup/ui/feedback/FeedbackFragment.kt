package com.example.birdup.ui.feedback

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.birdup.R
import com.example.birdup.databinding.FragmentFeedbackBinding

class FeedbackFragment : Fragment() {

    private lateinit var feedbackViewModel: FeedbackViewModel
    private var _binding: FragmentFeedbackBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        feedbackViewModel =
            ViewModelProvider(this).get(FeedbackViewModel::class.java)

        _binding = FragmentFeedbackBinding.inflate(inflater, container, false)
        val root: View = binding.root

        fun sendEmail(){
            Log.i("Send e-mail", "")
            val TO = arrayOf("anmessis@inf.uth.gr")
            val emailIntent = Intent(Intent.ACTION_SEND)
            emailIntent.data = Uri.parse("mailto:")
            emailIntent.type = "text/plain"
            emailIntent.putExtra(Intent.EXTRA_EMAIL, TO)

            try{
                startActivity(Intent.createChooser(emailIntent, "Send e-mail"))
            } catch (e: ActivityNotFoundException)  {
                Toast.makeText(requireContext(), "There is no e-mail client installed.", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        val mailButton = root.findViewById<Button>(R.id.feedback_send)
        mailButton.setOnClickListener {
            sendEmail()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}