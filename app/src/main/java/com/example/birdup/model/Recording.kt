package com.example.birdup.model

import java.io.Serializable

class Recording(
    val bird: Bird,
    val path: String,
    val probability: String,//Double,
    val dateTime: String
): Serializable {
}