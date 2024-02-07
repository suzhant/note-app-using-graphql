package com.example.tweetapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AlarmItem(
    val triggerTimeInMillis : Long,
    val message : String,
    val title : String,
    val postId : String,
    val timeStamp : Long
) : Parcelable