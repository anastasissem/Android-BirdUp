package com.example.birdup.ui.home

import android.app.Dialog
import android.text.TextUtils.replace
import android.text.TextUtils.split
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.example.birdup.model.Bird
import com.example.birdup.model.Recording
import com.example.birdup.utils.FileHelper
import java.io.File
import java.io.IOException


class ShowDialog(view: View, infoList: MutableList<String>, val onSave:() -> Unit ) {

    // tsekare kwdika mina na katalaveis kalytera
    private val context = view.context
    private val title = context.getString(com.example.birdup.R.string.popup_title)
    private val save = context.getString(com.example.birdup.R.string.popup_save_btn)
    private val cancel = context.getString(com.example.birdup.R.string.popup_cancel_btn)

    private val birdInfo = infoList

    fun showDialog() {
        val dialog = Dialog(context)
        dialog.setCancelable(true)
        dialog.setContentView(com.example.birdup.R.layout.popup_layout)
        dialog.findViewById<TextView>(com.example.birdup.R.id.popup_window_title).text = title
        val saveButton = dialog.findViewById<TextView>(com.example.birdup.R.id.popup_savebtn)
        saveButton.text = save

        saveButton.setOnClickListener {

            //keep 3gp and rec files in a sorted manner
            val files = ArrayList<File>()

            val sourcePath = context.filesDir.listFiles()!!
            // add newest recording at the top
            for (f in sourcePath) {
                if ((f.name.endsWith(".3gp")) or (f.name.endsWith(".rec"))) {
                    files.add(0, f)
                    Log.d("ADDED", f.toString())
                }
            }

            val recorded = files[0]
            println(recorded)
            //delete .wav copy
            File(context.filesDir.path + "/${recorded.nameWithoutExtension}.wav").delete()
            try {

                //create rec file
                val fileName = "${recorded.nameWithoutExtension}.rec"
                val filePath = context.filesDir.path + "/$fileName"
                val file = File(filePath)
                Log.d("-------------------ABSOLUTE REC FILE NAME", filePath)
                Log.d("-------------------REC FILE NAME", fileName)

                val isNewFileCreated: Boolean = file.createNewFile()

                if (isNewFileCreated) {
                    println("$fileName is created successfully.")
                } else {
                    println("$fileName already exists.")
                }
                // Save the recording model as a file
                // provide path without extensions to manage both files more easily
                val pairPath = context.filesDir.path + "/" + recorded.nameWithoutExtension
                val dateVal = recorded.nameWithoutExtension.split("_").toMutableList()

                // Pass date in the globally accepted format, replace @ w/ space, '*', w/ ':'
                val dash = dateVal[1].indexOf("@")
                dateVal[1] = dateVal[1].substring(0, dash) + ' ' + dateVal[1].substring(dash + 1)
                dateVal[1] = dateVal[1].replace("*", ":")

                Log.d("no extension 1", recorded.nameWithoutExtension)
                Log.d("no extension 2", recorded.name)
                Log.d("no extension 3", recorded.path)
                Log.d("DATEEEEEEEEEEEEEEEEEEEEEEEEEE", dateVal[1])
                val rec: Recording?
                rec = Recording(
                    //replace dash in latin name with space
                    Bird(birdInfo[0].toInt(), birdInfo[1], birdInfo[2].replace('-', ' ')),
                    pairPath,
                    birdInfo[3]+'%',//.toDouble(),
                    dateVal[1]
                )
                FileHelper(context).saveRecObjectAsRecFile(rec, fileName)
                Toast.makeText(context, "Audio Saved!", Toast.LENGTH_SHORT).show()
                files.add(File(filePath))

                files.sortBy { it.lastModified() }
                Log.d("FINALSORT", files.toString())

            } catch (e: IOException) {
                Log.e("Failed to copy file", e.toString())
                e.printStackTrace()
            }
            dialog.dismiss()
            onSave()
        }

        val cancelButton = dialog.findViewById<TextView>(com.example.birdup.R.id.popup_discardbtn)
        cancelButton.text = cancel
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}