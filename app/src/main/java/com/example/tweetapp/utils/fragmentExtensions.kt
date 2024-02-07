package com.example.tweetapp.utils

import android.content.res.Resources
import android.graphics.Rect
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment

fun DialogFragment.setSize(widthPercentage: Int, heightPercentage: Int = 100) {
    val newWidth = widthPercentage.toFloat() / 100
//        val newHeight = heightPercentage.toFloat() / 100
    val dm = Resources.getSystem().displayMetrics
    val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
    val percentWidth = rect.width() * newWidth
    //   val percentHeight = rect.height() * newHeight
    dialog?.window?.setLayout(percentWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
}