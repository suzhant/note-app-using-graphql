package com.example.tweetapp.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "notes")
data class Post(
    @PrimaryKey(autoGenerate = false) val id: String,
    @ColumnInfo(name = "title") val title : String,
    @ColumnInfo(name = "body")val body : String,
    @ColumnInfo(name = "timestamp") val timestamp : Long,
    @ColumnInfo(name = "user_id") val uuid : String
) : Parcelable
