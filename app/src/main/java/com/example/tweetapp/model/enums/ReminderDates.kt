package com.example.tweetapp.model.enums

import com.example.tweetapp.utils.TimeStamp
import java.sql.Time
import java.time.Duration

//enum class ReminderDates {
//    SAME_WITH_DUE_DATE {
//        override fun getTimeStamp(givenTimeInMillis: Long): Long {
//            return givenTimeInMillis
//        }
//
//        override fun toString(): String {
//            return super.toString()
//        }
//    },
//    _5_MIN_BEFORE {
//        override fun getTimeStamp(givenTimeInMillis: Long): Long {
//            return givenTimeInMillis - TimeStamp.ofMinute(5)
//        }
//
//        override fun toString(): String {
//            return super.toString()
//        }
//    },
//    _10_MIN_BEFORE {
//        override fun getTimeStamp(givenTimeInMillis: Long): Long {
//            return givenTimeInMillis - TimeStamp.ofMinute(10)
//        }
//
//        override fun toString(): String {
//            return super.toString()
//        }
//    },
//    _1_DAY {
//        override fun getTimeStamp(givenTimeInMillis: Long): Long {
//            val oneDayInMillis = 1L * 24 * 60 * 60 * 1000
//            return givenTimeInMillis - TimeStamp.ofDay(1)
//        }
//
//        override fun toString(): String {
//            return super.toString()
//        }
//    },
//    _2_DAY {
//        override fun getTimeStamp(givenTimeInMillis: Long): Long {
//            val oneDayInMillis = 2L * 24 * 60 * 60 * 1000
//            return givenTimeInMillis - TimeStamp.ofDay(2)
//        }
//
//        override fun toString(): String {
//            return super.toString()
//        }
//    };
//    abstract fun getTimeStamp(givenTimeInMillis : Long) : Long
//}

sealed class ReminderDates {
    abstract fun getTimeStamp(givenTimeInMillis: Long): Long

    object SAME_WITH_DUE_DATE : ReminderDates() {
        override fun getTimeStamp(givenTimeInMillis: Long): Long = givenTimeInMillis

        override fun toString(): String {
            return "Same with due date"
        }
    }

    object _5_MIN_BEFORE : ReminderDates() {
        override fun getTimeStamp(givenTimeInMillis: Long): Long = givenTimeInMillis - TimeStamp.ofMinutes(5)

        override fun toString(): String {
            return "5 minutes before"
        }
    }

    object _10_MIN_BEFORE : ReminderDates() {
        override fun getTimeStamp(givenTimeInMillis: Long): Long = givenTimeInMillis - TimeStamp.ofMinutes(10)

        override fun toString(): String {
            return "10 minutes before"
        }
    }

    object _1_DAY : ReminderDates() {
        override fun getTimeStamp(givenTimeInMillis: Long): Long = givenTimeInMillis - TimeStamp.ofDays(1)

        override fun toString(): String {
            return "1 days before"
        }
    }

    object _2_DAY : ReminderDates() {
        override fun getTimeStamp(givenTimeInMillis: Long): Long = givenTimeInMillis - TimeStamp.ofDays(2)
        override fun toString(): String {
            return "2 days before"
        }
    }
}