package com.example.birdup.ui.recordings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.birdup.model.Bird
import com.example.birdup.model.Recording

class RecordingsViewModel : ViewModel() {
    private val _recordings = MutableLiveData<ArrayList<Recording>>() //List of recording files
    val recordings: LiveData<ArrayList<Recording>> = _recordings

    fun setRecordings(list: ArrayList<Recording>){
        _recordings.value = list
    }
}