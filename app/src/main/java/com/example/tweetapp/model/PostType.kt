package com.example.tweetapp.model

import com.hasura.FetchNoteQuery

data class PostType(
    val type : Action,
    val post: Post
)