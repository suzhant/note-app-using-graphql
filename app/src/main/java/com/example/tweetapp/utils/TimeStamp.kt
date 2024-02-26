package com.example.tweetapp.utils

import java.util.Calendar

object TimeStamp {

    private var calendar: Calendar = Calendar.getInstance()

    fun ofMinutes(min : Int) : Long{
        calendar.clear()
        calendar.set(Calendar.MINUTE,min)
        return calendar.timeInMillis
    }

    fun ofDays(day : Int) : Long{
        calendar.clear()
        calendar.set(Calendar.DAY_OF_MONTH,day)
        return calendar.timeInMillis
    }

    fun ofYears(year : Int) : Long{
        calendar.clear()
        calendar.set(Calendar.YEAR,year)
        return calendar.timeInMillis
    }

    fun calculateBefore(timeInMillis : Long) : Long{
        val currentTime = Calendar.getInstance().timeInMillis
        return currentTime - timeInMillis
    }

    fun isValidDuration(targetMillis : Long, beforeTimeMillis : Long) : Boolean{
        val currentTime = Calendar.getInstance().timeInMillis
        val diff = targetMillis - beforeTimeMillis
        return currentTime <= diff
    }
}