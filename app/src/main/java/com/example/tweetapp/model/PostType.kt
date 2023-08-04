package com.example.tweetapp.model

import com.hasura.FetchPostQuery

data class PostType(
    val type : Action,
    val post: FetchPostQuery.Post
)