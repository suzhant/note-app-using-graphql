package com.example.tweetapp.extension

import com.example.tweetapp.model.AlarmItem
import com.example.tweetapp.model.Post
import com.google.firebase.auth.FirebaseAuth

fun AlarmItem.toPost(): Post {
    return Post(
        id = postId,
        title = title,
        body = message,
        timestamp = timeStamp,
        uuid = FirebaseAuth.getInstance().uid.toString(),
    )
}