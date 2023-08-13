package com.example.tweetapp.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object DateTimeUtil {

     private fun choosePattern(date: Long): String {
        val secondsIn24Hours = 24 * 60 * 60 * 1000
        val currentTime = Date().time
        val timeDiff = currentTime - date
        return  if (timeDiff>=secondsIn24Hours){
            "MMMM dd, yyyy"
        }else{
            "HH:mm a"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun convertMillisToDate(time: Long): String {
        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault())
        val pattern = choosePattern(time)
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.US)
        return date.format(formatter)
    }

}