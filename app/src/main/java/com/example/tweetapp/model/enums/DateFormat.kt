package com.example.tweetapp.model.enums

enum class DateFormat  {
    TIME{
        override fun toString(): String {
            return "HH:mm"
        }
        },
    DATE{
        override fun toString(): String {
            return "yyyy-MM-dd"
        }
    }
}