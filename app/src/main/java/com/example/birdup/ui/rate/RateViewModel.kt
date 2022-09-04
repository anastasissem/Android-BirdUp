package com.example.birdup.ui.rate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RateViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
      value = "This is Rate Fragment"
    }
    val text: LiveData<String> = _text
}