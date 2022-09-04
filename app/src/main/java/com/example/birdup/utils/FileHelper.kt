package com.example.birdup.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.birdup.model.Recording
import java.io.*
import java.nio.file.Files

class FileHelper(val context: Context) {

//    private fun getRecording(filename: String): Recording? {
//        //val rec : Recording
//
//        //val rec : Recording?
//        //rec = createRecordingObjectFromRecFile(context, "00.rec")
//
//         return null //rec
//    }

    private fun createRecordingObjectFromRecFile(fileName: String?): Recording?{
        var rec: Recording? = null
        try {
            val fis: FileInputStream = context.openFileInput(fileName)
            Log.d("FIS", fis.toString())
            val ois = ObjectInputStream(fis)
            Log.d("OIS", fis.toString())
            rec = ois.readObject() as Recording
            Log.d("REC", fis.toString())
            ois.close()
            fis.close()
        }
        catch (e: Exception){
            e.printStackTrace()
        }
        return rec
    }

    private fun String?.withPath(): String{
        return context.filesDir.path+"/"+this
    }

    fun deleteRecording(fileName: String) {
        val file1 = File("$fileName.3gp").absoluteFile
        val file2 = File("$fileName.rec").absoluteFile
        Log.d("file1", file1.path)
        Log.d("file2", file2.path)
        val file = File(fileName.withPath()).absoluteFile
        Log.d("WWWWWWWWWWWWWWWWWWWWWWWWithPath()", fileName.withPath())
        if (file1.exists()) {
            if (file1.delete() && file2.delete())
                Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(context, "File couldn't be deleted", Toast.LENGTH_SHORT).show()
        } else Log.v("Info", "File \"$fileName\" does not exist!")
    }

    //todo call this when user saves a recording, in order to save the Recording model as a file
    fun saveRecObjectAsRecFile(rec: Recording, fileNameWithSuffix: String){ //to filename na periexei suffix (.rec)
        try {
            val fos: FileOutputStream = context.openFileOutput(fileNameWithSuffix, Context.MODE_PRIVATE)
            val os = ObjectOutputStream(fos)
            os.writeObject(rec)
            os.close()
            fos.close()
        }
        catch (e: Exception){
            e.printStackTrace()
        }
    }

    //takes .3gp and .bird files and creates list of Recording model objects
    fun getAllRecordings(): ArrayList<Recording>{
        val recList = ArrayList<Recording>()
        val filenames = ArrayList<String>()
        //order files based on time of last modification
        try {
            val files = ArrayList<File>()

            for (fn in context.filesDir.listFiles()!!) {
                if(fn.name.endsWith(".3gp")) {
                    files.add(File(fn.toString()))
                    Log.d("SHOWME", fn.toString())
                }
            }
            files.sortBy { it.lastModified() }
            for (f in files) {
                filenames.add(0, f.nameWithoutExtension+".rec")    //Place the most recent first
                Log.d("filenames contents", f.absolutePath)
            }
        }
        catch (e: Exception){
            for(fn in context.fileList())
                filenames.add(fn)
        }
        var loadedRecording: Recording?
        Log.d("FILENAMES", filenames.toString())
        for(f in filenames){
            Log.d("WHAT IS THIS", f)
            loadedRecording = createRecordingObjectFromRecFile(f)
            if(loadedRecording!=null)
                recList += loadedRecording
        }
        return recList
    }

    companion object {
        fun getFilesDirPath(context: Context) = context.filesDir.path + "/"
        fun getFilesDirPath(context: Context, fileName: String?) =
            context.filesDir.path + "/" + fileName
    }

}