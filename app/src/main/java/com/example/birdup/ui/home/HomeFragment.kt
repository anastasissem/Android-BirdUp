package com.example.birdup.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arthenica.ffmpegkit.FFmpegKit
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.birdup.R
import com.example.birdup.databinding.FragmentHomeBinding
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.*
import java.math.RoundingMode
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.ByteOrder
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


private const val LOG_TAG = "AudioRecordTest"

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    //RecyclerAdapter Vars
    private lateinit var titlesList: MutableList<String>
    private lateinit var detailsList: MutableList<String>
    private lateinit var percentsList: MutableList<String>
    private lateinit var imageList: MutableList<Int>

    //Secondary helper lists for result averaging
    private lateinit var titlesListHelper: MutableList<String>
    private lateinit var detailsListHelper: MutableList<String>
    private lateinit var percentsListHelper: MutableList<Double>
    private lateinit var imageListHelper: MutableList<Int>

    // HomeFragment exclusive vars
    private var fileName: String? = null
    private var modelInput: String? = null
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    private var timeWhenStopped: Long = 0

    private var isRecording = false
    private var isPaused = false

    //pass date of recording to filename
    private var recDate: String = "0"

    // Requesting permission to use device microphone
    private fun recordPermissionSetup() {
        val permission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.RECORD_AUDIO)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            permissionsResultCallback.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            //onRecord()
        }
    }

    // Requesting permission to write to device storage
    private fun writePermissionSetup(){
        val permission = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            permissionsResultCallback.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            //onRecord()
        }
    }


    private val permissionsResultCallback = registerForActivityResult(
        ActivityResultContracts.RequestPermission()){
        when (it) {
            true -> {
                //onRecord()
                println("Permission has been granted by user")
            }
            false -> {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        recordPermissionSetup()
        writePermissionSetup()
    }


    @SuppressLint("SdCardPath")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Init RecyclerAdapter lists
        titlesList = mutableListOf()
        detailsList = mutableListOf()
        percentsList = mutableListOf()
        imageList = mutableListOf()

        // Init results helper lists
        titlesListHelper = mutableListOf()
        detailsListHelper = mutableListOf()
        percentsListHelper = mutableListOf()
        imageListHelper = mutableListOf()

        // DISPLAY PREDICTION RESULTS
        val predictionList: RecyclerView = binding.predictionsView
        predictionList.visibility = View.INVISIBLE
        predictionList.layoutManager = LinearLayoutManager(context)
        predictionList.adapter = RecyclerAdapter(titlesList, detailsList, percentsList, imageList){
            predictionList.visibility = View.INVISIBLE
        }

        val meter: Chronometer = root.findViewById(R.id.chronometer)
        val progressBar: ProgressBar = root.findViewById(R.id.progressBar)
        progressBar.visibility = View.INVISIBLE

        // INTERNAL FUNCTIONS

        fun showprogress(){
            if(progressBar.visibility == View.VISIBLE){
                progressBar.visibility = View.INVISIBLE
            }
            else{
                progressBar.visibility = View.VISIBLE
            }
        }

        fun timer() {

            if (!isPaused) {
                meter.base = SystemClock.elapsedRealtime() + timeWhenStopped
                meter.start()
            } else {
                timeWhenStopped = meter.base - SystemClock.elapsedRealtime()
                meter.stop()
            }

            if (timeWhenStopped == 0L) {
                if (!isPaused){
                    Toast.makeText(requireContext(), "RECORDING", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (isPaused){
                    Toast.makeText(requireContext(), "PAUSED", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "RESUMED", Toast.LENGTH_SHORT).show()
                }
            }
        }

        /* START/STOP */
        val recordButton: ImageButton = root.findViewById(R.id.RecordButton)
        recordButton.setOnClickListener {
            //recordPermissionSetup()
            vibrate()
            onRecord()
            timer()

            /* Change bird icon between start/pause */
            if (isPaused) {
                val icon = getDrawable(requireContext(), R.drawable.still_stork)
                recordButton.setImageDrawable(icon)
            } else {
                val icon = getDrawable(requireContext(), R.drawable.stork_medium)
                recordButton.setImageDrawable(icon)
            }
        }

        /* PLAYER FUNCTION*/
        val playButton: ImageButton = root.findViewById(R.id.playButton)
        playButton.setOnClickListener  {
            vibrate()
            // Not interactive when recording is in progress
            if ((!isRecording && !isPaused) || (isRecording && isPaused)) {

                // move save permissions to save button
                //writePermissionSetup()
                if (!hasAudio()){
                    Toast.makeText(requireContext(), "Nothing to Play!", Toast.LENGTH_SHORT).show()
                } else {
                    releaseRecorder()

//                    val uri = Uri.fromFile(File(fileName))
//                    val uri = URI(fileName)

                    //player = MediaPlayer.create(context, uri)
                    player = MediaPlayer().apply{
                        //setDataSource(requireContext(), Uri.fromFile(File(fileName)))
                        setDataSource(fileName)
                        prepare()
                        start()

                        //disable playback if already playing
                        playButton.isEnabled = false
                        Log.d(LOG_TAG, "PLAY ON")
                        val icon = getDrawable(requireContext(), R.drawable.pause_black_small)
                        playButton.setImageDrawable(icon)


                        setOnCompletionListener {
                            val icon = getDrawable(requireContext(), R.drawable.play_black_small)
                            playButton.setImageDrawable(icon)
                            reset()
                            //enable playback for future clicks
                            playButton.isEnabled = true
                            Log.d(LOG_TAG, "PLAY END")
                        }
                    }
                }
            }
        }

        /* RESET & PREDICT */
        val analyzeButton: Button = root.findViewById(R.id.analyzeButton)
        analyzeButton.setOnClickListener {

            vibrate()
            // Not interactive when recording is in progress
            if ((!isRecording && !isPaused) || (isRecording && isPaused)) {
                if (!hasAudio()){
                    Toast.makeText(requireContext(), "Nothing to Predict!", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Log.d(LOG_TAG, "RESET")
                    /* reset meter after completing recording*/

//                    showprogress()
                    finish()
                    Toast.makeText(requireContext(), "STOPPED", Toast.LENGTH_SHORT).show()
                    meter.base = SystemClock.elapsedRealtime()

                    // Init python
                    if (!Python.isStarted()){
                        Python.start(AndroidPlatform(requireContext()))
                    }

                    // Init scripts
                    val python = Python.getInstance()

                    //convert the .3gp to .wav for python scripts to use with AudioSegment
                    // TODO (REMOVE HARDCODED METHOD)
                    FFmpegKit.execute("-i /data/user/0/com.example.birdup/files/audiorecordtest_$recDate.3gp " +
                            "/data/user/0/com.example.birdup/files/audiorecordtest_$recDate.wav")

                    //get input audio path
                    for (file in requireContext().filesDir.listFiles()!!)
                        if (file.name.endsWith("wav"))
                            modelInput = file.toString()

                    Log.d("INPUT", modelInput.toString())
                    Log.d("FILE", fileName.toString())

                    //create new folder to store chunks and images separately from audio
                    val sampleDir = File(requireContext().filesDir?.path+"/samples")
                    if (!sampleDir.isDirectory)
                        Log.d("FOLDER CREATED", sampleDir.toString())
                        sampleDir.mkdirs()

                    // Split audio in 5sec chunks, remove noisy/empty ones
                    val splitFile = python.getModule("split_wav")
                    splitFile.callAttr("split", modelInput!!.toString(), sampleDir.toString())

                    // CHECK SAMPLEDIR CONTENTS AFTER SPLIT
                    for (f in sampleDir.listFiles()!!) {
                        Log.d("DIRECTORY AFTER SPLIT", f.absolutePath)
//                        f.delete()
                    }

                    // Convert chunks into spectrograms with STFT
                    val preprocessFile = python.getModule("preprocessing")
                    preprocessFile.callAttr("make_specs", sampleDir.toString())

                    // CHECK SAMPLEDIR CONTENTS AFTER PREPROCESSING
                    for (f in sampleDir.listFiles()!!) {
                        Log.d("DIRECTORY AFTER PREPROCESSING", f.absolutePath)
                        //f.delete()
                    }

                    //Resize image dims, compress, convert to lighter .jpg format
                    val convertPng = python.getModule("png2jpg")
                    convertPng.callAttr("compress", sampleDir.toString())

                    // CHECK SAMPLEDIR CONTENTS AFTER COMPRESSING
                    for (f in sampleDir.listFiles()!!) {
                        Log.d("DIRECTORY AFTER COMPRESSING", f.absolutePath)
                        //f.delete()
                    }

                    //If sample directory is empty, let user know
                    if(sampleDir.listFiles()!!.isEmpty()) {
                        Toast.makeText(
                            requireContext(), "No valid audio remained. Please try again",
                            Toast.LENGTH_LONG).show()
                        //remove previous recording
                        File(requireContext().filesDir?.path+"/audiorecordtest_$recDate.3gp").delete()
                        File(requireContext().filesDir?.path+"/audiorecordtest_$recDate.wav").delete()
//                        for (f in requireContext().filesDir?.listFiles()!!) {
//                            if(f.name.endsWith("wav") or f.name.endsWith("3gp")) {
//                                Log.d(LOG_TAG, f.absolutePath)
//                                f.delete()
//                            }
//                        }
                    }
                    // pass the valid chunks through the model to make predictions
                    else {
                        //LOAD MODEL
//                        val model = context?.let { it1 -> FileUtil.loadMappedFile(it1, "finalized_test.tflite") }
                        val model = context?.let { it1 -> FileUtil.loadMappedFile(it1, "1_sec_specs.tflite") }
                        val interpreter = model?.let { it1 -> Interpreter(it1) }

                        //LOAD LABELS
                        val labels = context?.let { it1 -> FileUtil.loadLabels(it1, "labels.txt") }
                        Log.d("LABELS CONTENT", labels.toString())
                        val commons = context?.let { it1 -> FileUtil.loadLabels(it1, "commons.txt") }

                        // INIT IMAGEPROCESSOR - NORMALIZING, RESIZING
                        val imageProcessor = ImageProcessor.Builder()
                            .add(NormalizeOp(0F, 255.0F))
//                            .add(ResizeOp(168, 224, ResizeOp.ResizeMethod.BILINEAR))
                            .build()

                        for (sample in sampleDir.listFiles()!!){

                            val bitmap = BitmapFactory.decodeFile(sample.absolutePath)

                            // Only FLOAT32 and UINT-8 supported
                            val inputShape = intArrayOf(1, 168, 224, 3)
                            val inputFeature0 = TensorBuffer.createFixedSize(inputShape,
                            DataType.FLOAT32)

                            val outputShape = intArrayOf(1, 50)
                            val outputFeature0 = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32)

                            //// IMAGE PROCESSOR CODE
                            var tImage = TensorImage(DataType.FLOAT32)
                            tImage.load(bitmap)
                            tImage = imageProcessor.process(tImage)

                            try {
                                //inputFeature0.loadBuffer(buffer)
                                inputFeature0.loadBuffer(tImage.buffer.order(ByteOrder.nativeOrder()))
                            } catch (e: java.lang.IllegalArgumentException) {
                                Log.e(LOG_TAG, "INPUT TO MODEL FAIL")
                                e.printStackTrace()
                            }

                            if (interpreter != null)
                                with(interpreter) { run(inputFeature0.buffer, outputFeature0.buffer) }

                            // SORT OUTPUTS BY DESCENDING ORDER
                            val sortedOutputs = outputFeature0.floatArray.sortedArrayDescending()
                            val idxResults = getMax(outputFeature0.floatArray)

                            outputFeature0.buffer.rewind()
//                            val probabilities = outputFeature0.buffer.asFloatBuffer()
//                            try {
//                                val am = requireContext().assets
//                                val stream = am.open("labels.txt")
//                                val reader = BufferedReader(
//                                    InputStreamReader(stream)
//                                )
//                                for (i in 0 until probabilities.capacity()) {
//                                    val label: String = reader.readLine()
//                                    val probability = probabilities.get(i)
//                                    println("$label: $probability")
//                                }
//                            } catch (e: IOException) {
//                                // File not found?
//                            }

                            for(i in 0..49){
                                Log.d("RESULTS $i", sortedOutputs[i].toString())
                            }

                            // Only display predictions if the max probability is higher than 50%
                            if(sortedOutputs[0] > 0.5){
                                // Round percentages before showing
                                val df = DecimalFormat("#.##")
                                df.roundingMode = RoundingMode.CEILING

                                // instantiateRecycler Adapter
                                predictionList.adapter = RecyclerAdapter(
                                    titlesList,
                                    detailsList,
                                    percentsList,
                                    imageList
                                ){
                                    predictionList.visibility = View.INVISIBLE
                                }
//                                showprogress()

                                // POPULATE HELPER LISTS WITH TOP 3 PREDICTIONS
                                for (i in 1..3) {
                                    titlesListHelper.add(
                                        (labels?.get(idxResults[i-1]) ?: idxResults[i-1]).toString()
                                    )
                                    detailsListHelper.add(
                                        (commons?.get(idxResults[i-1]) ?: idxResults[i-1]).toString()
                                    )
//                                    percentsListHelper.add(df.format(sortedOutputs[i-1].times(100).toDouble()).toString()+"%")
                                    percentsListHelper.add(df.format(sortedOutputs[i-1].times(100)).toDouble())
                                    imageListHelper.add(R.mipmap.ic_launcher_round)
                                }

                                Log.d("~~~~~~~~~~~~~~~HELPER TITLES LIST", titlesListHelper.toString())
                                Log.d("~~~~~~~~~~~~~~~HELPER PERCENTS LIST", percentsListHelper.toString())
                            }
                        }
                        // close model when predictions are completed
                        interpreter?.close()

                        // Average all results among helper lists
                        if (percentsListHelper.size > 3) {

                            // List to keep indexes to be removed
                            val idxToRemove = mutableListOf<Int>()
                            var max_index = titlesListHelper.indices.last
                            // Check for all predicted names, starting from the 1st one
                            for (i in 0..max_index) {
                                Log.d("NOW CHECKING", titlesListHelper[i])
                                Log.d("IIIIIIIII MAX INDEX", max_index.toString())
                                val target = titlesListHelper[i]
                                // appearances of species among predictions
                                var frequency = 0
                                // initial score of predicted species
                                var sum: Double = 0.0
                                for (j in i..max_index) {
                                    Log.d("COMPARING WITH", titlesListHelper[j])
                                    Log.d("JJJJJJJJJ MAX INDEX", max_index.toString())
                                    //  If predicted species appears more than once, average
                                    // its predicted scores and remove all occurrences after 1st
                                    if (titlesListHelper[j] == target) {
                                        Log.d("FOUND A MATCH WITH:::", titlesListHelper[j])
                                        frequency += 1
                                        sum += percentsListHelper[j]
                                        // Remove all occurrences after the first one
                                        if (frequency > 1) {
                                            idxToRemove.add(j)
                                            Log.d("TO BE REMOVED:::", titlesListHelper[j])
                                        }
                                        // If there are only 3 predictions remaining, break from loop
                                        if (percentsListHelper.size == 3) {
                                            Log.d("@@@@@@@@@@@@@@@@@@", "BREAK")
                                            break
                                        }
                                    }
                                }
                                // Average scores at end of iteration for species i
                                percentsListHelper[i] = sum / frequency

                                for(duplicate in idxToRemove.indices.last downTo 0){
                                    Log.d("REMOVING...", titlesListHelper[idxToRemove[duplicate]])
                                    titlesListHelper.removeAt(idxToRemove[duplicate])
                                    detailsListHelper.removeAt(idxToRemove[duplicate])
                                    percentsListHelper.removeAt(idxToRemove[duplicate])
                                    imageListHelper.removeAt(idxToRemove[duplicate])
                                    max_index -= 1
                                }
                                // Empty the list from already deleted elements
                                idxToRemove.clear()
                                Log.d(
                                    "~~~~~~~~~~~~~~~HELPER TITLES LIST",
                                    titlesListHelper.toString()
                                )
                                Log.d(
                                    "~~~~~~~~~~~~~~~HELPER PERCENTS LIST",
                                    percentsListHelper.toString()
                                )
                                // If there are only 3 predictions remaining, break from loop
                                if (percentsListHelper.size == 3) {
                                    Log.d("@@@@@@@@@@@@@@@@@@", "BREAK")
                                    break
                                }
                            }
                            //sort final results among all lists
                            val percentsArrayHelper = percentsListHelper.toDoubleArray()
                            val sortedPercentsArrayHelper = percentsArrayHelper.sortedArrayDescending()
                            val idxList = mutableListOf<Int>(51, 52, 53)

                            Log.d("@@@@@LIST BEFORE", idxList.toString())
                            // Get Indexes of top 3 predictions
                            for(i in percentsListHelper.indices){
                                when {
                                    percentsArrayHelper[i] == sortedPercentsArrayHelper[0] -> {
                                        idxList.add(0, i)
                                    }
                                    percentsArrayHelper[i] == sortedPercentsArrayHelper[1] -> {
                                        idxList.add(1, i)
                                    }
                                    percentsArrayHelper[i] == sortedPercentsArrayHelper[2] -> {
                                        idxList.add(2, i)
                                    }
                                }
                            }
                            idxList.remove(51)
                            idxList.remove(52)
                            idxList.remove(53)

                            Log.d("@@@@@LIST AFTER", idxList.toString())

                            // Round averaged results to avoid long numbers
                            val df = DecimalFormat("#.##")
                            df.roundingMode = RoundingMode.CEILING

                            // Pass results to PredictionsList
                            for (i in 1..3) {
                                titlesList.add((titlesListHelper[idxList[i - 1]]))
                                detailsList.add(detailsListHelper[idxList[i - 1]])
                                percentsList.add(df.format(percentsListHelper[idxList[i - 1]]).toString() + "%")
                                imageList.add(R.mipmap.ic_launcher_round)
                            }


                        // If initial predictions are only 3, pass them directly to the RecyclerView
                        }else{
                            for (i in 1..3) {
                                titlesList.add((titlesListHelper[i-1]))
                                detailsList.add(detailsListHelper[i-1])
                                percentsList.add((percentsListHelper[i-1]).toString()+"%")
                                imageList.add(R.mipmap.ic_launcher_round)
                            }
                        }
                        // Empty the helper lists to be ready for new data
                        titlesListHelper.clear()
                        detailsListHelper.clear()
                        percentsListHelper.clear()
                        imageListHelper.clear()

                        val coin_toss = percentsList[0].replace("%", "")
                        if(coin_toss.toFloat()<0.5){
                            if(hasAudio()) {
                                File(requireContext().filesDir?.path+"/audiorecordtest_$recDate.3gp").delete()
                                File(requireContext().filesDir?.path+"/audiorecordtest_$recDate.wav").delete()
                            }
                            titlesList.clear()
                            detailsList.clear()
                            percentsList.clear()
                            imageList.clear()
                            finish()
                            Toast.makeText(requireContext(), "No specific bird detected. Please try again.", Toast.LENGTH_SHORT).show()
                        }else{
                            // SHOW RESULTS TO USER
                            predictionList.visibility = View.VISIBLE
                        }
                    }
                    for (f in sampleDir.listFiles()!!) {
                        Log.d("DIRECTORY AFTER PREPROCESSING", f.absolutePath)
                        f.delete()
                    }
                }
            }
        }

        /* DISCARD & RESET */
        val trashButton: ImageButton = root.findViewById((R.id.trashButton))
        trashButton.setOnClickListener {
            vibrate()
            // Not interactive when recording is in progress
            if ((!isRecording && !isPaused) || (isRecording && isPaused)) {
                Log.d(LOG_TAG, "DISCARD")
                finish()
                meter.base = SystemClock.elapsedRealtime()
                predictionList.visibility = View.INVISIBLE
                if(hasAudio()){
                    File(requireContext().filesDir?.path+"/audiorecordtest_$recDate.3gp").delete()
                    File(requireContext().filesDir?.path+"/audiorecordtest_$recDate.wav").delete()
//                    for (f in requireContext().filesDir?.listFiles()!!) {
//                        if(f.name.endsWith("wav") or f.name.endsWith("3gp")) {
//                            Log.d(LOG_TAG, f.absolutePath)
//                            f.delete()
//                        }
//                    }
                    // REMOVE PREDICTIONS FROM LIST/VIEW
                    predictionList.visibility = View.INVISIBLE
                    titlesList.clear()
                    detailsList.clear()
                    percentsList.clear()
                    imageList.clear()

                    Toast.makeText(requireContext(), "Trash emptied!", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Log.d(LOG_TAG, "No Files.\n")
                    Toast.makeText(requireContext(), "Nothing in trash", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        return root
    }

    // DECLARE FUNCTIONS TO BE USED //

    // Check for existing recordings in internal storage
    private fun hasAudio(): Boolean{
        return if((recDate == "0") or(File(requireContext().filesDir?.path+"/audiorecordtest_$recDate.rec").exists())){
            false
        } else {
            (File(requireContext().filesDir?.path+"/audiorecordtest_$recDate.3gp").exists())
        }
//        val f: File = requireContext().filesDir
//        val filter = FileFilter { f -> f.name.endsWith("3gp") or f.name.endsWith("wav") }
//        val files = f.listFiles(filter) ?: throw IllegalArgumentException("non-null value expected")
//        Log.d("3GP/WAV FILES", files.size.toString())
//        return(files.isNotEmpty())
    }

    // Reset recorder
    private fun finish() {
        Log.d(LOG_TAG, "STOP")
        releaseRecorder()
        timeWhenStopped = 0
        isPaused = false
        isRecording = false
    }

    // Vibrate button on click
    private fun vibrate() {
        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) { // Vibrator availability checking
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)) // New vibrate method for API Level 26 or higher
            } else {
                vibrator.vibrate(100) // Vibrate method for below API Level 26
            }
        }
    }


    private fun getMax(arr: FloatArray) : List<Int>{

        val idxList = mutableListOf(50, 51, 52)
        val sortedArray = arr.sortedArrayDescending()

        for(i in 0..49){
        //for(i in 0..arr.indices.last){
            when {
                arr[i] == sortedArray[0] -> {
                    idxList.add(0, i)
                }
                arr[i] == sortedArray[1] -> {
                    idxList.add(1, i)
                }
                arr[i] == sortedArray[2] -> {
                    idxList.add(2, i)
                }
            }
        }
        // Too stupid, change it please
        idxList.remove(50)
        idxList.remove(51)
        idxList.remove(52)

        return idxList
    }

    /* RECORDER FUNCTIONS START*/
    private fun onRecord() {
        when{
            isPaused -> resumeRecording()
            isRecording -> pauseRecording()
            else -> startRecording()
        }
    }

    private fun releaseRecorder() {
        recorder?.stop()
        recorder?.release()
        recorder = null
    }
    private fun startRecording() {
        val simpleDateFormat = SimpleDateFormat("d-MM-yyyy@HH:mm:ss", Locale.getDefault())
        val formatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter.ofPattern("d-MM-yyyy@HH:mm:ss", Locale.getDefault())
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val localDateTime: LocalDateTime = LocalDateTime.now()
        val ldtString: String = formatter.format(localDateTime).replace(":", "*")
        Log.d("formatted", ldtString)
        Log.d("formatted enc", URLEncoder.encode(ldtString, "utf-8"))
        Log.d("formatted dec", URLDecoder.decode(ldtString, "utf-8"))


        //replace : in date because it changes when encoded and filename is invalid
        val date : String = simpleDateFormat.format(Date()).replace(":", "*")
        recDate = date
        val encodedDate = URLEncoder.encode(recDate, "utf-8")
        val decodedDate = URLDecoder.decode(recDate, "utf-8")
        Log.d("recDate", recDate)
        Log.d("date", date)
        Log.d("ENCODED", encodedDate)
        Log.d("DECODED", decodedDate)


        //":" character in date has to be replaced for encoding issues in MediaPlayer(Reserved by Unix)
        fileName = requireContext().filesDir?.path+"/audiorecordtest_$date.3gp"
//        fileName = requireContext().filesDir?.path+"/audiorecordtest.3gp"

        File(fileName?:"").createNewFile()
//        File(fileName?:"").setReadable(true, false)
//        File(fileName?:"").setWritable(true, false)

        /* Branch for if MediaRecorder() is not deprecated */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            recorder = MediaRecorder(requireContext()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                // FOR MP3,
                //Use AAC_ELD(optimized for higher quality) for medium/higher bitrate(1411kbps)
                // instead of HE_AAC(low bandwidth for livestreaming), for low bitrate(705kbps)
                //TODO("TRY LOWER SAMPLE RATES FOR TESTING")
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(fileName)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC_ELD)
                setAudioChannels(1)
                setAudioEncodingBitRate(16*22050)
                setAudioSamplingRate(22050)

                Log.d(LOG_TAG, "UPDATED")
                try {
                    prepare()
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "prepare() failed")
                    e.printStackTrace()
                }

                try {
                    Log.d(LOG_TAG, "START")
                    start()
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "start() failed")
                    e.printStackTrace()
                }
            }
            isRecording = true
            isPaused = false
        }
        else {
            /* Branch for if MediaRecorder() is deprecated */
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                // FOR MP3,
                //Use AAC_ELD(optimized for higher quality) for medium/higher bitrate(1411kbps)
                // instead of HE_AAC(low bandwidth for livestreaming), for low bitrate(705kbps)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(fileName)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC_ELD)
                //TODO("TRY LOWER SAMPLE RATES FOR TESTING")
                setAudioChannels(1)
                setAudioEncodingBitRate(16*22050)
                setAudioSamplingRate(22050)

                Log.d(LOG_TAG, "DEPRECATED")
                try {
                    prepare()
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "prepare() failed")
                    e.printStackTrace()
                }

                try {
                    Log.d(LOG_TAG, "START")
                    start()
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "start() failed")
                    e.printStackTrace()
                }
            }
        }
        isRecording = true
        isPaused = false
    }

    private fun pauseRecording(){
        /* pause() requires Android 7 or higher(API 24) */
        Log.d(LOG_TAG, "PAUSE")
        try {
            recorder?.pause()
        } catch (e: IOException) {
            Log.e(LOG_TAG, "pause() failed")
            e.printStackTrace()
        }
        isPaused = true
    }

    private fun resumeRecording(){
        /* resume() requires Android 7 or higher(API 24) */
        Log.d(LOG_TAG, "RESUME")
        try {
            recorder?.resume()
        } catch (e: IOException) {
            Log.e(LOG_TAG, "resume() failed")
            e.printStackTrace()
        }
        isPaused = false
    }
    /* RECORDER FUNCTIONS END */

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onStop() {
        super.onStop()
        recorder?.release()
        recorder = null
        player?.release()
        player = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // remove any recordings if the app is closed
        if(hasAudio()) {
            File(requireContext().filesDir?.path+"/audiorecordtest_$recDate.3gp").delete()
            File(requireContext().filesDir?.path+"/audiorecordtest_$recDate.wav").delete()
//            for (f in requireContext().filesDir?.listFiles()!!) {
//                if (f.name.endsWith("wav") or f.name.endsWith("3gp")) {
//                    Log.d(LOG_TAG, f.absolutePath)
//                    f.delete()
//                }
//            }
        }
    }
}