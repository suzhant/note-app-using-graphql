package com.example.tweetapp.utils

import android.content.res.Resources
import android.graphics.Rect
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController

fun DialogFragment.setSize(widthPercentage: Int, heightPercentage: Int = 100) {
    val newWidth = widthPercentage.toFloat() / 100
//        val newHeight = heightPercentage.toFloat() / 100
    val dm = Resources.getSystem().displayMetrics
    val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
    val percentWidth = rect.width() * newWidth
    //   val percentHeight = rect.height() * newHeight
    dialog?.window?.setLayout(percentWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
}

fun Fragment.navigateSafe(destination: NavDirections?) {
    val navController = findNavController()
    val currentDestination = navController.currentDestination
    destination?.let {
        val action = currentDestination?.getAction(it.actionId)
        action?.let {
            navController.navigate(destination)
        }
    }
}