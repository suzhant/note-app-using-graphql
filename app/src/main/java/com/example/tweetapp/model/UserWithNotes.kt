package com.example.tweetapp.model

import androidx.room.Embedded
import androidx.room.Relation

data class UserWithNotes(
    @Embedded val user: User,
    @Relation(
        parentColumn = "uuid",
        entityColumn = "user_id"
    )
    val posts: List<Post>
)
