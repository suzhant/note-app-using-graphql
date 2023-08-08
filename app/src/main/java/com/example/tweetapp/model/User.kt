package com.example.tweetapp.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = false) val uuid : String,
    @ColumnInfo(name = "username") val username : String,
    @ColumnInfo(name = "email") val email : String,
    @ColumnInfo(name = "profilePic") val profilePic: String
)
